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
 * Defines an object that has a prefix, either declared or inherited from a registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-13
 */
public interface Prefixed extends Registerable {
    
    /**
     * Retrieves the <i>declared</i> prefix of the calling instance.
     * <p>
     * By default, returns null.
     *
     * @return The prefix of the instance. If the instance has no set prefix, returns null.
     */
    default String getPrefix() { return null; }
    
    /**
     * Retrieves the <i>effective</i> prefix of the calling instance.<br>
     * If the calling instance has no declared prefix, inherits the prefix from the registry it is registered to.
     *
     * @return The effective prefix used for this instance.
     * @throws IllegalStateException if called when the instance is not registered to any registry.
     */
    default String getEffectivePrefix() throws IllegalStateException {
        
        if ( getRegistry() == null ) {
            throw new IllegalStateException( "Tried to obtain effective prefix before registering." );
        }
        return ( getPrefix() != null ) ? getPrefix() : getRegistry().getEffectivePrefix();
        
    }

}
