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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import com.github.thiagotgm.modular_commands.registry.ClientCommandRegistry;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.RequestBuilder;

/**
 * Command set that displays the commands currently registered and info about
 * each of them.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-25
 */
public class HelpCommand {

    private static final Logger LOG = LoggerFactory.getLogger( HelpCommand.class );

    public static final String MAIN_COMMAND_NAME = "Default Help Command";
    private static final String REGISTRY_LIST_SUBCOMMAND_NAME = "Registry List";
    private static final String REGISTRY_DETAILS_SUBCOMMAND_NAME = "Registry Details";
    private static final String PUBLIC_HELP_SUBCOMMAND_NAME = "Public Default Help Command";

    private static final String BLOCK_PREFIX = "```\n";
    private static final String BLOCK_SUFFIX = "```";
    private static final int BLOCK_EXTRA = BLOCK_PREFIX.length() + BLOCK_SUFFIX.length();
    private static final int BLOCK_SIZE = IMessage.MAX_MESSAGE_LENGTH - BLOCK_EXTRA;

    private static final String LIST_TITLE = "[COMMAND LIST]\n";
    private static final String SUBREGISTRY_TITLE = "[REGISTRY %s - COMMAND LIST]\n";
    private static final String REGISTRY_TITLE = "[REGISTRY DETAILS]\n";
    private static final String DISABLED_TAG = "[DISABLED] ";

    private static final String EMPTY_REGISTRY = "<no commands>\n";

    private static final Pattern LINE_SPLITTER_PATTERN = Pattern.compile( "\\s*\n\\s*" );
    
    private static final String TABBED_LINE = '\n' + StringUtils.repeat( "\u200B\t", 5 );

    private static final Color EMBED_COLOR = Color.WHITE;

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
     * Builds the command-signature buffer to reflect the current state of the
     * registry.
     */
    private void bufferCommandList() {

        LOG.info( "Buffering command aliases." );
        buffer = new HashMap<>(); // Initialize new buffer.
        bufferRegistry( registry ); // Start buffering from the root registry.
        lastUpdated = System.currentTimeMillis(); // Record update time.
        LOG.info( "Buffering finished." );

    }

    /**
     * Buffers the commands from a given registry and its subregistries.
     *
     * @param registry
     *            The registry to get commands from.
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
     * After the call to this method, the buffer will reflect the current state of
     * the registry the command is registered to.
     *
     * @param curRegistry
     *            The registry that the command is currently registered to.
     */
    private synchronized void ensureUpdatedBuffer( ClientCommandRegistry curRegistry ) {

        if ( ( registry != curRegistry ) || // Registry updated or changed registries
                ( registry.getLastChanged() > lastUpdated ) ) { // since last buffer was made.
            registry = curRegistry;
            bufferCommandList(); // Update buffer.
        }

    }

    /**
     * Ensures all buffered data is up to date before executing a command.
     *
     * @param context
     *            The context of the command being executed.
     */
    private void update( CommandContext context ) {

        IDiscordClient client = context.getEvent().getClient();
        ClientCommandRegistry curRegistry = CommandRegistry.getRegistry( client );
        ensureUpdatedBuffer( curRegistry ); // Check if the buffer is up-to-date.

    }

    /**
     * Parses a command's description into multiple lines.
     * <p>
     * The <i>short description</i> is the first line of the description, and is
     * displayed in both the command list and in the command details.<br>
     * The <i>detailed description</i> is every line after the first line, and is
     * only displayed in the command details, after the short description.
     * <p>
     * Leading and trailing whitespace in each line is removed.
     *
     * @param command
     *            The command to parse the description of.
     * @return The split command description. The index 0 always exists and contains
     *         the <i>short description</i>, and may be empty, but never
     *         <tt>null</tt>. Any further indices that exist are part of the
     *         <i>detailed description</i>, and may be empty but not <tt>null</tt>.
     *         This implicates that the length of the array is at least 1, and every
     *         valid index in the array contains a non-<tt>null</tt> string.
     */
    private static String[] parseDescription( ICommand command ) {

        String[] lines = LINE_SPLITTER_PATTERN.split( command.getDescription().trim() );
        return lines.length > 0 ? lines : new String[] { "" };

    }

    /**
     * Formats a command into a single line that shortly describes it.
     *
     * @param command
     *            The command to format.
     * @return A string that describes the command in a single line, or null if the
     *         command has no callable signatures.
     */
    private String formatCommandShort( ICommand command ) {

        List<String> signatureList = buffer.get( command );
        if ( signatureList.isEmpty() ) {
            return null; // Command has no callable signatures.
        }
        String signatures = signatureList.toString(); // Get signature list.
        StringBuilder builder = new StringBuilder();
        if ( !command.isEffectivelyEnabled() ) {
            builder.append( DISABLED_TAG );
        }
        builder.append( signatures, 1, signatures.length() - 1 ); // Remove brackets.
        builder.append( " - " );
        builder.append( parseDescription( command )[0] );
        return builder.toString();

    }

    /**
     * Formats a command into all the information a user might need.
     *
     * @param command
     *            The command to format.
     * @param parent
     *            The parent command. Can be just null if the command is a main
     *            command.
     * @param mainCommand
     *            The parent of the command that is a main command. Can be the
     *            command itself.
     * @return The string that fully describes the command.
     */
    private EmbedObject formatCommandLong( ICommand command, ICommand parent, ICommand mainCommand ) {

        EmbedBuilder builder = new EmbedBuilder().withColor( EMBED_COLOR );

        /* Add command name */
        builder.withTitle( command.getName() );

        /* Add description */
        String description = String.join( TABBED_LINE, parseDescription( command ) );
        if ( !description.isEmpty() ) {
            builder.withDescription( description );
        }

        /* Add prefix if this is a main command */
        String effectivePrefix = mainCommand.getEffectivePrefix();
        if ( !command.isSubCommand() ) {
            String title = command.getPrefix() == null ? "Inherited Prefix" : "Prefix";
            builder.appendField( title, effectivePrefix, false );
        }

        /* Add aliases */
        List<String> aliases = new ArrayList<>( command.getAliases().size() );
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
        builder.appendField( "Aliases", aliases.toString(), false );

        /* Add usage */
        String usage = command.getUsage();
        if ( !usage.isEmpty() ) {
            if ( usage.startsWith( "{}" ) ) { // Usage has prefix placeholder.
                usage = effectivePrefix + usage.substring( 2, usage.length() ); // Substitute with effective prefix.
            }
            builder.appendField( "Usage", usage, false );
        }

        /* Add subcommands */
        SortedSet<List<String>> subCommandAliases = new TreeSet<>( ( s1, s2 ) -> {
            return s1.get( 0 ).compareTo( s2.get( 0 ) ); // Compare head elements.
        } );
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
        String subcommands;
        if ( subCommandAliases.isEmpty() ) {
            subcommands = "N/A";
        } else {
            String subCommands = subCommandAliases.toString();
            subcommands = subCommands.substring( 1, subCommands.length() - 1 );
        }
        builder.appendField( "Subcommands", subcommands, false );

        /* Add permissions */
        EnumSet<Permissions> channelPermissions = command.getRequiredPermissions();
        if ( !channelPermissions.isEmpty() ) {
            builder.appendField( "Required permissions in channel", channelPermissions.toString(), false );
        }
        EnumSet<Permissions> guildPermissions = command.getRequiredGuildPermissions();
        if ( !guildPermissions.isEmpty() ) {
            builder.appendField( "Required permissions in server", guildPermissions.toString(), false );
        }

        /* Add modifiers */
        List<String> modifiers = new LinkedList<>();
        if ( command.isNSFW() ) {
            modifiers.add( "- NSFW" );
        }
        if ( command.requiresOwner() ) {
            modifiers.add( "- Bot owner only" );
        }
        if ( command.requiresParentPermissions() ) {
            modifiers.add( "- Must have the permissions to execute the parent command" );
        }
        if ( command.isEssential() ) {
            modifiers.add( "- Essential" );
        }
        if ( !command.isEnabled() ) {
            modifiers.add( "- Disabled" );
        } else if ( !command.isEffectivelyEnabled() ) {
            modifiers.add( "- Registry disabled" );
        }
        if ( !modifiers.isEmpty() ) {
            builder.appendField( "Modifiers", String.join( "\n", modifiers ), false );
        }

        return builder.build();

    }

    /**
     * Formats a registry into all the information a user might need.
     *
     * @param registry
     *            The registry to format.
     * @return The string that fully describes the registry.
     */
    private String formatRegistry( CommandRegistry registry ) {

        /* Add registry name */
        StringBuilder builder = new StringBuilder( "Name: " );
        builder.append( registry.getName() );
        builder.append( '\n' );

        /* Add prefix */
        String prefix = registry.getPrefix();
        if ( prefix != null ) { // Has a prefix.
            builder.append( "Prefix: " );
            builder.append( prefix );
        } else { // Only has inherited prefix.
            builder.append( "Inherited Prefix: " );
            builder.append( registry.getEffectivePrefix() );
        }
        builder.append( '\n' );

        /* Add some options */
        if ( registry.isEssential() ) {
            builder.append( "- Essential\n" );
        }
        if ( !registry.isEnabled() ) {
            builder.append( "- Disabled\n" );
        } else if ( !registry.isEffectivelyEnabled() ) {
            builder.append( "- Parent registry disabled\n" );
        }

        return builder.toString();

    }

    /**
     * Formats a collection of commands into a list, where each line describes a
     * single command.
     * <p>
     * All blocks are small enough to fit into a single message with the
     * {@link #BLOCK_PREFIX} and {@link #BLOCK_SUFFIX}. If the first block needs to
     * be smaller, the reduction to its maximum size can be specified.
     *
     * @param commands
     *            The commands to make a list of.
     * @param firstBlockReduction
     *            How much the maximum size of the first block is smaller than the
     *            normal block size.
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

        if ( blocks.isEmpty() ) {
            blocks.add( EMPTY_REGISTRY ); // Ensure at least one block.
        }

        return blocks;

    }

    @MainCommand(
            name = MAIN_COMMAND_NAME,
            aliases = { "help" },
            description = "Displays information about registered commands.\nIf a command "
                    + "signature is specified, displays information about that command.\n"
                    + "Else, displays a list of all registered commands.",
            usage = "{}help [here] [command signature]",
            essential = true,
            replyPrivately = true,
            overrideable = false,
            priority = Integer.MAX_VALUE,
            canModifySubCommands = false,
            subCommands = { REGISTRY_LIST_SUBCOMMAND_NAME, REGISTRY_DETAILS_SUBCOMMAND_NAME,
                    PUBLIC_HELP_SUBCOMMAND_NAME } )
    public boolean helpCommand( CommandContext context ) {

        update( context );

        if ( context.getArgs().isEmpty() ) { // No command specified.
            RequestBuilder request = new RequestBuilder( this.registry.getClient() ).shouldBufferRequests( true );
            List<String> blocks = formatCommandList( registry.getCommands(), LIST_TITLE.length() );
            MessageBuilder builder = context.getReplyBuilder();
            final String first = BLOCK_PREFIX + LIST_TITLE + blocks.get( 0 ) + BLOCK_SUFFIX;
            request.doAction( () -> {
                builder.withContent( first ).build();
                return true;
            } );
            for ( int i = 1; i < blocks.size(); i++ ) {

                final String next = BLOCK_PREFIX + blocks.get( i ) + BLOCK_SUFFIX;
                request.andThen( () -> {
                    builder.withContent( next ).build();
                    return true;
                } );
                builder.withContent( next ).build();

            }
            request.execute();
        } else { // A command was specified.
            Iterator<String> args = context.getArgs().iterator();
            ICommand command = registry.parseCommand( args.next(), false );
            if ( command == null ) {
                return false; // No command with that signature.
            }

            ICommand mainCommand = command;
            ICommand parent = null;
            while ( args.hasNext() ) { // Identify subcommands.

                parent = command;
                command = command.getSubCommand( args.next() );
                if ( command == null ) {
                    return false; // No subcommand with the argument alias.
                }

            }
            context.getReplyBuilder().withEmbed( formatCommandLong( command, parent, mainCommand ) ).build();
        }

        return true;

    }

    @SubCommand(
            name = REGISTRY_DETAILS_SUBCOMMAND_NAME,
            aliases = { "registry" },
            description = "Displays information about a particular registry.\nThe registry "
                    + "type and name (for both parent registries and the target registry "
                    + "itself) should be just as shown in the registry list. All parent "
                    + "registries must be included in order. If there is a space in a "
                    + "registry name, put the whole qualified name (type:name) between " + "double-quotes.",
            usage = "{}help registry [here] [parent registries...] " + "<registry type>:<registry name>",
            essential = true,
            replyPrivately = true,
            canModifySubCommands = false,
            subCommands = { PUBLIC_HELP_SUBCOMMAND_NAME } )
    public boolean moduleDetailsCommand( CommandContext context ) {

        if ( context.getArgs().isEmpty() ) {
            return false; // No args.
        }

        update( context );

        /* Get target registry */
        CommandRegistry target = registry;
        Iterator<String> args = context.getArgs().iterator();
        if ( !args.next().equals( registry.getName() ) ) {
            return false; // First arg is not root registry.
        }
        while ( args.hasNext() ) {

            target = target.getSubRegistry( args.next() );
            if ( target == null ) {
                return false; // Arg specified a non-existing subregistry.
            }

        }

        /* Send details */
        MessageBuilder builder = context.getReplyBuilder();
        builder.withContent( BLOCK_PREFIX );
        builder.appendContent( REGISTRY_TITLE );
        builder.appendContent( formatRegistry( target ) );
        builder.appendContent( BLOCK_SUFFIX );
        builder.build(); // Send message.

        return true;

    }

    @SubCommand(
            name = REGISTRY_LIST_SUBCOMMAND_NAME,
            aliases = { "registries" },
            description = "Displays all the registered commands, categorized by "
                    + "the subregistries that they are registered in.",
            usage = "{}help registries [here]",
            essential = true,
            replyPrivately = true,
            canModifySubCommands = false,
            subCommands = { PUBLIC_HELP_SUBCOMMAND_NAME } )
    public void moduleListCommand( CommandContext context ) {

        update( context );

        Stack<CommandRegistry> registries = new Stack<>();
        registries.push( registry );
        final MessageBuilder builder = context.getReplyBuilder();

        String lastBlock = "";
        while ( !registries.isEmpty() ) { // For each registry.

            CommandRegistry registry = registries.pop();
            RequestBuilder request = new RequestBuilder( this.registry.getClient() ).shouldBufferRequests( true );

            /* Get registry path */
            String path = registry.getPath();

            /* Makes the title, and appends after the leftover block if there's space */
            String title = String.format( SUBREGISTRY_TITLE, path );
            if ( !registry.isEffectivelyEnabled() ) {
                title = DISABLED_TAG + title;
            }
            List<String> blocks = null;
            if ( !lastBlock.isEmpty() ) { // There is a leftover block.
                blocks = formatCommandList( registry.getRegisteredCommands(), // Try to fit it before
                        title.length() + lastBlock.length() + 1 ); // the title.
                if ( !blocks.get( 0 ).isEmpty() ) { // There's enough space for the leftover block.
                    title = lastBlock + "\n" + title; // Send it before the title.
                } else { // No space. Send the leftover block on its own.
                    blocks = null;
                    final String leftover = BLOCK_PREFIX + lastBlock + BLOCK_SUFFIX;
                    ;
                    new RequestBuilder( this.registry.getClient() ).shouldBufferRequests( true ).doAction( () -> {
                        builder.withContent( leftover ).build();
                        return true;
                    } ).execute();
                }
            }
            lastBlock = "";
            if ( blocks == null ) { // Blocks haven't been initialized yet, or tried
                blocks = formatCommandList( registry.getRegisteredCommands(), // with the
                        title.length() ); // leftover and there was no space.
            }

            /* Makes and sends the command list for the current registry */
            blocks.set( 0, title + blocks.get( 0 ) ); // Add title to the first block.
            request.doAction( () -> {
                return true;
            } );
            for ( int i = 0; i < blocks.size() - 1; i++ ) { // Message all blocks except the last.

                final String next = BLOCK_PREFIX + blocks.get( i ) + BLOCK_SUFFIX;
                request.andThen( () -> {
                    builder.withContent( next ).build();
                    return true;
                } );
                builder.withContent( next ).build();

            }
            request.execute();
            lastBlock = blocks.get( blocks.size() - 1 );

            for ( CommandRegistry subRegistry : registry.getSubRegistries().descendingSet() ) {
                // Add subregistries to the stack in reverse order.
                registries.push( subRegistry );

            }

        }
        if ( !lastBlock.isEmpty() ) { // There was a block leftover.
            final String leftover = BLOCK_PREFIX + lastBlock + BLOCK_SUFFIX;
            ;
            new RequestBuilder( this.registry.getClient() ).shouldBufferRequests( true ).doAction( () -> { // Print
                                                                                                           // leftover
                                                                                                           // block.
                builder.withContent( leftover ).build();
                return true;
            } ).execute();
        }

    }

    @SubCommand(
            name = PUBLIC_HELP_SUBCOMMAND_NAME,
            aliases = { "here" },
            description = "Modifies a help command to output the information to the current "
                    + "channel instead of a private channel.\nThe information is the same "
                    + "as calling without the \"here\" modifier.",
            usage = "{}help [subcommand] here [arguments]",
            canModifySubCommands = false,
            executeParent = true )
    public void publicHelpCommand( CommandContext context ) {

        // Do nothing.

    }

}
