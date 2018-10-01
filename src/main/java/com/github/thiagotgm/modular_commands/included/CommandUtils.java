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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;

/**
 * Functions for commands that take other commands as arguments. Used by the
 * included commands.
 * 
 * @version 1.0
 * @author ThiagoTGM
 * @since 2018-09-30
 */
class CommandUtils {

    private static final String PATH_SEPARATOR = Character.toString( CommandRegistry.PATH_SEPARATOR );

    /**
     * Parses a registry from a list of arguments.
     * <p>
     * If only one argument is present, parses it as a path string
     * ({@value CommandRegistry#PATH_SEPARATOR} is the separator, a leading
     * separator is ignored, and an empty string (or only the separator) is the root
     * itself.
     * <p>
     * If more than one argument, each argument is considered one element in the
     * path (empty argument list corresponds to the root itself).
     *
     * @param args
     *            The args to parse.
     * @param root
     *            The root registry.
     * @return The registry specified by the given arguments in the given root, or
     *         <tt>null</tt> if there is no registry in the given root that
     *         corresponds to the given arguments.
     */
    public static CommandRegistry parseRegistry( List<String> args, CommandRegistry root ) {

        List<String> path;
        if ( args.size() == 1 ) {
            String pathStr = args.get( 0 ).trim();
            if ( pathStr.startsWith( PATH_SEPARATOR ) ) { // Remove leading separator.
                pathStr = pathStr.substring( PATH_SEPARATOR.length() );
            } // Split path.
            path = pathStr.isEmpty() ? new ArrayList<>() : Arrays.asList( pathStr.split( PATH_SEPARATOR ) );
        } else {
            path = args;
        }

        CommandRegistry target = root;
        for ( String registry : path ) {

            if ( !target.hasSubRegistry( registry ) ) {
                return null; // Arg specified a non-existing subregistry.
            }
            target = target.getSubRegistry( registry );

        }
        return target;

    }

    /**
     * Parses a command from a list of arguments.
     * <p>
     * The arguments are interpreted as being the signature of the target command.
     *
     * @param args
     *            The args to parse.
     * @param registry
     *            The registry to search in.
     * @return The command specified by the given arguments in the given registry,
     *         or <tt>null</tt> if there is no command in the given registry that
     *         corresponds to the given arguments.
     */
    public static List<ICommand> parseCommand( List<String> args, CommandRegistry registry ) {

        Iterator<String> argIter = args.iterator();
        ICommand command = registry.parseCommand( argIter.next(), false );
        if ( command == null ) {
            return null; // No command with that signature.
        }

        List<ICommand> commandChain = new ArrayList<>( args.size() );
        commandChain.add( command );
        while ( argIter.hasNext() ) { // Identify subcommands.

            command = command.getSubCommand( argIter.next() );
            if ( command == null ) {
                return null; // No subcommand with the argument alias.
            }
            commandChain.add( command );

        }
        return commandChain;

    }

    /**
     * Runs the given operation on an object obtained from the arguments in the
     * given context. If the operation could not be successfully completed, an error
     * message is placed as the helper of the given context. If it is completed
     * successfully, a success message is placed as the helper of the given context.
     *
     * @param <T>
     *            The type of object to run on.
     * @param context
     *            The context of execution.
     * @param parser
     *            The parser to use to extract an instance from the context's
     *            arguments.
     * @param operation
     *            The operation to run on the parsed instance. It should return
     *            <tt>null</tt> if the operation was completed successfully, or the
     *            appropriate error message otherwise.
     * @param objType
     *            The name of the type of object the operation runs on (such as
     *            "command" or "registry").
     * @param successMessage
     *            The function that provides the success message if the operation
     *            succeeds.
     * @return <tt>true</tt> if the operation was executed successfully,
     *         <tt>false</tt> otherwise.
     */
    public static <T> boolean runOn( CommandContext context, Function<List<String>, T> parser,
            Function<T, String> operation, String objType, Function<T, String> successMessage ) {

        T target = parser.apply( context.getArgs() );
        if ( target == null ) {
            context.setHelper( String.format( "Could not find specified %s.", objType ) );
            return false;
        }

        String errorMessage = operation.apply( target );
        context.setHelper( errorMessage == null ? successMessage.apply( target ) : errorMessage );
        return errorMessage == null;

    }

    /**
     * Runs the given operation on a registry obtained from the arguments in the
     * given context. If the operation could not be successfully completed, an error
     * message is placed as the helper of the given context. If it is completed
     * successfully, a success message is placed as the helper of the given context.
     *
     * @param context
     *            The context of execution.
     * @param root
     *            The root registry to use when parsing the arguments.
     * @param operation
     *            The operation to run on the parsed instance. It should return
     *            <tt>null</tt> if the operation was completed successfully, or the
     *            appropriate error message otherwise.
     * @param successMessage
     *            The function that provides the success message if the operation
     *            succeeds.
     * @return <tt>true</tt> if the operation was executed successfully,
     *         <tt>false</tt> otherwise.
     */
    public static boolean runOnRegistry( CommandContext context, CommandRegistry root,
            Function<CommandRegistry, String> operation, Function<CommandRegistry, String> successMessage ) {

        return runOn( context, args -> parseRegistry( args, root ), operation, "registry", successMessage );

    }

    /**
     * Runs the given operation on a command obtained from the arguments in the
     * given context. If the operation could not be successfully completed, an error
     * message is placed as the helper of the given context. If it is completed
     * successfully, a success message is placed as the helper of the given context.
     *
     * @param context
     *            The context of execution.
     * @param registry
     *            The registry to parse the command from.
     * @param operation
     *            The operation to run on the parsed instance. It should return
     *            <tt>null</tt> if the operation was completed successfully, or the
     *            appropriate error message otherwise.
     * @param successMessage
     *            The function that provides the success message if the operation
     *            succeeds.
     * @return <tt>true</tt> if the operation was executed successfully,
     *         <tt>false</tt> otherwise.
     */
    public static boolean runOnCommand( CommandContext context, CommandRegistry registry,
            Function<List<ICommand>, String> operation, Function<List<ICommand>, String> successMessage ) {

        return runOn( context, args -> parseCommand( args, registry ), operation, "command", successMessage );

    }

}
