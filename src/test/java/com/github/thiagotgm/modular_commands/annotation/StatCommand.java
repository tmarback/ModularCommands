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

package com.github.thiagotgm.modular_commands.annotation;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandStats;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;

/**
 * Command that shows command count.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-09-13
 */
public class StatCommand {

    @MainCommand(
            name = "Command stats displayer",
            aliases = "stats",
            description = "Shows command execution stats.",
            usage = "{}stats"
            )
    public void showCount( CommandContext context ) {
        
        String message = String.format( "Executed %d commands.", CommandStats.getCount() );
        context.getReplyBuilder().withContent( message ).build();
        
    }

}
