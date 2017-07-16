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

package com.github.thiagotgm.modular_commands;

import java.util.Scanner;

import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.ModuleLoader;

/**
 * Small bot for testing commands.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-16
 */
public class TestBot {
    
    public static void main( String[] args ) {
        
        Scanner in = new Scanner( TestBot.class.getClassLoader().getResourceAsStream( "testToken.txt" ) );
        String token = in.next();
        in.close();
        IDiscordClient client = new ClientBuilder().withToken( token ).build();
        ModuleLoader loader = client.getModuleLoader();
        loader.loadModule( new TestModule() );
        loader.loadModule( new TestModuleModule() );
        CommandRegistry reg = CommandRegistry.getRegistry( client );
        reg.registerCommand( new InterfaceCommand() );
        reg.registerCommand( new OverrideableCommand() );
        reg.registerCommand( new NonOverrideableCommand() );
        reg.registerCommand( new PermissionCommand() );
        reg.registerCommand( new NeedsOwnerCommand() );
        client.login();
        for ( ICommand c : reg.getCommands() ) {
            
            System.out.println( c.getSignatures() );
            
        }
        reg.registerCommand( new InterfaceCommand() );
        
    }

}
