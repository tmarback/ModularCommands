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

package com.github.thiagotgm.modular_commands.command;

import java.util.Collection;
import java.util.EnumSet;
import com.github.thiagotgm.modular_commands.api.Executor;
import com.github.thiagotgm.modular_commands.api.FailureHandler;
import com.github.thiagotgm.modular_commands.api.ICommand;

import sx.blah.discord.handle.obj.Permissions;

/**
 * Builder for creating new commands with set properties.
 *
 * @version 1.0.0
 * @author ThiagoTGM
 * @since 2017-07-18
 */
public class CommandBuilder {
    
    private boolean essential;
    private String prefix;
    private final String name;
    private Collection<String> aliases;
    private boolean subCommand;
    private String description;
    private String usage;
    private Executor commandOperation;
    private int onSuccessDelay;
    private Executor onSuccessOperation;
    private FailureHandler onFailureOperation;
    private boolean replyPrivately;
    private boolean ignorePublic;
    private boolean ignorePrivate;
    private boolean ignoreBots;
    private boolean deleteCommand;
    private boolean requiresOwner;
    private boolean NSFW;
    private boolean overrideable;
    private boolean executeParent;
    private boolean requiresParentPermissions;
    private EnumSet<Permissions> requiredPermissions;
    private EnumSet<Permissions> requiredGuildPermissions;
    private Collection<ICommand> subCommands;
    private boolean canModifySubCommands;
    private int priority;

    /**
     * Constructs a new builder with default values for all properties (that have default values)
     * and the given name.
     * 
     * @param name The name of the command.
     */
    public CommandBuilder( String name ) {
        
        this.name = name;
        
    }
    
    /**
     * Builds a command with the current property values.
     *
     * @return The built command.
     * @throws IllegalStateException If a value was not specified for a property that does not
     *                               have a default value.
     */
    public Command build() throws IllegalStateException {
        
        return new Command( essential,
                            prefix,
                            name,
                            aliases,
                            subCommand,
                            description,
                            usage,
                            commandOperation,
                            onSuccessDelay,
                            onSuccessOperation,
                            onFailureOperation,
                            replyPrivately,
                            ignorePublic,
                            ignorePrivate,
                            ignoreBots,
                            deleteCommand,
                            requiresOwner,
                            NSFW,
                            overrideable,
                            executeParent,
                            requiresParentPermissions,
                            requiredPermissions,
                            requiredGuildPermissions,
                            subCommands,
                            canModifySubCommands,
                            priority );
        
    }

}
