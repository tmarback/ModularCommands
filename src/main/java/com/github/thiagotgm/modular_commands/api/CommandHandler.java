/*
 * This file is part of ModularCommands.
 *
 * ModularCommands is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModularCommands is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ModularCommands. If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.thiagotgm.modular_commands.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuilder;

/**
 * Listener that identifies commands from text messages and executes them.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-14
 */
public class CommandHandler implements IListener<MessageReceivedEvent> {
    
    private static final Logger LOG = LoggerFactory.getLogger( CommandHandler.class );
    private static final String COMMAND_FORMAT = "\"%s\" (\"%s\")";
    private static final String COMMAND_TRACE_FORMAT = "\"%s\" called by \"%s\" in channel \"%s\" on server \"%s\"";
    
    private final CommandRegistry registry;
    
    /**
     * Creates a command handler that uses a given registry.
     *
     * @param registry Registry that this handler should use.
     */
    public CommandHandler( CommandRegistry registry ) {
        
        this.registry = registry;
        
    }

    /**
     * When a message is received, parses it to determine if it has the signature of a registered
     * command.<br>
     * If it does, checks if the command should be executed (based on context and the command's settings)
     * and executes it if so.
     * 
     * @param event Event fired by a message being received.
     */
    @Override
    public void handle( MessageReceivedEvent event ) {
        
        if ( event.getAuthor().equals( event.getClient().getOurUser() ) ) {
            return; // Ignores own messages.
        }

        /* Get command and args */
        String message = event.getMessage().getContent();
        List<String> args = Arrays.asList( message.trim().split( " +" ) );
        if ( args.size() == 0 ) {
            return; // Empty message.
        }
        
        /* Identify command */
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Parsing command " + args );
        }
        ICommand mainCommand = registry.parseCommand( args.get( 0 ) );
        if ( mainCommand == null ) {
            LOG.trace( "No command found." );
            return; // No recognized command.
        }
        if ( LOG.isTraceEnabled() ) {
            LOG.trace( "Identified main command " +
                    String.format( COMMAND_FORMAT, args.get( 0 ), mainCommand.getName() ) + "." );
        }
        
        /* Get command chain */
        List<ICommand> commands = new ArrayList<>();
        commands.add( mainCommand );
        commands.addAll( getSubcommands( mainCommand, args.subList( 1, args.size() ) ) ); // Identify subcommands.
        final ICommand command = commands.get( commands.size() - 1 ); // Actual command is the last
        List<String> actualArgs = args.subList( commands.size(), args.size() );      // subcommand.
        final CommandContext context = new CommandContext( event, command, actualArgs );
        if ( LOG.isTraceEnabled() ) {
            LOG.trace( "Identified command " +
                    String.format( COMMAND_FORMAT, getCommandSignature( args, commands.size() ),
                            command.getName() ) );
        }
        
        /* Check if the command should be executed */
        for ( ICommand curCommand : commands ) { // Check that each command on the chain
            if ( !curCommand.isEffectivelyEnabled() ) {                   // is enabled.
                LOG.trace( "Command is disabled." );
                return; // Command is disabled.
            }
        }
        if ( command.ignorePublic() && !event.getChannel().isPrivate() ) {
            LOG.trace( "Ignoring public execution." );
            return; // Ignore public command.
        }
        if ( command.ignorePrivate() && event.getChannel().isPrivate() ) {
            LOG.trace( "Ignoring private execution." );
            return; // Ignore private command.
        }
        if ( command.ignoreBots() && event.getAuthor().isBot() ) {
            LOG.trace( "Ignoring bot caller." );
            return; // Ignore bot.
        }
        if ( !command.getRegistry().contextCheck( context ) ) {
            LOG.trace( "Registry context check failed." );
            return; // Registry disabled under calling context.
        }
        
        /* Check if the caller is allowed to call the command. */
        if ( LOG.isDebugEnabled() ) {
            LOG.debug( "Command " + getCommandTrace( command, event ) + " - checking permission" );
        }
        RequestBuilder errorBuilder = new RequestBuilder( event.getClient() ); // Builds request for
        errorBuilder.shouldBufferRequests( true ).setAsync( true );            // error handler.
        errorBuilder.onMissingPermissionsError( ( exception ) -> {
            // In case error handler hits a MissingPermissions error.
            LOG.warn( "Lacking permissions to execute operation.", exception );
            
        });
        errorBuilder.onDiscordError( ( exception ) -> {
            // In case error handler hits a Discord error.
            LOG.warn( "Discord error encountered while performing operation.", exception );
        
        });
        if ( command.requiresOwner() && !event.getClient().getApplicationOwner().equals( event.getAuthor() ) ) {
            LOG.debug( "Caller is not the owner of the bot." );
            errorBuilder.doAction( () -> { // Command can only be called by bot owner.
                
                command.onFailure( context, FailureReason.USER_NOT_OWNER );
                return true;
                
            }).execute();
            return;
        }
        EnumSet<Permissions> channelPermissions = event.getChannel().getModifiedPermissions( event.getAuthor() );
        if ( !channelPermissions.containsAll( command.getRequiredPermissions() ) ) {
            LOG.debug( "Caller does not have the required permissions in the channel." );
            errorBuilder.doAction( () -> { // User does not have required channel-overriden permissions.
                
                command.onFailure( context, FailureReason.USER_MISSING_PERMISSIONS );
                return true;
                
            }).execute();
            return;
        }
        EnumSet<Permissions> guildPermissions = event.getAuthor().getPermissionsForGuild( event.getGuild() );
        if ( !guildPermissions.containsAll( command.getRequiredGuildPermissions() ) ) {
            LOG.debug( "Caller does not have the required permissions in the server." );
            errorBuilder.doAction( () -> { // User does not have required server-wide permissions.
                
                command.onFailure( context, FailureReason.USER_MISSING_GUILD_PERMISSIONS );
                return true;
                
            }).execute();
            return;
        }
        
        /* Get execution chain */
        LOG.trace( "Permission check passed. Preparing to execute." );
        Stack<ICommand> executionChain = new Stack<>(); // Gets all the parent commands that should also
        buildExecutionChain( executionChain, commands.subList( 0, commands.size() - 1 ) ); // be executed.
        
        /* Build request */
        RequestBuilder builder = new RequestBuilder( event.getClient() );
        builder.shouldBufferRequests( true ).setAsync( true );
        final AtomicBoolean permissionsError = new AtomicBoolean();
        builder.onMissingPermissionsError( ( exception ) -> {
            
            permissionsError.set( true ); // Mark that a permission error occurred.
            LOG.warn( "Lacking permissions to execute operation.", exception );
            
        });
        final AtomicBoolean discordError = new AtomicBoolean();
        builder.onDiscordError( ( exception ) -> {
            
            discordError.set( true ); // Mark that a Discord error occurred.
            LOG.warn( "Discord error encountered while performing operation.", exception );
        
        });
        final ICommand firstCommand = executionChain.pop();
        builder.doAction( () -> {
            // Execute the first command in the chain.
            if ( LOG.isTraceEnabled() ) {
                LOG.trace( "Executing \"" + firstCommand.getName() + "\"" );
            }
            firstCommand.execute( context );
            return true;
            
        });
        while ( !executionChain.isEmpty() ) {
            // Execute each subsequent command in the chain.
            final ICommand nextCommand = executionChain.pop();
            builder.andThen( () -> {
                
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace( "Executing \"" + nextCommand.getName() + "\"" );
                }
                nextCommand.execute( context );
                return true;
                
            });
            
        }
        builder.elseDo( () -> { // In case command fails.
            
            LOG.debug( "Command failed. Executing failure handler." );
            if ( permissionsError.get() ) { // Failed due to missing permissions.
                command.onFailure( context, FailureReason.BOT_MISSING_PERMISSIONS );
            } else if ( discordError.get() ) { // Failed due to miscellaneous error.
                command.onFailure( context, FailureReason.DISCORD_ERROR );
            }
            return true;
            
        });
        builder.andThen( () -> { // In case command succeeds.
            
            LOG.debug( "Command succeeded." );
            Thread.sleep( command.getOnSuccessDelay() ); // Wait specified time.
            LOG.debug( "Executing success handler." );
            command.onSuccess( context ); // Execute success operation.
            return true;
            
        });
        if ( command.deleteCommand() ) { // Command message should be deleted after
            builder.andThen( () -> {     // successful execution.
                
                LOG.debug( "Successful execution. Deleting command message." );
                event.getMessage().delete();
                return true;
                
            });
        }
        
        /* Execute command */
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Executing command " + getCommandTrace( command, event ) );
        }
        builder.execute(); // Execute the command.
        
    }
    
    /**
     * Obtains the subcommand chain of the given command with the given arguments.<br>
     * That is, identifies the list of subcommands that matches the arguments.
     *
     * @param command Main command called.
     * @param args Arguments passed in to the command.
     * @return The chain of subcommands that match the args signature.
     */
    private List<ICommand> getSubcommands( ICommand command, List<String> args ) {
        
        List<ICommand> chain = new ArrayList<>( args.size() );
        if ( args.isEmpty() ) {
            return chain; // No args left, so no more possible subcommands.
        }
        
        String arg = args.get( 0 ); // Get next arg.
        ICommand subCommand = command.getSubCommand( arg );
        if ( subCommand != null ) { // Found a subcommand.
            if ( LOG.isTraceEnabled() ) {
                LOG.trace( "Identified subcommand \"" + subCommand.getName() + "\"" );
            }
            chain.add( subCommand ); // Adds the subcommand to the command list.
            chain.addAll( getSubcommands( subCommand, args.subList( 1, args.size() ) ) ); // Adds the subcommand's
        }                                                                                 // subcommands.
        return chain; // Return subcommand chain found.
        
    }
    
    /**
     * Builds the execution chain for the given commands.<br>
     * That is, gets the commands that should be executed according to the {@link ICommand#executeParent()}
     * setting of the commands.
     * <p>
     * After this method returns, the command on top of the stack is the first one that should be
     * executed, with the other commands in the stack also being retrieved in the order that
     * they should be executed.
     *
     * @param executionChain Where the commands to be executed should be added.
     * @param commands The commands that were triggered.
     */
    private void buildExecutionChain( Stack<ICommand> executionChain, List<ICommand> commands ) {
        
        if ( commands.isEmpty() ) {
            return; // No more commands to add.
        }
        ICommand curCommand = commands.get( commands.size() - 1 ); // Gets the last command.
        executionChain.push( curCommand ); // Add command to chain.
        if ( curCommand.executeParent() ) { // If specified, add the command before it to the chain.
            buildExecutionChain( executionChain, commands.subList( 0, commands.size() - 1 ) );
        }
        
    }
    
    /**
     * Rebuilds the signature of a command given the split args in the message and the
     * amount of commands that were parsed from it.
     *
     * @param args The args from the message received.
     * @param commandAmount How many commands (main command + subcommands) are in the args.
     * @return The signature of the command.
     */
    private static String getCommandSignature( List<String> args, int commandAmount ) {
        
        Iterator<String> commands = args.iterator();
        StringBuilder signature = new StringBuilder( commands.next() );
        commandAmount--;
        while ( commandAmount > 0 ) {
            
            signature.append( ' ' );
            signature.append( commands.next() );
            commandAmount--;
            
        }
        return signature.toString();
        
    }
    
    /**
     * Buils a trace message for a command that includes the command  name, who called it,
     * and where it was called from.
     *
     * @param command Command called.
     * @param event Event that triggered the command.
     * @return A trace message for the command.
     */
    private static String getCommandTrace( ICommand command, MessageReceivedEvent event ) {
        
        return String.format( COMMAND_TRACE_FORMAT, command.getName(), event.getAuthor().getName(),
                event.getChannel().getName(), event.getGuild().getName() );
        
    }

}
