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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import com.github.thiagotgm.modular_commands.registry.ClientCommandRegistry;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.RequestBuilder;

/**
 * Command that displays the commands currently registered and info about each of them.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-25
 */
public class HelpCommand {
    
    public static final String MAIN_COMMAND_NAME = "Default Help Command";
    private static final String REGISTRY_LIST_SUBCOMMAND_NAME = "Registry List";
    private static final String PUBLIC_HELP_SUBCOMMAND_NAME = "Public Default Help Command";
    
    private static final String BLOCK_PREFIX = "```java\n";
    private static final String BLOCK_SUFFIX = "```";
    private static final int BLOCK_EXTRA = BLOCK_PREFIX.length() + BLOCK_SUFFIX.length();
    private static final int BLOCK_SIZE = IMessage.MAX_MESSAGE_LENGTH - BLOCK_EXTRA;
    
    private static final String LIST_TITLE = "[COMMAND LIST]\n";
    private static final String SUBREGISTRY_TITLE = "[%s (%s) COMMAND LIST]\n";
    private static final String SUBREGISTRY_PATH_DELIMITER = "::";
    private static final String COMMAND_TITLE = "[COMMAND DETAILS]\n";
    
    private ClientCommandRegistry registry;
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
    private synchronized void ensureUpdatedBuffer( ClientCommandRegistry curRegistry ) {
        
        if ( ( registry != curRegistry ) || // Registry updated or changed registries
             ( registry.getLastChanged() > lastUpdated ) ) { // since last buffer was made.
            registry = curRegistry;
            bufferCommandList(); // Update buffer.
        }
        
    }
    
    /**
     * Formats a command into a single line that shortly describes it.
     *
     * @param command The command to format.
     * @return A string that describes the command in a single line, or null if the command has
     *         no callable signatures.
     */
    private String formatCommandShort( ICommand command ) {
        
        List<String> signatureList = buffer.get( command );
        if ( signatureList.isEmpty() ) {
            return null; // Command has no callable signatures.
        }
        String signatures = signatureList.toString(); // Get signature list.
        StringBuilder builder = new StringBuilder();
        if ( !command.isEffectivelyEnabled() ) {
            builder.append( "[DISABLED] " );
        }
        builder.append( signatures, 1, signatures.length() - 1 ); // Remove brackets.
        builder.append( " - " );
        builder.append( command.getDescription() );
        return builder.toString();
        
    }
    
    /**
     * Formats a command into all the information a user might need.
     *
     * @param command The command to format.
     * @param parent The parent command. Can be just null if the command is a main command.
     * @param mainCommand The parent of the command that is a main command. Can be the command itself.
     * @return The string that fully describes the command.
     */
    private String formatCommandLong( ICommand command, ICommand parent, ICommand mainCommand ) {
        
        /* Add command name */
        StringBuilder builder = new StringBuilder( "Name: " );
        builder.append( command.getName() );
        builder.append( '\n' );
        
        /* Add command prefix if this is a main command and has one */
        if ( !command.isSubCommand() ) {
            String prefix = command.getPrefix();
            if ( prefix != null ) {
                builder.append( "Prefix: " );
                builder.append( prefix );
                builder.append( '\n' );
            }
        }
        
        /* Add aliases */
        List<String> aliases = new ArrayList<>( command.getAliases().size() );
        String effectivePrefix = mainCommand.getEffectivePrefix();
        if ( command.isSubCommand() ) {
            for ( String alias : command.getAliases() ) {
                
                if ( parent.getSubCommand( alias ) == command ) {
                    aliases.add( alias );
                }
                
            }
        } else {
            List<String> signatureList = buffer.get( command );
            int prefixSize = effectivePrefix.length();
            for ( String signature : signatureList ) {
                // Get each callable alias.
                aliases.add( signature.substring( prefixSize ) );
                
            }
        }
        if ( aliases.isEmpty() ) {
            return null; // Command has no callable aliases.
        }
        builder.append( "Aliases: " );
        builder.append( aliases );
        builder.append( '\n' );
        
        /* Add description */
        builder.append( "Description: " );
        builder.append( command.getDescription() );
        builder.append( '\n' );
        
        /* Add usage */
        builder.append( "Usage: " );
        String usage = command.getUsage();
        if ( usage.startsWith( "{}" ) ) { // Usage has prefix placeholder.
            builder.append( effectivePrefix ); // Substitute with effective
            builder.append( usage, 2, usage.length() ); // prefix.
        } else {
            builder.append( usage );
        }
        builder.append( '\n' );
        
        /* Add subcommands */
        SortedSet<List<String>> subCommandAliases = new TreeSet<>( ( s1, s2 ) -> {
            return s1.get( 0 ).compareTo( s2.get( 0 ) ); // Compare head elements.
        });
        for ( ICommand subCommand : command.getSubCommands() ) {
            
            aliases = new ArrayList<>(); // Get callable alias of the subcommand.
            for ( String alias : subCommand.getAliases() ) {
                
                if ( command.getSubCommand( alias ) == subCommand ) {
                    aliases.add( alias ); // Alias is callable.
                }
                
            }
            if ( !aliases.isEmpty() ) { // This subcommand had callable aliases.
                subCommandAliases.add( aliases );
            }
            
        }
        builder.append( "Subcommands: " );
        if ( subCommandAliases.isEmpty() ) {
            builder.append( "N/A" );
        } else {
            String subCommands = subCommandAliases.toString();
            builder.append( subCommands, 1, subCommands.length() - 1 );
        }
        builder.append( '\n' );
        
        /* Add some options */
        if ( command.isNSFW() ) {
            builder.append( "- NSFW\n" );
        }
        if ( command.requiresOwner() ) {
            builder.append( "- Bot owner only\n" );
        }
        if ( command.isEssential() ) {
            builder.append( "- Essential\n" );
        }
        if ( !command.isEnabled() ) {
            builder.append( "- Disabled\n" );
        } else if ( !command.isEffectivelyEnabled() ) {
            builder.append( "- Registry disabled\n" );
        }
        
        return builder.toString();
        
    }
    
    /**
     * Formats a collection of commands into a list, where each line describes a
     * single command.
     * <p>
     * All blocks are small enough to fit into a single message with the {@link #BLOCK_PREFIX} and
     * {@link #BLOCK_SUFFIX}. If the first block needs to be smaller, the reduction to its maximum
     * size can be specified.
     *
     * @param commands The commands to make a list of.
     * @param firstBlockReduction How much the maximum size of the first block is smaller than the
     *                            normal block size.
     * @return The formatted command list split in blocks.
     */
    private List<String> formatCommandList( Collection<ICommand> commands, int firstBlockReduction ) {
        
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
        int maxBlockLength = BLOCK_SIZE - firstBlockReduction;
        for ( String commandString : commandStrings ) {
            
            if ( ( builder.length() + commandString.length() + 1 ) > maxBlockLength ) {
                // Reached max block length.
                if ( blocks.isEmpty() ) { // Update max lenght to the other blocks.
                    maxBlockLength = BLOCK_SIZE;
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
            description = "Displays all the registered commands. If a command signature is specified, "
                    + "displays information about that command.",
            usage = "{}help [(command signature), registries]",
            essential = true,
            replyPrivately = true,
            overrideable = false,
            canModifySubCommands = false,
            subCommands = { REGISTRY_LIST_SUBCOMMAND_NAME, PUBLIC_HELP_SUBCOMMAND_NAME }
            )
    public void helpCommand( CommandContext context ) {
        
        IDiscordClient client = context.getEvent().getClient();
        ClientCommandRegistry curRegistry = CommandRegistry.getRegistry( client );
        ensureUpdatedBuffer( curRegistry ); // Check if the buffer is up-to-date.
        
        if ( context.getCommand().getName().equals( REGISTRY_LIST_SUBCOMMAND_NAME ) ) {
            return; // Registry list subcommand was called.
        }
        
        if ( context.getArgs().isEmpty() ) { // No command specified.
            RequestBuilder request = new RequestBuilder( this.registry.getClient() )
                    .shouldBufferRequests( true );
            List<String> blocks = formatCommandList( registry.getCommands(), LIST_TITLE.length() );
            MessageBuilder builder = context.getReplyBuilder();
            final String first = BLOCK_PREFIX + LIST_TITLE + blocks.get( 0 ) + BLOCK_SUFFIX;
            request.doAction( () -> {
                builder.withContent( first ).build();
                return true;
            });
            for ( int i = 1; i < blocks.size(); i++ ) {
                
                final String next = BLOCK_PREFIX + blocks.get( i ) + BLOCK_SUFFIX;
                request.andThen( () -> {
                    builder.withContent( next ).build();
                    return true;
                });
                builder.withContent( next ).build();
                
            }
            request.execute();
        } else { // A command was specified.
            Iterator<String> args = context.getArgs().iterator();
            ICommand command = registry.parseCommand( args.next(), false );
            if ( command == null ) {
                return; // No command with that signature.
            }
            
            ICommand mainCommand = command;
            ICommand parent = null;
            while ( args.hasNext() ) { // Identify subcommands.
                
                parent = command;
                command = command.getSubCommand( args.next() );
                if ( command == null ) {
                    return; // No subcommand with the argument alias.
                }
                
            }
            MessageBuilder builder = context.getReplyBuilder();
            builder.withContent( BLOCK_PREFIX );
            builder.appendContent( COMMAND_TITLE );
            builder.appendContent( formatCommandLong( command, parent, mainCommand ) );
            builder.appendContent( BLOCK_SUFFIX );
            builder.build(); // Send message.
        }
        
    }
    
    @SubCommand(
            name = PUBLIC_HELP_SUBCOMMAND_NAME,
            aliases = { "here" },
            description = "Displays all the registered commands. If a command signature is specified, "
                    + "displays information about that command. However, this subcommand outputs "
                    + "to the same channel instead of a private message.",
            usage = "{}help here [command signature]",
            canModifySubCommands = false,
            executeParent = true
            )
    public void publicHelpCommand( CommandContext context ) {
        
        // Do nothing.
        
    }
    
    @SubCommand(
            name = REGISTRY_LIST_SUBCOMMAND_NAME,
            aliases = { "registries" },
            description = "Displays all the registered commands, categorized by the subregistries "
                    + "that they are registered in.",
            usage = "{}help registries",
            essential = true,
            replyPrivately = true,
            canModifySubCommands = false,
            executeParent = true,
            requiresOwner = true
            )
    public void moduleListCommand( CommandContext context ) {
        
        Stack<CommandRegistry> registries = new Stack<>();
        registries.push( registry );
        final MessageBuilder builder = context.getReplyBuilder();
        
        while ( !registries.isEmpty() ) { // For each registry.
            
            CommandRegistry registry = registries.pop();
            RequestBuilder request = new RequestBuilder( this.registry.getClient() )
                    .shouldBufferRequests( true );
            
            /* Get registry path */
            String path;
            if ( registry.getRegistry() != null ) { // Not root registry.
                List<String> pathList = new LinkedList<>();
                CommandRegistry cur = registry;
                while ( cur != this.registry ) { // Get all parents except root
                    
                    pathList.add( 0, cur.getName() ); // Add parent to beginning of pathlist.
                    cur = cur.getRegistry();
                    
                }
                path = String.join( SUBREGISTRY_PATH_DELIMITER, pathList );
            } else { // Root registry.
                path = "Core";
            }
            
            /* Makes and sends the command list for the current registry */
            String title = String.format( SUBREGISTRY_TITLE, path, registry.getQualifier() );
            if ( !registry.isEffectivelyEnabled() ) {
                title = "[DISABLED] " + title;
            }
            List<String> blocks = formatCommandList( registry.getRegisteredCommands(), title.length() );
            builder.withContent( title );
            final String first = BLOCK_PREFIX + title + blocks.get( 0 ) + BLOCK_SUFFIX;
            request.doAction( () -> {
                builder.withContent( first ).build();
                return true;
            });
            for ( int i = 1; i < blocks.size(); i++ ) {
                
                final String next = BLOCK_PREFIX + blocks.get( i ) + BLOCK_SUFFIX;
                request.andThen( () -> {
                    builder.withContent( next ).build();
                    return true;
                });
                builder.withContent( next ).build();
                
            }
            request.execute();
            
            System.out.println( registry.getSubRegistries() );
            for ( CommandRegistry subRegistry : registry.getSubRegistries() ) {
                // Add each subregistry to the stack.
                registries.push( subRegistry );
                
            }
            
        }
        
    }

}
