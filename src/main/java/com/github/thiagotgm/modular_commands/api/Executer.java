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

import java.util.function.Consumer;

import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

/**
 * Specialization of the Consumer interface.<br>
 * Represents an operation that, given the context of a Discord command,
 * executes a task with the Discord API, with the possibility of passing in Discord-related
 * exceptions (missing permissions or a miscellaneous Discord error) to the caller.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 * @see Consumer
 */
@FunctionalInterface
public interface Executer extends Consumer<CommandContext> {
    
    /**
     * Performs the operation on the given context.
     *
     * @param context Context to perform the operation on.
     * @throws MissingPermissionsException If the operation could not be completed due to the
     *                                     executing client not having the required permissions.
     * @throws DiscordException If the operation could not be completed due to a miscellaneous error.
     */
    @Override
    void accept( CommandContext context ) throws MissingPermissionsException, DiscordException;
    
    /**
     * Returns a composed Executer that executes this operation followed by the given
     * operation. Exceptions thrown by either operation are passed in to the composed
     * operation.
     *
     * @param after Operation to execute after this.
     * @return The composed Executer.
     * @see Consumer#andThen(Consumer)
     */
    default Executer andThen( Executer after ) {
        
        return ( context ) -> {
            
            this.accept( context );
            after.accept( context );
            
        };
        
    }

}
