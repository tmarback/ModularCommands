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
import com.github.thiagotgm.modular_commands.api.ICommand;

/**
 * A placeholder for a command registry that does not currently exist, but may be created
 * in the future.
 * <p>
 * Does not allow registering commands, but allows creating and retrieving subregistries
 * that will be transfered to the actual registry later.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-31
 */
public class PlaceholderCommandRegistry extends CommandRegistry {
    
    private final String qualifier;
    private final String name;

    /**
     * Creates a placeholder for a registry that has the given qualifier and name.
     *
     * @param qualifier The qualifier of the registry the placeholder represents.
     * @param name The name of the registry the placeholder represents.
     * @throws NullPointerException if either argument is null.
     */
    public PlaceholderCommandRegistry( String qualifier, String name )
            throws NullPointerException {
        
        super();
        
        if ( ( qualifier == null ) || ( name == null ) ) {
            throw new NullPointerException( "Arguments cannot be null." );
        }
        
        this.qualifier = qualifier;
        this.name = name;
        
    }
    
    /**
     * Creates a placeholder for the given registry, transferring its subregistries to
     * the constructed placeholder.
     * <p>
     * To be used when the registry is being deleted.
     *
     * @param registry The registry to construct a placeholder for.
     * @throws NullPointerException if the argument is null.
     */
    public PlaceholderCommandRegistry( CommandRegistry registry )
            throws NullPointerException {
        
        super();
        
        if ( registry == null ) {
            throw new NullPointerException( "Argument cannot be null." );
        }
        
        this.qualifier = registry.getQualifier();
        this.name = registry.getName();
        
        /* Transfers placeholder subregistries and placeholders to this */
        for ( CommandRegistry subRegistry : registry.getSubRegistries() ) {
            
            this.subRegistries.put( subRegistry.getQualifiedName(), subRegistry );
            subRegistry.setRegistry( this );
            
        }
        for ( PlaceholderCommandRegistry placeholder : registry.getPlaceholders() ) {
            
            this.placeholders.put( placeholder.getQualifiedName(), placeholder );
            placeholder.setRegistry( this );
            
        }
        
    }

    @Override
    public String getName() {

        return name;
        
    }

    @Override
    public String getQualifier() {

        return qualifier;
        
    }
    
    /**
     * Placeholder registries cannot register commands. This method throws an exception.
     *
     * @throws UnsupportedOperationException if called.
     */
    @Override
    public boolean registerCommand( ICommand command ) throws UnsupportedOperationException {
        
        throw new UnsupportedOperationException( "A placeholder registry cannot register commands." );
        
    }

}
