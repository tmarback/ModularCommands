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

/**
 * A registry that allows registering of commands to be later called.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public abstract class CommandRegistry implements Disableable {

    public CommandRegistry() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isEnabled() {

        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setEnabled( boolean enabled ) throws IllegalStateException {

        // TODO Auto-generated method stub

    }
    
    public String getEffectivePrefix() {
        
        // TODO
        return null;
        
    }

}
