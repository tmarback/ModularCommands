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
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
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
        List<String> argsCopy = new LinkedList<>( args );
        ICommand mainCommand = registry.parseCommand( argsCopy.remove( 0 ) );
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
        commands.addAll( getSubcommands( mainCommand, argsCopy ) ); // Identify subcommands.
        List<String> actualArgs = args.subList( commands.size(), args.size() );
        final ICommand command = commands.remove( commands.size() - 1 ); // Actual command is the last
        final CommandContext context = new CommandContext( event, command, actualArgs ); // subcommand.
        if ( LOG.isTraceEnabled() ) {
            LOG.trace( "Identified command " +
                    String.format( COMMAND_FORMAT, getCommandSignature( args, commands.size() ),
                            command.getName() ) );
                    
        }
        
        /* Check if the command should be executed */
        if ( !command.isEnabled() ) {
            LOG.trace( "Command was disabled." );
            return; // Command is disabled.
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
        
        /* Check if the caller is allowed to call the command. */
        if ( LOG.isTraceEnabled() ) {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace( "Command " + getCommandTrace( command, event ) + " - checking permission" );
                        
            }
        }
        RequestBuilder errorBuilder = new RequestBuilder( event.getClient() ); // Builds request for
        errorBuilder.shouldBufferRequests( true ).setAsync( true );            // error handler.
        errorBuilder.onMissingPermissionsError( ( exception ) -> {
            // In case error handler hits a MissingPermissions error.
            LOG.info( "Lacking permissions to execute operation.", exception );
            
        });
        errorBuilder.onDiscordError( ( exception ) -> {
            // In case error handler hits a Discord error.
            LOG.info( "Discord error encountered while performing operation.", exception );
        
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
        Stack<ICommand> executionChain = new Stack<>();
        Stack<CommandContext> contextChain = new Stack<>();
        executionChain.add( command );
        contextChain.add( context );
        buildExecutionChain( executionChain, contextChain, commands, args, event ); // Get the commands that
                                                                                    // should be executed.
        /* Build request */
        RequestBuilder builder = new RequestBuilder( event.getClient() );
        builder.shouldBufferRequests( true ).setAsync( true );
        final AtomicBoolean permissionsError = new AtomicBoolean();
        builder.onMissingPermissionsError( ( exception ) -> {
            
            permissionsError.set( true ); // Mark that a permission error occurred.
            LOG.info( "Lacking permissions to execute operation.", exception );
            
        });
        final AtomicBoolean discordError = new AtomicBoolean();
        builder.onDiscordError( ( exception ) -> {
            
            discordError.set( true ); // Mark that a Discord error occurred.
            LOG.info( "Discord error encountered while performing operation.", exception );
        
        });
        final ICommand firstCommand = executionChain.pop();
        final CommandContext firstContext = contextChain.pop();
        builder.doAction( () -> {
            // Execute the first command in the chain.
            firstCommand.execute( firstContext );
            return true;
            
        });
        while ( !executionChain.isEmpty() ) {
            // Execute each subsequent command in the chain.
            final ICommand nextCommand = executionChain.pop();
            final CommandContext nextContext = contextChain.pop();
            builder.andThen( () -> {
                
                nextCommand.execute( nextContext );
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
     * Obtains the command chain of the given command with the given arguments.<br>
     * That is, identifies the list of subcommands that matches the arguments.
     *
     * @param command Main command called.
     * @param args Arguments passed in to the command.
     * @return The chain of the command and its subcommands that match args.
     */
    private List<ICommand> getSubcommands( ICommand command, List<String> args ) {
        
        List<ICommand> chain = new ArrayList<>( args.size() );
        if ( args.isEmpty() ) {
            return chain; // No args left, so no more possible subcommands.
        }
        
        String arg = args.remove( 0 ); // Get next arg.
        PriorityQueue<ICommand> possible = new PriorityQueue<>();
        for ( ICommand candidate : command.getSubCommands() ) { // Check each subcommand.
            
            if ( candidate.getAliases().contains( arg ) ) { // Subcommand has matching alias.
                possible.add( candidate ); // Adds to possible list.
            }
            
        }
        if ( !possible.isEmpty() ) { // At least one subcommand matched.
            if ( LOG.isTraceEnabled() ) {
                LOG.trace( "Identified subcommand \"" + possible.peek().getName() + "\"" );
            }
            chain.add( possible.peek() ); // Gets the matching command with highest precedence.
            chain.addAll( getSubcommands( possible.peek(), args ) ); // Adds the chain for the found subcommand.
        }
        return chain; // Return command chain found.
        
    }
    
    /**
     * Builds the execution chain for the given commands with the given arguments.<br>
     * That is, gets the commands that should be executed according to the {@link ICommand#executeParent()}
     * setting of the commands, and the context for each of these commands.
     * <p>
     * After this method returns, the command on top of the stack is the first one that should be
     * executed, with the other commands in the stack also being retrieved in the order that
     * they should be executed. The context chain has the matching order for the contexts.
     *
     * @param executionChain Where the commands to be executed should be added.
     * @param contextChain Where the context of the commands to be executed should be added.
     * @param commands The commands that were triggered.
     * @param args The args received in the message, including the main command signature.
     * @param event The event that triggered the command.
     */
    private void buildExecutionChain( Stack<ICommand> executionChain, Stack<CommandContext> contextChain,
            List<ICommand> commands, List<String> args, MessageReceivedEvent event ) {
        
        if ( commands.isEmpty() ) {
            return; // No more commands to add.
        }
        List<String> curArgs = args.subList( commands.size(), args.size() ); // Gets the args.
        ICommand curCommand = commands.remove( commands.size() ); // Gets the last command.
        executionChain.push( curCommand ); // Add command to chain.
        contextChain.push( new CommandContext( event, curCommand, curArgs ) ); // Make the command's context.
        if ( curCommand.executeParent() ) { // If specified, add the command before it to the chain.
            buildExecutionChain( executionChain, contextChain, commands, args, event );
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
