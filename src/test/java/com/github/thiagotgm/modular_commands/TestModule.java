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

/**
 * Test version of the module class with fixed versions.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-14
 */
public class TestModule extends ModularCommandsModule {

    @Override
    public String getMinimumDiscord4JVersion() {

        return "2.8.4";
        
    }
    
    @Override
    public String getVersion() {

        return "0.1.0";
        
    }

}
