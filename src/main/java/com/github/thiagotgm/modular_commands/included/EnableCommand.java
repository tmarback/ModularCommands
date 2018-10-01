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

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;

/**
 * Command for enabling a command or registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-26
 */
public class EnableCommand {

    /**
     * Name of the default enable command.
     */
    public static final String COMMAND_NAME = "Enable Command";
    private static final String SUBCOMMAND_NAME = "Enable Registry";

    /**
     * Enables the specified command.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
    @MainCommand(
            name = COMMAND_NAME,
            aliases = { "enable" },
            description = "Enables a command.",
            usage = "{}enable <command signature>",
            requiresOwner = true,
            essential = true,
            overrideable = false,
            priority = Integer.MAX_VALUE,
            canModifySubCommands = false,
            subCommands = SUBCOMMAND_NAME,
            successHandler = ICommand.STANDARD_SUCCESS_HANDLER,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean enableCommand( CommandContext context ) {

        if ( context.getArgs().isEmpty() ) {
            context.setHelper( "A command must be specified." );
            return false; // No arguments given.
        }

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );
        return CommandUtils.runOnCommand( context, registry, cl -> {

            ICommand target = cl.get( 0 );
            if ( target.isEnabled() ) {
                return String.format( "Command `%s` is already enabled!", target.getName() );
            }
            target.enable();
            return null;

        }, cl -> String.format( "Enabled command `%s`!", cl.get( cl.size() - 1 ).getName() ) );

    }

    /**
     * Enables the specified registry.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
    @SubCommand(
            name = SUBCOMMAND_NAME,
            aliases = { "registry" },
            description = "Enables a registry.\n"
                    + "The registry type and name (for both parent registries and the target registry "
                    + "itself) should be just as shown in the registry list. All parent "
                    + "registries must be included in order. If there is a space in a "
                    + "registry name, put the whole qualified name (type:name) between " + "double-quotes.",
            usage = "{}enable registry [parent registries...] <registry type>:<registry name>",
            requiresOwner = true,
            essential = true,
            canModifySubCommands = false,
            successHandler = ICommand.STANDARD_SUCCESS_HANDLER,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean enableRegistry( CommandContext context ) {

        if ( context.getArgs().isEmpty() ) {
            context.setHelper( "A registry must be specified." );
            return false; // No arguments given.
        }

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );
        return CommandUtils.runOnRegistry( context, registry, r -> {

            if ( r.isEnabled() ) {
                return String.format( "Registry `%s` is already enabled!", r.getPath() );
            }
            r.enable();
            return null;

        }, r -> String.format( "Enabled registry `%s`!", r.getPath() ) );

    }

}
