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
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;

/**
 * Annotation-based commands.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-21
 */
public class AnnotatedCommands {

    @MainCommand(
            name = "Annotated main command",
            aliases = { "ano" },
            subCommands = { "Annotated sub command" },
            successHandler = "handle",
            description = "This is a sample command.",
            usage = "?ano"
            )
    public void reply( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SubCommand(
            name = "Annotated sub command",
            aliases = { "hana" },
            onSuccessDelay = 5000,
            successHandler = "handle"
            )
    public void sub( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "*cries*" ).build();
        
    }
    
    @MainCommand(
            name = "Unloadable main command",
            aliases = { "ano" },
            subCommands = { "null" }
            )
    public void fail( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SubCommand(
            name = "Unloadable sub command",
            aliases = { "ano" },
            subCommands = { "null" }
            )
    public void fail2( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SuccessHandler( "handle" )
    public void success( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Success!" ).build();
        
    }
    
    @MainCommand(
            name = "Incorrect main command",
            aliases = { "wrong" }
            )
    public void wrong( CommandContext context, boolean thing ) { }
    
    @MainCommand(
            name = "Prefixed annotated command",
            aliases = { "king" },
            prefix = ">-<|"
            )
    public void kingKong( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "kong!" ).build();
        
    }

}
