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

/**
 * Defines an object that can be registered into a command registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-13
 */
public interface Registerable {
    
    /**
     * Retrieves the registry that the calling instance is registered to.
     *
     * @return The registry that contains this instance.
     */
    abstract CommandRegistry getRegistry();
    
    /**
     * Sets the registry that the calling instance is registered to.
     *
     * @param registry The registry that this instance is now registered to.
     */
    abstract void setRegistry( CommandRegistry registry );

}
