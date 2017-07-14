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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.thiagotgm.modular_commands.registry.ClientCommandRegistry;
import com.github.thiagotgm.modular_commands.registry.ModuleCommandRegistry;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * A registry that allows registering of commands to be later called, or other registries
 * as subregistries.
 * <p>
 * A registry can only be registered to one parent registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public abstract class CommandRegistry implements Disableable, Prefixed, Comparable<CommandRegistry> {
    
    /** Default prefix inherited when no prefix was specified in the inheritance chain. */
    public static final String DEFAULT_PREFIX = "?";
    
    /** Separator used between qualifier and name in the qualified name. */
    protected static final String QUALIFIER_SEPARATOR = ":";
    
    private boolean enabled;
    private String prefix;
    
    /** Table of commands, stored by name. */
    private Map<String, ICommand> commands;
    /** Table of commands with specified prefix, stored by (each) signature. */
    private Map<String, PriorityQueue<ICommand>> withPrefix;
    /** Table of commands with no specified prefix, stored by (each) signature. */
    private Map<String, PriorityQueue<ICommand>> noPrefix;
    
    private CommandRegistry parentRegistry;
    private final Map<String, CommandRegistry> subRegistries;
    private static final Map<IDiscordClient, CommandRegistry> registries = new HashMap<>();
    
    /**
     * Creates a new registry.
     */
    protected CommandRegistry() {

        this.enabled = true;
        
        this.commands = new HashMap<>();
        this.withPrefix = new HashMap<>();
        this.noPrefix = new HashMap<>();
        
        this.subRegistries = new TreeMap<>();
        
    }
    
    /**
     * Retrieves the registry linked to a given client.
     * <p>
     * If no such registry is registered, creates one.
     *
     * @param client The client whose linked registry should be retrieved.
     * @return The registry linked to the given client.
     * @throws NullPointerException if the client passed in is null.
     */
    public static CommandRegistry getRegistry( IDiscordClient client ) throws NullPointerException {
        
        if ( client == null ) {
            throw new NullPointerException( "Client argument cannot be null." );
        }        
        CommandRegistry registry = registries.get( client );
        if ( registry == null ) { // Registry not found, create one.
            registry = new ClientCommandRegistry( client );
            registries.put( client, registry );
        }
        return registry;
        
    }
    
    /**
     * Given the qualifier of a registry type and the name of a registry, retrieves
     * the qualified name.
     *
     * @param qualifier Qualifier of the registry type.
     * @param name Name of the registry.
     * @return The qualified registry name.
     */
    protected static String qualifiedName( String qualifier, String name ) {
        
        return qualifier + QUALIFIER_SEPARATOR + name;
        
    }
    
    /**
     * Retrieves the name of the registry.
     *
     * @return The name that identifies the registry.
     */
    public abstract String getName();
    
    
    /**
     * Retrieves the qualifier that identifies the registry type.
     *
     * @return The qualifier of caller's registry type.
     */
    public abstract String getQualifier();
    
    /**
     * Retrieves the fully qualified name of the registry.
     *
     * @return The qualified name.
     */
    public String getQualifiedName() {
        
        return qualifiedName( getQualifier(), getName() );
        
    }
    
    /**
     * Registers a new subregistry into this registry.
     * <p>
     * If the given registry was already registered into another registry, it is
     * unregistered from it.
     * <p>
     * If the registry is already registered as a subregistry, does nothing.
     *
     * @param registry The subregistry to register.
     * @throws IllegalArgumentException if the registry given is the calling registry (attempted to register
     *                                  a registry into itself).
     */
    protected void registerSubRegistry( CommandRegistry registry ) throws IllegalArgumentException {
        
        if ( registry == this ) {
            throw new IllegalArgumentException( "Attempted to register a registry into itself." );
        }
        String qualifiedName = registry.getQualifiedName();
        if ( subRegistries.containsKey( qualifiedName ) ) {
            return; // If already registered, do nothing.
        }
        subRegistries.put( qualifiedName, registry );
        if ( registry.getRegistry() != null ) { // Unregister from previous registry if any.
            registry.getRegistry().unregisterSubRegistry( registry );
        }
        registry.setRegistry( this );
        
    }
    
    /**
     * Unregisters the given registry from this registry. If the given registry was not a
     * subregistry of the calling registry, nothing is changed.
     *
     * @param registry The subregistry to be unregistered.
     */
    protected void unregisterSubRegistry( CommandRegistry registry ) {
        
        if ( subRegistries.remove( registry.getQualifiedName() ) != null ) {
            registry.setRegistry( null );
        }
        
    }
    
    /**
     * Retrieves the subregistry linked to a given module.
     * <p>
     * If no such subregistry is registered, creates one.
     *
     * @param module The module whose linked subregistry should be retrieved.
     * @return The subregistry linked to the given module.
     * @throws NullPointerException if the module passed in is null.
     */
    public CommandRegistry getSubRegistry( IModule module ) throws NullPointerException {
        
        if ( module == null ) {
            throw new NullPointerException( "Module argument cannot be null." );
        }
        String qualifiedName = qualifiedName( ModuleCommandRegistry.QUALIFIER, module.getName() );
        CommandRegistry registry = subRegistries.get( qualifiedName );
        if ( registry == null ) { // Subregistry not found, create one.
            registry = new ModuleCommandRegistry( module );
            registerSubRegistry( registry );
        }
        return registry;
        
    }
    
    /**
     * Retrieves the subregistries registered in this registry.
     *
     * @return The registered subregistries.
     */
    public SortedSet<CommandRegistry> getSubRegistries() {
        
        return new TreeSet<>( subRegistries.values() );
        
    }
    
    @Override
    public CommandRegistry getRegistry() {
        
        return parentRegistry;
        
    }
    
    /**
     * Retrieves the registry that is the root of the calling registry's inheritance chain.<br>
     * That is, retrieves this registry's farthest parent registry, the first in the chain that
     * is not registered to any registry.
     *
     * @return The root of this registry's inheritance chain.
     */
    public CommandRegistry getRoot() {
        
        CommandRegistry cur = this;
        while ( cur.getRegistry() != null ) {
            cur = cur.getRegistry();
        }
        return cur;
        
    }
    
    @Override
    public void setRegistry( CommandRegistry registry ) {
        
        this.parentRegistry = registry;
        
    }
    
    /**
     * Sets the prefix of this registry.
     *
     * @param prefix The prefix to use for this registry.
     */
    public void setPrefix( String prefix ) {
        
        this.prefix = prefix;
        
    }
    
    @Override
    public String getPrefix() {
        
        return prefix;
        
    }

    @Override
    public boolean isEnabled() {

        return enabled;
        
    }

    @Override
    public void setEnabled( boolean enabled ) throws IllegalStateException {

        if ( isEssential() && !enabled ) {
            throw new IllegalStateException( "Attempted to disabled an essential registry." );
        }
        this.enabled = enabled;

    }
    
    /**
     * Compares two CommandRegistries.
     * <p>
     * Registries are compared using their names. In case of ties, their qualifiers are compared.
     * <p>
     * Used to sort their display order.
     *
     * @param cr The registry to compare to.
     * @return A negative value if this registry is lesser than the given registry (comes first).
     *         A positive value if this registry is greater than the given registry (comes after).
     *         Zero if both are equal (either can come first).
     * @see String#compareTo(String)
     */
    @Override
    public int compareTo( CommandRegistry cr ) {
        
        int compare = this.getName().compareTo( cr.getName() );
        return ( compare != 0 ) ? compare : this.getQualifier().compareTo( cr.getQualifier() );
        
    }
    
    /**
     * Registers a command into the calling registry.<br>
     * The command will fail to be added if there is already a command in the registry hierarchy
     * (parent and sub registries) with the same name (eg the command name must be unique).
     * <p>
     * If the command was already registered to another registry (that is not part of the
     * hierarchy of the calling registry), it is unregistered from it first.
     *
     * @param command The command to be registered.
     * @return true if the command was registered successfully, false if it could not be registered.
     */
    public boolean registerCommand( ICommand command ) {
        
        if ( getRoot().getCommand( command.getName() ) != null ) {
            return false; // Check if there is a command in the chain with the same name.
        }
        if ( command.getRegistry() != null ) { // Unregister from current registry if any.
            command.getRegistry().unregisterCommand( command );
        }
        command.setRegistry( this );
        commands.put( command.getName(), command ); // Add command to main table.
        
        /* Add identifiers to the appropriate table */
        Collection<String> identifiers;
        Map<String, PriorityQueue<ICommand>> commandTable;
        if ( command.getPrefix() != null ) { // Command specifies a prefix.
            identifiers = command.getSignatures();
            commandTable = withPrefix; // Add signatures to table of commands with prefix.
        } else { // Command does not specify a prefix.
            identifiers = command.getAliases();
            commandTable = noPrefix; // Add aliases to table of commands without prefix.
        }
        for ( String identifier : identifiers ) { // For each identifier (signature or alias).
            
            PriorityQueue<ICommand> queue = commandTable.get( identifier ); // Get queue of commands with
            if ( queue == null ) {                                          // that identifier.
                queue = new PriorityQueue<>(); // If none, initialize it.
                commandTable.put( identifier, queue );
            }
            queue.add( command ); // Add command to list of commands with that identifier.
            
        }
        return true;
        
    }
    
    /**
     * Unregisters a command from this registry.
     *
     * @param command The command to be unregistered.
     * @return true if the command was unregistered successfully;<br>
     *         false if it was not registered in this registry.
     */
    public boolean unregisterCommand( ICommand command ) {
        
        if ( command == null ) {
            return false; // Received null command.
        }
        if ( commands.get( command.getName() ) != command ) {
            return false; // No command with this name, or the command registered with this name was not
        }                 // the one given.
        commands.remove( command.getName() );
        
        /* Remove identifiers from the appropriate table */
        Collection<String> identifiers;
        Map<String, PriorityQueue<ICommand>> commandTable;
        if ( command.getPrefix() != null ) { // Command specifies a prefix.
            identifiers = command.getSignatures();
            commandTable = withPrefix; // Add signatures to table of commands with prefix.
        } else { // Command does not specify a prefix.
            identifiers = command.getAliases();
            commandTable = noPrefix; // Add aliases to table of commands without prefix.
        }
        for ( String identifier : identifiers ) { // For each identifier (signature or alias).
            
            commandTable.get( identifier ).remove( command ); // Remove command from the queue of commands with
                                                              // that identifier.
        }
        
        command.setRegistry( null );
        return true;
        
    }
    
    /**
     * Retrieves the command registered in this registry that has the given name, if
     * one exists.
     *
     * @param name The name of the command.
     * @return The command in this registry with the given name, or null if there is no such command.
     */
    public ICommand getRegisteredCommand( String name ) {
        
        return commands.get( name );
        
    }
    
    /**
     * Retrieves all commands registered in this registry.
     * <p>
     * The returned set is sorted by the lexicographical order of the command names.
     *
     * @return The commands registered in this registry.
     */
    public SortedSet<ICommand> getRegisteredCommands() {
        
        SortedSet<ICommand> commands = new TreeSet<>( ( c1, c2 ) -> {
            // Compare elements by their names.
            return c1.getName().compareTo( c2.getName() );
            
        });
        commands.addAll( this.commands.values() );
        return commands;
        
    }
    
    /**
     * Retrieves the command registered in this registry or its subregistries that has
     * the given name, if one exists.
     * <p>
     * The search on the subregistries also includes their respective subregistries
     * (eg searches recursively).
     *
     * @param name The name of the command.
     * @return The command in this registry or its subregistries with the given name,
     *         or null if there is no such command.
     */
    public ICommand getCommand( String name ) {
        
        ICommand command = getRegisteredCommand( name );
        if ( command != null ) {
            return command; // Found command in this registry.
        }
        for ( CommandRegistry subRegistry : getSubRegistries() ) { // Check each subregistry.
            
            command = subRegistry.getCommand( name );
            if ( command != null ) {
                return command; // Found command in a subregistry.
            }
            
        }
        return null; // Command not found.
        
    }
    
    /**
     * Retrieves all commands registered in this registry or its subregistries.
     * <p>
     * The returned set is sorted by the lexicographical order of the command names.
     * <p>
     * The search on the subregistries also includes their respective subregistries
     * (eg searches recursively).
     *
     * @return The commands registered in this registry or its subregistries.
     */
    public SortedSet<ICommand> getCommands() {
        
        SortedSet<ICommand> commands = getRegisteredCommands();
        for ( CommandRegistry subRegistry : getSubRegistries() ) {
            // Add commands from subregistries.
            commands.addAll( subRegistry.getCommands() );
            
        }
        return commands;
        
    }
    
}
