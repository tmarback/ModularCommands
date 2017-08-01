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

import sx.blah.discord.api.IDiscordClient;

/**
 * A registry that is linked to a certain Discord4J client.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-13
 */
public class ClientCommandRegistry extends CommandRegistry {
    
    /** Qualifier for this registry type. */
    public static final String QUALIFIER = "client";
    
    /** Name for all instances of this class. */
    private static final String NAME = "Core";
    
    /**
     * Initializes a registry linked to the given client.
     *
     * @param client The client that the registry should be linked to.
     * @throws NullPointerException if the client received is null.
     * @see CommandRegistry#CommandRegistry(Object)
     */
    public ClientCommandRegistry( IDiscordClient client ) throws NullPointerException {

        super( client );

    }
    
    /**
     * Creates a new command registry to be linked to the given client that is
     * initialized from the given placeholder.
     *
     * @param client The client that will be linked to the initialized registry.
     * @throws NullPointerException if the client or placeholder received is null.
     * @see CommandRegistry#CommandRegistry(Object, PlaceholderCommandRegistry)
     */
    public ClientCommandRegistry( IDiscordClient client, PlaceholderCommandRegistry placeholder )
            throws NullPointerException {
        
        super( client, placeholder );
        
    }
    
    { // Sets the registry to be essential.
        
        this.essential = true;
        
    }
    
    @Override
    public IDiscordClient getLinkedObject() {
        
        return (IDiscordClient) super.getLinkedObject();
        
    }
    
    /**
     * Retrieves the client that this registry is linked to.
     *
     * @return The client linked to this registry, or null if this registry is not
     *         linked to a client.
     */
    public IDiscordClient getClient() {
        
        return getLinkedObject();
        
    }

    @Override
    public String getName() {

        return NAME;
        
    }

    @Override
    public String getQualifier() {

        return QUALIFIER;
        
    }
    
    /**
     * Retrieves the <i>effective</i> prefix of the calling instance.<br>
     * If the calling instance has no declared prefix, uses the
     * {@link CommandRegistry#DEFAULT_PREFIX default prefix}.
     *
     * @return The effective prefix used for this instance.
     */
    @Override
    public String getEffectivePrefix() {
        
        return ( getPrefix() != null ) ? getPrefix() : CommandRegistry.DEFAULT_PREFIX;
        
    }

}
