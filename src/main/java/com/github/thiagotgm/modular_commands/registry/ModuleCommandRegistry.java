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

package com.github.thiagotgm.modular_commands.registry;

import com.github.thiagotgm.modular_commands.api.CommandRegistry;

import sx.blah.discord.modules.IModule;

/**
 * A registry that is linked to a certain Discord4J module.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-13
 */
public class ModuleCommandRegistry extends CommandRegistry {
    
    /** Qualifier for this registry type. */
    public static final String QUALIFIER = "module";

    private final IModule module;
    private boolean essential;
    
    /**
     * Initializes a registry linked to the given module.
     *
     * @param module The module that the registry should be linked to.
     * @throws NullPointerException if the module received is null.
     */
    public ModuleCommandRegistry( IModule module ) throws NullPointerException {

        if ( module == null ) {
            throw new NullPointerException( "Linked module cannot be null." );
        }
        this.module = module;
        this.essential = false;

    }
    
    /**
     * Retrieves the module that this registry is linked to.
     *
     * @return The module linked to this registry.
     */
    public IModule getModule() {

        return module;
        
    }
    
    /**
     * Sets whether this registry is essential.
     *
     * @param essential If true, the registry is marked as essential. If false, it is marked
     *                  as not essential.
     * @see #isEssential()
     */
    public void setEssential( boolean essential ) {
        
        this.essential = essential;
        
    }
    
    @Override
    public boolean isEssential() {
        
        return essential;
        
    }

    @Override
    public String getName() {

        return module.getName();
        
    }
    
    @Override
    public String getQualifier() {
        
        return QUALIFIER;
        
    }

}
