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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
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
   
    
    /**
     * Matches one or more whitespaces.
     */
    private static final String WHITE_SPACE_REGEX = "\\s+";
    private static final Pattern WHITE_SPACE = Pattern.compile( WHITE_SPACE_REGEX );
    
    /**
     * Regex that matches a string that starts with a quote and, at some point before the end,
     * has another quote that either is followed by one or more whitespaces or is the last character
     * in the string.
     * <p>
     * eg, if the string is an argument string, matches it if the first argument in the string is between
     * quotes (starts with a quote and end with a quote, followed by a whitespace or the end of the string).
     */
    private static final String QUOTED_ARG_REGEX = "\\A\".*\"(?:" + WHITE_SPACE_REGEX + ".*)?\\Z";
    private static final int QUOTED_ARG_FLAGS = Pattern.DOTALL;
    private static final Pattern QUOTED_ARG = Pattern.compile( QUOTED_ARG_REGEX, QUOTED_ARG_FLAGS );
    
    /**
     * Matches a quote followed by one or more whitespaces or end of string.
     */
    private static final String CLOSING_QUOTE_REGEX = "\"(?:" + WHITE_SPACE_REGEX + "|\\Z)";
    private static final Pattern CLOSING_QUOTE = Pattern.compile( CLOSING_QUOTE_REGEX );
    
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
        String message = event.getMessage().getContent().trim();
        if ( message.isEmpty() ) {
            return; // Empty message.
        }
        
        /* Identify command */
        String[] split = WHITE_SPACE.split( message, 2 ); // Split main command and args.
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Parsing command " + split[0] );
        }
        ICommand mainCommand = registry.parseCommand( split[0] );
        if ( mainCommand == null ) {
            LOG.trace( "No command found." );
            return; // No recognized command.
        }
        if ( LOG.isTraceEnabled() ) {
            LOG.trace( "Identified main command " +
                    String.format( COMMAND_FORMAT, split[0], mainCommand.getName() ) + "." );
        }
        
        /* Get command chain */
        List<String> args = splitArgs( ( split.length == 2 ) ? split[1] : "" );
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Parsing args " + args );
        }
        List<ICommand> commands = new ArrayList<>();
        commands.add( mainCommand );
        commands.addAll( getSubcommands( mainCommand, args ) ); // Identify subcommands.
        final ICommand command = commands.get( commands.size() - 1 ); // Actual command is the last
        List<String> actualArgs = args.subList( commands.size() - 1, args.size() );  // subcommand.
        final CommandContext context = new CommandContext( event, command, actualArgs );
        if ( LOG.isTraceEnabled() ) {
            LOG.trace( "Identified command " +
                    String.format( COMMAND_FORMAT, getCommandSignature( split[0], args,
                            commands.size() - 1 ), command.getName() ) );
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
        if ( !mainCommand.getRegistry().contextCheck( context ) ) {
            LOG.trace( "Registry context check failed." );
            return; // Registry disabled under calling context.
        }
        
        /* Check if the caller is allowed to call the command. */
        if ( LOG.isDebugEnabled() ) {
            LOG.debug( "Command {} - checking permission", getCommandTrace( command, event ) );
        }
        RequestBuilder errorBuilder = new RequestBuilder( event.getClient() ); // Builds request for
        errorBuilder.shouldBufferRequests( true ).setAsync( true );            // error handler.
        errorBuilder.onMissingPermissionsError( ( exception ) -> {
            // In case error handler hits a MissingPermissions error.
            LOG.warn( "Lacking permissions to execute operation.", exception );
            
        });
        errorBuilder.onDiscordError( ( exception ) -> {
            // In case error handler hits a Discord error.
            LOG.error( "Discord error encountered while performing operation.", exception );
        
        });
        if ( command.isNSFW() && event.getChannel().isNSFW() ) {
            LOG.debug( "Channel is not NSFW." );
            errorBuilder.doAction( () -> { // Channel needs to be marked NSFW.
                
                command.onFailure( context, FailureReason.CHANNEL_NOT_NSFW );
                return true;
                
            }).execute();
            return;
        }
        if ( command.requiresOwner() && !event.getClient().getApplicationOwner().equals( event.getAuthor() ) ) {
            LOG.debug( "Caller is not the owner of the bot." );
            errorBuilder.doAction( () -> { // Command can only be called by bot owner.
                
                command.onFailure( context, FailureReason.USER_NOT_OWNER );
                return true;
                
            }).execute();
            return;
        }
        EnumSet<Permissions> requiredPermissions = EnumSet.noneOf( Permissions.class );
        EnumSet<Permissions> requiredGuildPermissions = EnumSet.noneOf( Permissions.class );
        ListIterator<ICommand> iter = commands.listIterator( commands.size() );
        while ( iter.hasPrevious() ) { // Get requirements for each command in the chain.
            
            ICommand cur = iter.previous(); // Get for the next in the chain.
            requiredPermissions.addAll( cur.getRequiredPermissions() );
            requiredGuildPermissions.addAll( cur.getRequiredGuildPermissions() );
            if ( !cur.requiresParentPermissions() ) {
                break; // Current command does not require parent's permissions.
            }
            
        }
        EnumSet<Permissions> channelPermissions = event.getChannel().getModifiedPermissions( event.getAuthor() );
        if ( !channelPermissions.containsAll( requiredPermissions ) ) {
            LOG.debug( "Caller does not have the required permissions in the channel." );
            errorBuilder.doAction( () -> { // User does not have required channel-overriden permissions.
                
                command.onFailure( context, FailureReason.USER_MISSING_PERMISSIONS );
                return true;
                
            }).execute();
            return;
        }
        if ( event.getGuild() != null ) { // If message came from guild, check required permissions for it.
            EnumSet<Permissions> guildPermissions = event.getAuthor().getPermissionsForGuild( event.getGuild() );
            if ( !guildPermissions.containsAll( requiredGuildPermissions ) ) {
                LOG.debug( "Caller does not have the required permissions in the server." );
                errorBuilder.doAction( () -> { // User does not have required server-wide permissions.
                    
                    command.onFailure( context, FailureReason.USER_MISSING_GUILD_PERMISSIONS );
                    return true;
                    
                }).execute();
                return;
            }
        }
        
        /* Get execution chain */
        LOG.trace( "Permission check passed. Preparing to execute." );
        Stack<ICommand> executionChain = new Stack<>(); // Gets all the parent commands that should also
        buildExecutionChain( executionChain, commands ); // be executed.
        
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
            LOG.error( "Discord error encountered while performing operation.", exception );
        
        });
        final AtomicBoolean operationException = new AtomicBoolean();
        builder.onGeneralError( ( exception ) -> { // Mark that an unexpected exception was thrown.
            
            operationException.set( true );
            LOG.error( "Unexpected exception thrown while performing operation.", exception );
            context.setHelper( exception ); // Store exception in context.
            
        });
        final ICommand firstCommand = executionChain.pop();
        builder.doAction( () -> {
            // Execute the first command in the chain.
            if ( LOG.isTraceEnabled() ) {
                LOG.trace( "Executing \"" + firstCommand.getName() + "\"" );
            }
            return firstCommand.execute( context );
            
        });
        while ( !executionChain.isEmpty() ) {
            // Execute each subsequent command in the chain.
            final ICommand nextCommand = executionChain.pop();
            builder.andThen( () -> {
                
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace( "Executing \"" + nextCommand.getName() + "\"" );
                }
                return nextCommand.execute( context );
                
            });
            
        }
        builder.elseDo( () -> { // In case command fails.
            
            LOG.debug( "Command failed. Executing failure handler." );
            if ( permissionsError.get() ) { // Failed due to missing permissions.
                command.onFailure( context, FailureReason.BOT_MISSING_PERMISSIONS );
            } else if ( discordError.get() ) { // Failed due to miscellaneous error.
                command.onFailure( context, FailureReason.DISCORD_ERROR );
            } else if ( operationException.get() ) { // Failed due to unexpected exception.
                command.onFailure( context, FailureReason.COMMAND_OPERATION_EXCEPTION );
            } else { // One of the commands returned false.
                command.onFailure( context, FailureReason.COMMAND_OPERATION_FAILED );
            }
            return true;
            
        });
        if ( command.deleteCommand() ) { // Command message should be deleted after
            builder.andThen( () -> {     // successful execution.
                
                LOG.debug( "Successful execution. Deleting command message." );
                try {
                    event.getMessage().delete();
                } catch ( MissingPermissionsException e ) {
                    LOG.warn( "Missing permissions to delete message.", e );
                } catch ( DiscordException e ) {
                    LOG.error( "Error encountered while deleting message.", e );
                }
                return true;
                
            });
        }
        builder.andThen( () -> { // In case command succeeds.
            
            LOG.debug( "Command succeeded." );
            Thread.sleep( command.getOnSuccessDelay() ); // Wait specified time.
            LOG.debug( "Executing success handler." );
            command.onSuccess( context ); // Execute success operation.
            return true;
            
        });
        
        /* Execute command */
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Executing command " + getCommandTrace( command, event ) );
        }
        builder.execute(); // Execute the command.
        
    }
    
    /**
     * Splits the given argument string into a list of arguments.
     * <p>
     * An "argument" is considered as text preceded by either a space or the start of the string, and
     * followed by either a space or the end of the string. Text between two double-quotes (where the first
     * quote is preceded by either a space or the start of the string, and the second is followed by either
     * a space or the end of the string) is considered a single argument, not including the quotes (it may
     * have spaces within it (or other double-quotes, as long as they are followed by non-space characters).
     *
     * @param argString The argument string to be split. Is assumed to not have any leading or trailing
     *                  whitespace.
     * @return The arguments in the given string.
     */
    private List<String> splitArgs( String argString ) {
        
        if ( argString.isEmpty() ) {
            return new LinkedList<>();
        }
        
        Pattern regex;
        if ( QUOTED_ARG.matcher( argString ).matches() ) { // Next argument is between quotes.
            regex = CLOSING_QUOTE;
            argString = argString.substring( 1 ); // Remove the first quote.
        } else { // Next argument ends at the next space.
            regex = WHITE_SPACE;
        }
        String[] split = regex.split( argString, 2 ); // Split off the first argument.
        List<String> args = splitArgs( ( split.length == 2 ) ? split[1] : "" ); // Split the remaining args.
        args.add( 0, split[0] ); // Insert first arg at the beginning.
        
        return args; // Return the split args.
        
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
     * @param command The command from the message received.
     * @param args The args from the message received.
     * @param subCommandAmount How many subcommands are in the args.
     * @return The signature of the command.
     */
    private static String getCommandSignature( String command, List<String> args, int subCommandAmount ) {
        
        StringBuilder signature = new StringBuilder( command );
        Iterator<String> subCommands = args.iterator();
        while ( subCommandAmount > 0 ) {
            
            signature.append( ' ' );
            signature.append( subCommands.next() );
            subCommandAmount--;
            
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
                event.getChannel().getName(), 
                ( event.getGuild() != null ) ? event.getGuild().getName() : "<private>" );
        
    }

}
