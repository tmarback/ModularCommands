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

import com.github.thiagotgm.modular_commands.api.Argument;
import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;

import sx.blah.discord.util.MessageBuilder;

public class MoreAnnotatedCommands {

    @MainCommand(
            name = "Argument Checker",
            aliases = { "args", "arguments" },
            successHandler = "done"
            )
    public void argument( CommandContext context ) {
        
        MessageBuilder builder = context.getReplyBuilder().withContent( "Arguments:" );
        for ( Argument arg : context.getArguments() ) {
            
            builder.appendContent( "\n\"`" + arg.getArgument() + "`\" (" + arg.getType() + ")" );
            
        }
        builder.build();
        
    }
    
    @SuccessHandler( "done" )
    public void handler( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "====[ Done ]====" ).build();
        
    }

}
