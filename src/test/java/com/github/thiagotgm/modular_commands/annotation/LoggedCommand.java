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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;

/**
 * Command that logs its call and has a delayed success handler.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2018-09-17
 */
public class LoggedCommand {

    private static final Logger LOG = LoggerFactory.getLogger( LoggedCommand.class );

    @MainCommand(
            name = "Logged command",
            aliases = "log",
            successHandler = "success",
            onSuccessDelay = 1000
            )
    public void command( CommandContext context ) {

        LOG.info( "Command was called!" );

    }
    
    @SuccessHandler( "success" )
    public void success( CommandContext context ) {
        
        LOG.info( "Success!" );
        
    }

}
