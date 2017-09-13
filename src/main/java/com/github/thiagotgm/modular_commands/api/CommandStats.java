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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Keeps track of command-related statistics. In order to ensure count consistency without
 * delaying command executions due to synchronization locks, stat increments are only
 * <b>requested</b> by other classes, and are applied one at a time by an independent
 * internal thread.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-09-13
 */
public abstract class CommandStats {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor( ( r ) -> {
        
        Thread thread = new Thread( r, "CommandStats Updater" );
        thread.setDaemon( true );
        return thread;
        
    });
    private static final Runnable INCREMENT_TASK = () -> { increment(); };
    
    private static volatile long executedCount = 0;
    
    /**
     * Increments the counter of command executions.
     */
    private static synchronized void increment() {
        
        executedCount++;
        
    }
    
    /**
     * Requests an increment of the counter of command executions. The increment is not
     * necessarily processed immediately.
     */
    protected static void incrementCount() {
        
        EXECUTOR.execute( INCREMENT_TASK );
        
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
        
        return executedCount;
        
    }

}
