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
 * Interface that defines an object with functionality that can be enabled or
 * disabled during runtime.<br>
 * Can also be marked as essential, preventing it from being disabled.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public interface Disableable {
    
    /**
     * Retrieves whether the calling instance is currently enabled.
     *
     * @return true if currently enabled, false if disabled.
     */
    abstract boolean isEnabled();
    
    /**
     * Sets the calling instance to be enabled or disabled.
     *
     * @param enabled If true, enables the calling instance. If false, disables it.
     * @throws IllegalStateException if the argument is false but the calling instance
     *                               cannot be disabled due to being marked as essential.
     * @see #isEssential()
     */
    abstract void setEnabled( boolean enabled ) throws IllegalStateException;
    
    /**
     * Sets the calling instance as enabled.
     */
    default void enable() { setEnabled( true ); }
    
    /**
     * Sets the calling instance as disabled.
     *
     * @throws IllegalStateException if the calling instance cannot be disabled due to
     *                               being marked as essential.
     * @see #isEssential()
     */
    default void disable() throws IllegalStateException { setEnabled( false ); }
    
    /**
     * Retrieves whether the calling instance is essential, that is, if it cannot be
     * disabled.
     * <p>
     * By default, this returns false.
     *
     * @return true if the calling instance is essential and so cannot be disabled.<br>
     *         false if it is not essential and can be disabled normally (default).
     */
    default boolean isEssential() { return false; }

}
