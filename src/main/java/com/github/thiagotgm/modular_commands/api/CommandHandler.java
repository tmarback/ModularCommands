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
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.RequestBuilder;

/**
 * Listener that identifies commands from text messages and executes them.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-14
 */
public class CommandHandler implements IListener<MessageReceivedEvent> {
    
    private final CommandRegistry registry;
    
    /**
     * Creates a command handler that uses a given registry.
     *
     * @param registry Registry that this handler should use.
     */
    public CommandHandler( CommandRegistry registry ) {
        
        this.registry = registry;
        
    }

    @Override
    public void handle( MessageReceivedEvent event ) {

        /* Get command and args */
        String message = event.getMessage().getContent();
        List<String> args = Arrays.asList( message.trim().split( " +" ) );
        if ( args.size() == 0 ) {
            return; // Empty message.
        }
        
        /* Identify command */
        List<String> argsCopy = new LinkedList<>( args );
        final ICommand command = registry.parseCommand( argsCopy.remove( 0 ) );
        if ( command == null ) {
            return; // No recognized command.
        }
        
        /* Get execution chain */
        List<ICommand> commands = new ArrayList<>();
        commands.add( command );
        commands.addAll( getSubcommands( command, argsCopy ) );
        Stack<ICommand> executionChain = new Stack<>();
        Stack<CommandContext> contextChain = new Stack<>();
        buildExecutionChain( executionChain, contextChain, commands, args, event );
        
        /* Build request */
        RequestBuilder builder = new RequestBuilder( event.getClient() );
        builder.shouldBufferRequests( true ).setAsync( true );
        final AtomicBoolean permissionsError = new AtomicBoolean();
        builder.onMissingPermissionsError( ( exception ) -> {
            
            permissionsError.set( true ); // Mark that a permission error occurred.
            
        });
        final AtomicBoolean discordError = new AtomicBoolean();
        builder.onDiscordError( ( exception ) -> {
            
            discordError.set( true ); // Mark that a Discord error occurred.
        
        });
        final ICommand firstCommand = executionChain.pop();
        final CommandContext firstContext = contextChain.pop();
        builder.doAction( () -> {
            // Execute the first command in the chain.
            firstCommand.execute( firstContext );
            return true;
            
        });
        CommandContext lastContext = firstContext;
        while ( !executionChain.isEmpty() ) {
            // Execute each subsequent command in the chain.
            final ICommand nextCommand = executionChain.pop();
            final CommandContext nextContext = contextChain.pop();
            builder.andThen( () -> {
                
                nextCommand.execute( nextContext );
                return true;
                
            });
            lastContext = nextContext;
            
        }
        final CommandContext context = lastContext; // Mark context of last command.
        builder.elseDo( () -> { // In case command fails.
            
            if ( permissionsError.get() ) { // Failed due to missing permissions.
                command.onFailure( context, FailureReason.BOT_MISSING_PERMISSIONS );
            } else if ( discordError.get() ) { // Failed due to miscellaneous error.
                command.onFailure( context, FailureReason.DISCORD_ERROR );
            }
            return true;
            
        });
        builder.andThen( () -> { // In case command succeeds.
            
            Thread.sleep( command.getOnSuccessDelay() ); // Wait specified time.
            command.onSuccess( context ); // Execute success operation.
            return true;
            
        });
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

}
