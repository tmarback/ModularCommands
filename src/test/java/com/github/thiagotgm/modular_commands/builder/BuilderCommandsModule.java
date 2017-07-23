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

package com.github.thiagotgm.modular_commands.builder;

import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.CommandBuilder;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Module for testing making commands through the CommandBuilder.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-19
 */
public class BuilderCommandsModule implements IModule {

    @Override
    public boolean enable( IDiscordClient client ) {

        ICommand c1 = new CommandBuilder( "Factory Test" )
                .withAliases( new String[] { "build" } )
                .onExecute( ( context  ) -> {
                    
                    context.getReplyBuilder().withContent( "Command was built!" ).build();
                    return true;
                    
                }).build();
        ICommand c2 = new CommandBuilder( "Factory Test (w/ prefix)" )
                .withAliases( new String[] { "build" } )
                .withPrefix( "build$" )
                .onExecute( ( context  ) -> {
                    
                    context.getReplyBuilder().withContent( "Command was also built!" ).build();
                    return true;
                    
                }).build();
        
        CommandRegistry reg = CommandRegistry.getRegistry( client ).getSubRegistry( this );
        reg.registerCommand( c1 );
        reg.registerCommand( c2 );
        
        return true;
    }

    @Override
    public void disable() {

        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {

        return "Factory commands";
        
    }

    @Override
    public String getAuthor() {

        return "ThiagoTGM";
        
    }

    @Override
    public String getVersion() {

        return "1.0.0";
 
    }

    @Override
    public String getMinimumDiscord4JVersion() {

        return "2.8.4";
        
    }

}
