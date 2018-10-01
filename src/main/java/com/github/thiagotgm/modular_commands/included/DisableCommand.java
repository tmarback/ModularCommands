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
 * Command for disabling a command or registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-26
 */
public class DisableCommand {

    /**
     * Name of the default disable command.
     */
    public static final String COMMAND_NAME = "Disable Command";
    private static final String SUBCOMMAND_NAME = "Disable Registry";

    /**
     * Disables the specified command.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
    @MainCommand(
            name = COMMAND_NAME,
            aliases = { "disable" },
            description = "Disables a command.\n"
                    + "A command that is marked as essential cannot be disabled.",
            usage = "{signature} <command signature>",
            requiresOwner = true,
            essential = true,
            overrideable = false,
            priority = Integer.MAX_VALUE,
            canModifySubCommands = false,
            subCommands = SUBCOMMAND_NAME,
            successHandler = ICommand.STANDARD_SUCCESS_HANDLER,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean disableCommand( CommandContext context ) {

        if ( context.getArgs().isEmpty() ) {
            context.setHelper( "A command must be specified." );
            return false; // No arguments given.
        }

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );
        return CommandUtils.runOnCommand( context, registry, cl -> {

            ICommand target = cl.get( 0 );
            if ( target.isEssential() ) {
                return String.format( "Command `%s` is essential and may not be disabled!", target.getName() );
            }
            if ( !target.isEnabled() ) {
                return String.format( "Command `%s` is already disabled!", target.getName() );
            }
            target.disable();
            return null;

        }, cl -> String.format( "Disabled command `%s`!", cl.get( cl.size() - 1 ).getName() ) );

    }

    /**
     * Disables the specified registry.
     *
     * @param context
     *            The context of execution.
     * @return <tt>true</tt> if executed successfully.
     */
    @SubCommand(
            name = SUBCOMMAND_NAME,
            aliases = { "registry" },
            description = "Disables a registry.\n" + "The registry path may be specified either by a `"
                    + CommandRegistry.PATH_SEPARATOR
                    + "`-separated list of names, like how it is shown in the registry list (the leading `"
                    + CommandRegistry.PATH_SEPARATOR + "` may be ommited), or by including the name of each "
                    + "registry in the path as a separate argument (the root registry would be an empty list, "
                    + "and is implied in any path specified in this manner).\n"
                    + "A registry that is marked as essential cannot be disabled.",
            usage = "{signature} <registry path>",
            requiresOwner = true,
            essential = true,
            canModifySubCommands = false,
            successHandler = ICommand.STANDARD_SUCCESS_HANDLER,
            failureHandler = ICommand.STANDARD_FAILURE_HANDLER )
    public boolean disableRegistry( CommandContext context ) {

        if ( context.getArgs().isEmpty() ) {
            context.setHelper( "A registry must be specified." );
            return false; // No arguments given.
        }

        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );
        return CommandUtils.runOnRegistry( context, registry, r -> {

            if ( r.isEssential() ) {
                return String.format( "Registry `%s` is essential and may not be disabled!", r.getPath() );
            }
            if ( !r.isEnabled() ) {
                return String.format( "Registry `%s` is already disabled!", r.getPath() );
            }
            r.disable();
            return null;

        }, r -> String.format( "Disabled registry `%s`!", r.getPath() ) );

    }

}
