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

import com.github.thiagotgm.modular_commands.api.CommandHandler;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Module class for the framework. Registers the command handler and the default
 * commands.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public class ModularCommandsModule implements IModule {
    
    private static final String MODULE_NAME = "Modular Commands";
    
    private CommandHandler handler;
    private IDiscordClient client;

    @Override
    public void disable() {

        client.getDispatcher().unregisterListener( handler );
        client = null; // Unregisters the handler to stop receiving events.
        handler = null;

    }

    @Override
    public boolean enable( IDiscordClient arg0 ) {

        handler = new CommandHandler( CommandRegistry.getRegistry( arg0 ) );
        arg0.getDispatcher().registerListener( handler ); // Create a handler and register it.
        client = arg0;
        return true;
        
    }

    @Override
    public String getAuthor() {

        return "ThiagoTGM";
        
    }

    @Override
    public String getMinimumDiscord4JVersion() {

        return getClass().getPackage().getSpecificationVersion();
        
    }

    @Override
    public String getName() {

        return MODULE_NAME;
        
    }

    @Override
    public String getVersion() {

        return getClass().getPackage().getImplementationVersion();
        
    }

}
