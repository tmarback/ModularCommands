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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of command-related statistics.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-09-13
 */
public abstract class CommandStats {

    private static final AtomicLong EXECUTED_COUNT = new AtomicLong( 0 );
    
    /**
     * Increments the counter of command executions.
     */
    protected static void incrementCount() {
        
        EXECUTED_COUNT.incrementAndGet();
        
    }
    
    /**
     * Retrieves the current count of command executions.<br>
     * This includes only <i>actual</i> command executions, so instances where the command
     * ignored the call, was disabled, or the user did not have the required permissions are
     * not included.
     *
     * @return The amount of commands that were executed since the program started.
     */
    public static long getCount() {
        
        return EXECUTED_COUNT.get();
        
    }

}
