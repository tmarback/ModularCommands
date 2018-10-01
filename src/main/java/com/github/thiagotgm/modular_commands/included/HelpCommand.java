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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
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

    /**
     * Name of the default help command.
     */
    public static final String MAIN_COMMAND_NAME = "Command Help";
    private static final String REGISTRY_LIST_SUBCOMMAND_NAME = "Registry List";
    private static final String REGISTRY_DETAILS_SUBCOMMAND_NAME = "Registry Details";
    private static final String PUBLIC_HELP_SUBCOMMAND_NAME = "Public Default Help Command";

    private static final String LIST_TITLE = "COMMAND LIST";
    private static final String LIST_BLOCK_TITLE = LIST_TITLE + " (%d/%d)";
    private static final String REGISTRY_PREFIX = "REGISTRY %s - ";
    private static final String REGISTRY_LIST_TITLE = REGISTRY_PREFIX + LIST_TITLE;
    private static final String REGISTRY_LIST_BLOCK_TITLE = REGISTRY_PREFIX + LIST_BLOCK_TITLE;
    private static final String EMPTY_REGISTRY = "<no commands>";

    private static final Pattern LINE_SPLITTER_PATTERN = Pattern.compile( "\\s*\n\\s*" );

    private static final String TABBED_LINE = '\n' + StringUtils.repeat( "\u200B\t", 5 );

    private static final Color EMBED_COLOR = Color.WHITE;

    private static final Map<Pattern, Function<Placeholder, String>> USAGE_PLACEHOLDERS;

    /**
     * A possible placeholder in the usage string.
     * 
     * @version 1.0
     * @author ThiagoTGM
     * @since 2018-09-26
     */
    private static class Placeholder {

        public final MatchResult match;
        public final List<ICommand> commandChain;
        public final CommandRegistry registry;

        /**
         * Creates a new instance.
         *
         * @param match
         *            The match within the placeholder.
         * @param commandChain
         *            The command chain being inspected.
         * @param registry
         *            The registry where the main command is registered.
         */
        public Placeholder( MatchResult match, List<ICommand> commandChain, CommandRegistry registry ) {

            this.match = match;
            this.commandChain = Collections.unmodifiableList( commandChain );
            this.registry = registry;

        }

    }

    /**
     * Retrieves the non-overridden aliases of the given command.
     *
     * @param command
     *            The command to inspect.
     * @param parent
     *            The parent of the command. May be <tt>null</tt> if the command is
     *            a main command.
     * @param registry
     *            The registry that the command is registered in, or a parent of
     *            said registry. Used if the command is a main command, to check if
     *            certain aliases are overridden. May be <tt>null</tt> if the
     *            command is a sub command.
     * @return The non-overridden aliases.
     */
    private static List<String> getAliases( ICommand command, ICommand parent, CommandRegistry registry ) {

        List<String> aliases = new ArrayList<>( command.getAliases().size() );
        if ( command.isSubCommand() ) {
            for ( String alias : command.getAliases() ) {

                if ( parent.getSubCommand( alias ) == command ) {
                    aliases.add( alias );
                }

            }
        } else {
            String prefix = command.getEffectivePrefix();
            for ( String alias : command.getAliases() ) { // Check if each alias is overridden.

                if ( registry.parseCommand( prefix + alias, false ) == command ) {
                    aliases.add( alias ); // Alias was not overridden.
                }

            }
        }
        return aliases;

    }

    /**
     * Formats the aliases of the given command for displaying.
     * <p>
     * Makes a comma-separated list of the aliases, between curly braces.
     *
     * @param command
     *            The command to inspect.
     * @param parent
     *            The parent of the command. May be <tt>null</tt> if the command is
     *            a main command.
     * @param registry
     *            The registry that the command is registered in, or a parent of
     *            said registry. Used if the command is a main command, to check if
     *            certain aliases are overridden. May be <tt>null</tt> if the
     *            command is a sub command.
     * @return The formatted alias list.
     */
    private static String formatAliases( ICommand command, ICommand parent, CommandRegistry registry ) {

        List<String> aliases = getAliases( command, parent, registry );
        String aliasString = String.join( ", ", aliases );
        return aliases.size() > 1 ? String.format( "{%s}", aliasString ) : aliasString;

    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile( "(.+?)\\s*([+-])\\s*(.+)" );

    /**
     * Evaluates the string expression given as an algebraic expression.
     * <p>
     * Supported values are integers, the operations + and -, and <tt>size</tt>,
     * which is replaced by the given size.
     *
     * @param expression
     *            The expression to evaluate.
     * @param size
     *            The value to replace occurrences of <tt>size</tt> with.
     * @return The value of the expression.
     * @throws NumberFormatException
     *             if the given expression is invalid.
     */
    private static int evaluateExpression( String expression, int size ) throws NumberFormatException {

        Matcher m = OPERATION_PATTERN.matcher( expression.trim() );
        if ( m.matches() ) { // Is an operation.
            int op1 = evaluateExpression( m.group( 1 ), size ); // Evaluate each operand.
            int op2 = evaluateExpression( m.group( 3 ), size );
            switch ( m.group( 2 ) ) {

                case "+":
                    return op1 + op2;

                case "-":
                    return op1 - op2;

                default:
                    return -1;

            }
        } else { // Is a value.
            if ( expression.equals( "size" ) ) {
                return size; // Replace with size.
            } else {
                return Integer.parseInt( expression ); // Parse as an integer.
            }
        }

    }

    /**
     * Parses the index to use.
     *
     * @param idxStr
     *            The string match that should be evaluated for the index.
     * @param size
     *            The size to replace occurrences of <tt>size</tt> with.
     * @return The index, or -1 if the matched string is not a valid expression or
     *         results in an index out of range (smaller than 0 or greater than
     *         size).
     */
    private static int parseIndex( MatchResult idxStr, int size ) {

        if ( idxStr.group( 1 ) == null ) {
            return size - 1;
        }

        int idx;
        try {
            idx = evaluateExpression( idxStr.group( 1 ), size );
        } catch ( NumberFormatException e ) {
            return -1; // Invalid expression.
        }
        return idx < size ? idx : -1;

    }

    private static final String INDEX = "(?:\\[(.+)\\])?";

    static { // Initializes placeholder expressions.

        Map<String, Function<Placeholder, String>> placeholders = new HashMap<>();

        placeholders.put( "prefix", p -> p.commandChain.get( 0 ).getEffectivePrefix() );
        placeholders.put( "aliases" + INDEX, p -> {

            int index = parseIndex( p.match, p.commandChain.size() );
            if ( index == -1 ) {
                return p.match.group();
            }

            ICommand command = p.commandChain.get( index );
            ICommand parent = index > 0 ? p.commandChain.get( index - 1 ) : null;
            return formatAliases( command, parent, p.registry );

        } );
        placeholders.put( "signature" + INDEX, p -> {

            int index = parseIndex( p.match, p.commandChain.size() );
            if ( index == -1 ) {
                return p.match.group();
            }

            List<ICommand> commands = p.commandChain.subList( 0, index + 1 );
            List<String> aliases = new ArrayList<>( commands.size() );
            for ( int i = 0; i < commands.size(); i++ ) {

                aliases.add( formatAliases( commands.get( i ), i > 0 ? commands.get( i - 1 ) : null, p.registry ) );

            }
            return commands.get( 0 ).getEffectivePrefix() + String.join( " ", aliases );

        } );

        // Compile patterns and store.
        USAGE_PLACEHOLDERS = Collections.unmodifiableMap( placeholders.entrySet().stream()
                .collect( Collectors.toMap( e -> Pattern.compile( e.getKey() ), e -> e.getValue() ) ) );

    }

    /**
     * Converts the given string to a quote format in markdown.
     *
     * @param str
     *            The string.
     * @return The quoted string.
     */
    private static String quote( String str ) {

        return String.format( "`%s`", str );

    }

    /**
     * Applies a strikethrough effect to the given string in markdown.
     *
     * @param str
     *            The string.
     * @return The string with a strikethrough effect.
     */
    private static String strikethrough( String str ) {

        return String.format( "~~%s~~", str );

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
     *            The command to format. Is always a main command.
     * @return A string that describes the command in a single line, or
     *         <tt>null</tt> if the command has no callable signatures.
     * @param registry
     *            The registry that the command is registered in, or a parent of
     *            said registry. Used to check if certain aliases are overridden.
     */
    private String formatCommandShort( ICommand command, CommandRegistry registry ) {

        List<String> aliases = getAliases( command, null, registry );
        if ( aliases.isEmpty() ) {
            return null; // Command has no callable signatures.
        }
        // Get signature list.
        String prefix = command.getEffectivePrefix();
        String signatures = aliases.stream().map( a -> quote( prefix + a ) ).collect( Collectors.toList() ).toString();
        StringBuilder builder = new StringBuilder();
        signatures = signatures.substring( 1, signatures.length() - 1 ); // Remove brackets.
        if ( !command.isEffectivelyEnabled() ) { // Strikethrough to show disabled.
            signatures = strikethrough( signatures );
        }
        builder.append( signatures );
        builder.append( " - " );
        builder.append( parseDescription( command )[0] );
        return builder.toString();

    }

    private static final Pattern USAGE_PLACEHOLDER_PATTERN = Pattern
            .compile( "((?:\\\\\\\\)*)(\\\\)??\\{\\s*(.+?)\\s*\\}" );

    /**
     * Parses the usage string.
     *
     * @param commandChain
     *            The command chain being inspected.
     * @param registry
     *            The registry that the main command is registered in, or a parent
     *            of said registry. Used to check if certain aliases are overridden.
     * @return The The usage string for the subcommand that the chain represented,
     *         with all <b>valid</b> placeholders replaced by the appropriate
     *         values. May be empty if the command has an empty usage.
     */
    private String parseUsage( List<ICommand> commandChain, CommandRegistry registry ) {

        String usage = commandChain.get( commandChain.size() - 1 ).getUsage();
        Matcher m = USAGE_PLACEHOLDER_PATTERN.matcher( usage );
        int curStart = 0;
        StringBuilder builder = new StringBuilder();

        while ( m.find() ) {

            builder.append( usage.substring( curStart, m.start() ) ); // Append content between placeholders.
            String backslashes = m.group( 1 );
            if ( backslashes != null ) { // Insert escaped backslashes.
                builder.append( backslashes.substring( 0, backslashes.length() / 2 ) );
            }
            boolean matched = false;
            if ( m.group( 2 ) == null ) { // Ignore if opening bracket escaped.
                for ( Map.Entry<Pattern, Function<Placeholder, String>> e : USAGE_PLACEHOLDERS.entrySet() ) {

                    Matcher match = e.getKey().matcher( m.group( 3 ) );
                    if ( match.matches() ) { // Matched placeholder.
                        matched = true;
                        builder.append( // Insert substituted value.
                                e.getValue()
                                        .apply( new Placeholder( match.toMatchResult(), commandChain, registry ) ) );
                        break;
                    }

                }
            }
            if ( !matched ) { // Did not match a placeholder or was escaped.
                builder.append( m.group() ); // Just insert original text.
            }
            curStart = m.end(); // Move start to after match.

        }
        builder.append( usage.substring( curStart, usage.length() ) ); // Append content after last placeholder.

        return builder.toString();

    }

    /**
     * Formats a command into all the information a user might need.
     *
     * @param commandChain
     *            The command to format, along with its parents.
     * @param registry
     *            The registry that the command is registered in, or a parent of
     *            said registry. Used if the command is a main command, to check if
     *            certain aliases are overridden. May be <tt>null</tt> if the
     *            command is a sub command.
     * @return The string that fully describes the command.
     */
    private EmbedObject formatCommandLong( List<ICommand> commandChain, CommandRegistry registry ) {

        ICommand command = commandChain.get( commandChain.size() - 1 );
        ICommand parent = commandChain.size() > 1 ? commandChain.get( commandChain.size() - 2 ) : null;

        EmbedBuilder builder = new EmbedBuilder().withColor( EMBED_COLOR );

        /* Add command name */
        builder.withTitle( command.getName() );

        /* Add description */
        String description = String.join( TABBED_LINE, parseDescription( command ) );
        if ( !description.isEmpty() ) {
            builder.withDescription( description );
        }

        /* Add prefix if this is a main command */
        if ( !command.isSubCommand() ) {
            String title = command.getPrefix() == null ? "Inherited Prefix" : "Prefix";
            builder.appendField( title, quote( command.getEffectivePrefix() ), false );
        }

        /* Add aliases */
        List<String> aliases = getAliases( command, parent, registry );
        if ( aliases.isEmpty() ) {
            return null; // Command has no callable aliases.
        }
        builder.appendField( "Aliases", quote( aliases.toString() ), false );

        /* Add usage */
        String usage = parseUsage( commandChain, registry );
        if ( !usage.isEmpty() ) {
            builder.appendField( "Usage", quote( usage ), false );
        }

        /* Add subcommands */
        SortedSet<List<String>> subCommandAliases = new TreeSet<>( ( s1, s2 ) -> {
            return s1.get( 0 ).compareTo( s2.get( 0 ) ); // Compare head elements.
        } );
        for ( ICommand subCommand : command.getSubCommands() ) {

            aliases = getAliases( subCommand, command, registry );
            if ( !aliases.isEmpty() ) { // This subcommand had callable aliases.
                subCommandAliases.add( aliases );
            }

        }
        if ( !subCommandAliases.isEmpty() ) {
            String subcommands = subCommandAliases.toString();
            subcommands = quote( subcommands.substring( 1, subcommands.length() - 1 ) );
            builder.appendField( "Subcommands", subcommands, false );
        }

        /* Add permissions */
        EnumSet<Permissions> channelPermissions = command.getRequiredPermissions();
        if ( !channelPermissions.isEmpty() ) {
            builder.appendField( "Required permissions in channel", quote( channelPermissions.toString() ), false );
        }
        EnumSet<Permissions> guildPermissions = command.getRequiredGuildPermissions();
        if ( !guildPermissions.isEmpty() ) {
            builder.appendField( "Required permissions in server", quote( guildPermissions.toString() ), false );
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
    private EmbedObject formatRegistry( CommandRegistry registry ) {

        /* Add registry name */
        EmbedBuilder builder = new EmbedBuilder().withColor( EMBED_COLOR ).withTitle( quote( registry.getPath() ) );

        /* Add prefix */
        String title = registry.getPrefix() == null ? "Inherited Prefix" : "Prefix";
        builder.appendField( title, quote( registry.getEffectivePrefix() ), false );

        /* Add modifiers */
        List<String> modifiers = new LinkedList<>();
        if ( registry.isEssential() ) {
            modifiers.add( "- Essential" );
        }
        if ( !registry.isEnabled() ) {
            modifiers.add( "- Disabled" );
        } else if ( !registry.isEffectivelyEnabled() ) {
            modifiers.add( "- Parent registry disabled" );
        }
        if ( !modifiers.isEmpty() ) {
            builder.appendField( "Modifiers", String.join( "\n", modifiers ), false );
        }

        return builder.build();

    }

    /**
     * Formats a collection of commands into a list, where each line describes a
     * single command.
     * <p>
     * All blocks have at most the given size.
     *
     * @param commands
     *            The commands to make a list of.
     * @param sizeLimit
     *            The maximum size of each block.
     * @param registry
     *            The registry that the commands are registered in, or a parent of
     *            said registry. Used to check if certain aliases of a command are
     *            overridden.
     * @return The formatted command list split in blocks.
     * @throws IllegalArgumentException
     *             if there is a command whose formatted form is larger than the
     *             given size limit.
     */
    private List<String> formatCommandList( Collection<ICommand> commands, int sizeLimit, CommandRegistry registry )
            throws IllegalArgumentException {

        /* Gets the formatted commands and sorts them */
        SortedSet<String> commandStrings = commands.stream().map( c -> formatCommandShort( c, registry ) )
                .filter( f -> f != null ).collect( Collectors.toCollection( TreeSet::new ) );

        /* Adds all formatted commands into different lines */
        List<String> blocks = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for ( String commandString : commandStrings ) {

            if ( commandString.length() > sizeLimit ) {
                throw new IllegalArgumentException( "Command description is too large." );
            }
            if ( builder.length() + commandString.length() + ( builder.length() == 0 ? 0 : 1 ) >= sizeLimit ) {
                // Reached max block length.
                blocks.add( builder.toString() ); // Record current block.
                builder = new StringBuilder();
            }
            if ( builder.length() > 0 ) { // Already has a line.
                builder.append( '\n' ); // Make a new line.
            }
            builder.append( commandString ); // Append line.

        }
        if ( builder.length() > 0 ) {
            blocks.add( builder.toString() );
        }

        if ( blocks.isEmpty() ) {
            blocks.add( EMPTY_REGISTRY ); // Ensure at least one block.
        }

        return blocks;

    }

    /**
     * Shows the help page of a specified command, or the list of commands if no
     * command is specified.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
    @MainCommand(
            name = MAIN_COMMAND_NAME,
            aliases = { "help" },
            description = "Displays information about registered commands.\nIf a command "
                    + "signature is specified, displays information about that command.\n"
                    + "Else, displays a list of all registered commands.",
            usage = "{prefix}{aliases} [here] [command signature]",
            essential = true,
            replyPrivately = true,
            overrideable = false,
            priority = Integer.MAX_VALUE,
            canModifySubCommands = false,
            subCommands = { REGISTRY_LIST_SUBCOMMAND_NAME, REGISTRY_DETAILS_SUBCOMMAND_NAME,
                    PUBLIC_HELP_SUBCOMMAND_NAME },
            successHandler = ICommand.STANDARD_SUCCESS_HANDLER,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean helpCommand( CommandContext context ) {

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );

        if ( context.getArgs().isEmpty() ) { // No command specified.
            RequestBuilder request = new RequestBuilder( context.getEvent().getClient() ).shouldBufferRequests( true )
                    .setAsync( true );
            AtomicInteger curBlock = new AtomicInteger( 1 );
            EmbedBuilder blockBuilder = new EmbedBuilder().withColor( EMBED_COLOR ).withFooterText(
                    "Pass in another command as an argument to this command to see info on that particular "
                            + "command! For example, try using this command as the argument to itself~" );
            List<String> strBlocks = formatCommandList( registry.getCommands(), EmbedBuilder.DESCRIPTION_CONTENT_LIMIT,
                    registry );
            List<EmbedObject> blocks = strBlocks
                    .stream().map(
                            b -> blockBuilder
                                    .withTitle( strBlocks.size() > 1 ? String.format( LIST_BLOCK_TITLE,
                                            curBlock.getAndIncrement(), strBlocks.size() ) : LIST_TITLE )
                                    .withDesc( b ).build() )
                    .collect( Collectors.toList() );
            MessageBuilder builder = context.getReplyBuilder();
            request.doAction( () -> true );
            for ( EmbedObject block : blocks ) {

                request.andThen( () -> {

                    builder.withEmbed( block ).build();
                    return true;

                } );

            }
            request.execute();
        } else { // A command was specified.
            List<ICommand> commandChain = CommandUtils.parseCommand( context.getArgs(), registry );
            if ( commandChain == null ) {
                context.setHelper( "There is no command that matches the given signature!" );
                return false; // No command with that signature.
            }
            context.getReplyBuilder().withEmbed( formatCommandLong( commandChain, registry ) ).build();
        }

        return true;

    }

    /**
     * Shows the information of the specified registry.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
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
            subCommands = { PUBLIC_HELP_SUBCOMMAND_NAME },
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean registryDetailsCommand( CommandContext context ) {

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );

        CommandRegistry target = CommandUtils.parseRegistry( context.getArgs(), registry );
        if ( target == null ) {
            context.setHelper( "There is no registry that matches the given path!" );
            return false; // Specified non-existing registry.
        }
        context.getReplyBuilder().withEmbed( formatRegistry( target ) ).build();

        return true;

    }

    /**
     * Shows the list of registries and the commands registered in each registry.
     *
     * @param context
     *            The context of execution.
     */
    @SubCommand(
            name = REGISTRY_LIST_SUBCOMMAND_NAME,
            aliases = { "registries" },
            description = "Displays all the registered commands, categorized by "
                    + "the subregistries that they are registered in.",
            usage = "{}help registries [here]",
            essential = true,
            replyPrivately = true,
            canModifySubCommands = false,
            subCommands = { PUBLIC_HELP_SUBCOMMAND_NAME },
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public void registryListCommand( CommandContext context ) {

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );

        Stack<CommandRegistry> registries = new Stack<>();
        registries.push( registry );

        EmbedBuilder blockBuilder = new EmbedBuilder().withColor( EMBED_COLOR );
        List<EmbedObject> blocks = new LinkedList<>();
        while ( !registries.isEmpty() ) { // For each registry.

            CommandRegistry target = registries.pop();

            /* Get registry path */
            String path = quote( target.getPath() );
            if ( !target.isEffectivelyEnabled() ) {
                path = strikethrough( path );
            }

            List<String> strBlocks = formatCommandList( target.getRegisteredCommands(),
                    EmbedBuilder.FIELD_CONTENT_LIMIT, registry );
            int curBlock = 1;
            for ( String strBlock : strBlocks ) {

                String title = strBlocks.size() > 1
                        ? String.format( REGISTRY_LIST_BLOCK_TITLE, path, curBlock++, strBlocks.size() )
                        : String.format( REGISTRY_LIST_TITLE, path );

                if ( ( blockBuilder.getFieldCount() >= EmbedBuilder.FIELD_COUNT_LIMIT ) // Won't fit in current block.
                        || ( blockBuilder.getTotalVisibleCharacters() + title.length()
                                + strBlock.length() > EmbedBuilder.MAX_CHAR_LIMIT ) ) {
                    blocks.add( blockBuilder.build() ); // Flush current block.
                    blockBuilder.clearFields(); // Start new block.
                }

                blockBuilder.appendField( title, strBlock, false );

            }

            for ( CommandRegistry subRegistry : target.getSubRegistries().descendingSet() ) {
                // Add subregistries to the stack in reverse order.
                registries.push( subRegistry );

            }

        }
        if ( blockBuilder.getFieldCount() > 0 ) { // Has unfinished block.
            blocks.add( blockBuilder.build() ); // Flush last block.
        }

        RequestBuilder request = new RequestBuilder( context.getEvent().getClient() ).shouldBufferRequests( true )
                .setAsync( true );
        MessageBuilder builder = context.getReplyBuilder();
        request.doAction( () -> true );
        for ( EmbedObject block : blocks ) { // Send blocks.

            request.andThen( () -> {

                builder.withEmbed( block ).build();
                return true;

            } );

        }
        request.execute();

    }

    /**
     * Sends the reply in the calling channel instead of a private message.
     *
     * @param context
     *            The context of execution.
     */
    @SubCommand(
            name = PUBLIC_HELP_SUBCOMMAND_NAME,
            aliases = { "here" },
            description = "Modifies a help command to output the information to the current "
                    + "channel instead of a private channel.\nThe information is the same "
                    + "as calling without the \"here\" modifier.",
            usage = "{}help [subcommand] here [arguments]",
            canModifySubCommands = false,
            executeParent = true,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public void publicHelpCommand( CommandContext context ) {

        // Do nothing.

    }

}
