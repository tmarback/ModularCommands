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

package com.github.thiagotgm.modular_commands.included;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MessageBuilder;

/**
 * Command that displays the commands currently registered and info about each of them.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-25
 */
public class HelpCommand {
    
    public static final String MAIN_COMMAND_NAME = "Default Help Command";
    
    private static final String BLOCK_PREFIX = "```java\n";
    private static final String BLOCK_SUFFIX = "```";
    private static final int BLOCK_EXTRA = BLOCK_PREFIX.length() + BLOCK_SUFFIX.length();
    private static final String LIST_TITLE = "[COMMAND LIST]\n";
    
    private CommandRegistry registry;
    private volatile long lastUpdated;
    private Map<ICommand, List<String>> buffer;

    /**
     * Constructs a new instance.
     */
    public HelpCommand() {
        
        this.registry = null;
        this.lastUpdated = 0;
        
    }
    
    /**
     * Builds the command-signature buffer to reflect the current state of the registry.
     */
    private void bufferCommandList() {
        
        buffer = new HashMap<>(); // Initialize new buffer.
        bufferRegistry( registry ); // Start buffering from the root registry.
        lastUpdated = System.currentTimeMillis(); // Record update time.
        
    }
    
    /**
     * Buffers the commands from a given registry and its subregistries.
     *
     * @param registry The registry to get commands from.
     */
    private void bufferRegistry( CommandRegistry registry ) {
        
        for ( ICommand command : registry.getCommands() ) { // Check each command in the 
            
            List<String> signatures = new ArrayList<>( command.getAliases().size() );
            for ( String signature : command.getSignatures() ) { // Check each of the command
                                                                 // signatures.
                if ( this.registry.parseCommand( signature, false ) == command ) {
                    signatures.add( signature ); // Signature is callable. Record it.
                }
                
            }
            buffer.put( command, signatures ); // Store valid signatures in buffer.
            
        }
        
        for ( CommandRegistry subRegistry : registry.getSubRegistries() ) {
            
            bufferRegistry( subRegistry ); // Buffer subregistries.
            
        }
        
    }
    
    /**
     * Checks if the current buffer is up-to-date, and rebuilds it if it is not.
     * <p>
     * After the call to this method, the buffer will reflect the current state of the
     * registry the command is registered to.
     *
     * @param curRegistry The registry that the command is currently registered to.
     */
    private synchronized void ensureUpdatedBuffer( CommandRegistry curRegistry ) {
        
        if ( ( registry != curRegistry ) || // Registry updated or changed registries
             ( registry.getLastChanged() > lastUpdated ) ) { // since last buffer was made.
            registry = curRegistry;
            bufferCommandList(); // Update buffer.
        }
        
    }
    
    /**
     * Formats the command into a single line that shortly describes it.
     *
     * @param command The command to format.
     * @return A string that describes the command in a single line, or null if the command has
     *         no callable signatures.
     */
    private String formatCommandShort( ICommand command ) {
        
        List<String> signatureList = buffer.get( command );
        if ( signatureList.isEmpty() ) {
            return null;
        }
        String signatures = signatureList.toString(); // Get signature list.
        StringBuilder builder = new StringBuilder();
        builder.append( signatures, 1, signatures.length() - 1 ); // Remove brackets.
        builder.append( " - \"" );
        builder.append( command.getName() );
        builder.append( "\" - " );
        builder.append( command.getDescription() );
        return builder.toString();
        
    }
    
    /**
     * Formats a collection of commands into a list, where each line describes a
     * single command.
     *
     * @param commands The commands to make a list of.
     * @return The formatted command list.
     */
    private List<String> formatCommandList( Collection<ICommand> commands, int firstBlockLength,
            int otherBlockLength ) {
        
        /* Gets the formatted commands and sorts them */
        SortedSet<String> commandStrings = new TreeSet<>();
        for ( ICommand command : commands ) {
            
            String formatted = formatCommandShort( command );
            if ( formatted != null ) { // Include the command if it has callable signatures.
                commandStrings.add( formatted );
            }
            
        }
        
        /* Adds all formatted commands into different lines */
        List<String> blocks = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int maxBlockLength = firstBlockLength;
        for ( String commandString : commandStrings ) {
            
            if ( ( builder.length() + commandString.length() + 1 ) > maxBlockLength ) {
                // Reached max block length.
                if ( blocks.isEmpty() ) { // Update max lenght to the other blocks.
                    maxBlockLength = otherBlockLength;
                }
                blocks.add( builder.toString() ); // Record current block.
                builder = new StringBuilder(); // Start another block.
            }
            builder.append( commandString );
            builder.append( '\n' );
            
        }
        if ( builder.length() > 0 ) {
            blocks.add( builder.toString() ); // Record last block.
        }
        
        return blocks;
        
    }
    
    @MainCommand(
            name = MAIN_COMMAND_NAME,
            aliases = { "help" },
            essential = true,
            replyPrivately = false,
            overrideable = false,
            canModifySubCommands = false
            )
    public void helpCommand( CommandContext context ) {
        
        IDiscordClient client = context.getEvent().getClient();
        CommandRegistry curRegistry = CommandRegistry.getRegistry( client );
        ensureUpdatedBuffer( curRegistry ); // Check if the buffer is up-to-date.
        
        if ( !context.getCommand().getName().equals( MAIN_COMMAND_NAME ) ) {
            return; // A subcommand was called.
        }
        
        if ( context.getArgs().isEmpty() ) {
            int blockSize = IMessage.MAX_MESSAGE_LENGTH - BLOCK_EXTRA;
            int firstBlockSize = blockSize - LIST_TITLE.length();
            List<String> blocks = formatCommandList( registry.getCommands(), firstBlockSize, blockSize );
            MessageBuilder builder = context.getReplyBuilder();
            builder.withContent( BLOCK_PREFIX + LIST_TITLE + blocks.get( 0 ) + BLOCK_SUFFIX ).build();
            for ( int i = 1; i < blocks.size(); i++ ) {
                
                builder.withContent( BLOCK_PREFIX + blocks.get( i ) + BLOCK_SUFFIX ).build();
                
            }
        }
        
    }

}
