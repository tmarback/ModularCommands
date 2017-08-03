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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thiagotgm.modular_commands.command.annotation.AnnotationParser;
import com.github.thiagotgm.modular_commands.registry.ClientCommandRegistry;
import com.github.thiagotgm.modular_commands.registry.ModuleCommandRegistry;
import com.github.thiagotgm.modular_commands.registry.PlaceholderCommandRegistry;
import com.github.thiagotgm.modular_commands.registry.annotation.Essential;
import com.github.thiagotgm.modular_commands.registry.annotation.HasPrefix;

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
    
    /** Default prefix to be inherited when no prefix was specified in the inheritance chain. */
    public static final String DEFAULT_PREFIX = "?";
    
    /** Separator used between qualifier and name in the qualified name. */
    protected static final String QUALIFIER_SEPARATOR = ":";
    
    private static final Logger LOG = LoggerFactory.getLogger( CommandRegistry.class );
    
    private final Object linkedObject;
    private volatile boolean enabled;
    private volatile String prefix;
    private volatile Predicate<CommandContext> contextCheck;
    private final List<Predicate<CommandContext>> contextChecks;
    private volatile long lastChanged;
    protected volatile boolean essential;
    
    /** Table of commands, stored by name. */
    private final Map<String, ICommand> commands;
    /** Table of commands with specified prefix, stored by (each) signature. */
    private final Map<String, PriorityQueue<ICommand>> withPrefix;
    /** Table of commands with no specified prefix, stored by (each) signature. */
    private final Map<String, PriorityQueue<ICommand>> noPrefix;
    
    private volatile CommandRegistry parentRegistry;
    protected final Map<String, CommandRegistry> subRegistries;
    protected final Map<String, PlaceholderCommandRegistry> placeholders;
    private static final Map<IDiscordClient, ClientCommandRegistry> registries =
            Collections.synchronizedMap( new HashMap<>() );
    
    protected static final Map<Class<?>, Class<? extends CommandRegistry>> registryTypes;
    protected static final Map<Class<?>, String> qualifiers;
    
    static { // Builds type-registry-qualifier maps.
        
        Map<Class<?>, Class<? extends CommandRegistry>> registryMap = new HashMap<>();
        Map<Class<?>, String> qualifierMap = new HashMap<>();
        
        registryMap.put( IDiscordClient.class, ClientCommandRegistry.class );
        qualifierMap.put( IDiscordClient.class, ClientCommandRegistry.QUALIFIER );
        
        registryMap.put( IModule.class, ModuleCommandRegistry.class );
        qualifierMap.put( IModule.class, ModuleCommandRegistry.QUALIFIER );
        
        registryTypes = Collections.synchronizedMap( registryMap );
        qualifiers = Collections.synchronizedMap( qualifierMap );
        
    }
    
    { // Initializes default values.
        
        this.enabled = true;
        this.prefix = null;
        this.contextCheck = null;
        this.contextChecks = Collections.synchronizedList( new LinkedList<>() );
        this.lastChanged = System.currentTimeMillis();
        
        this.commands = Collections.synchronizedMap( new HashMap<>() );
        this.withPrefix = Collections.synchronizedMap( new HashMap<>() );
        this.noPrefix = Collections.synchronizedMap( new HashMap<>() );
        
        this.subRegistries = Collections.synchronizedMap( new TreeMap<>() );
        this.placeholders = Collections.synchronizedMap( new HashMap<>() );
        
    }
    
    /**
     * Creates a new registry with no declared prefix and linked to no object.
     */
    protected CommandRegistry() {
        
        this.linkedObject = null;
        this.essential = false;
        
    }
    
    /**
     * Creates a new command registry to be linked to the given object.
     * <p>
     * If the object's class has a {@link HasPrefix} annotation, the registry
     * will be intialized to have the prefix specified in the annotation. Else,
     * the registry will have no declared prefix.<br>
     * If it has the {@link Essential} annotation, the registry is initialized to
     * be essential. Else, it is initialized to not be essential.
     *
     * @param obj The object that will be linked to the initialized registry.
     * @throws NullPointerException if the object received is null.
     */
    protected CommandRegistry( Object obj ) throws NullPointerException {
        
        if ( obj == null ) {
            throw new NullPointerException( "Linked object cannot be null." );
        }
        
        this.linkedObject = obj;
        
        HasPrefix prefix = obj.getClass().getDeclaredAnnotation( HasPrefix.class );
        if ( prefix != null ) { // A prefix was specified in the object's type.
            this.prefix = prefix.value();
        }
        
        if ( obj.getClass().isAnnotationPresent( Essential.class ) ) {
            this.essential = true; // Marked to be initialized as essential.
        } else {
            this.essential = false;
        }
        
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
    public static ClientCommandRegistry getRegistry( IDiscordClient client )
            throws NullPointerException {
        
        if ( client == null ) {
            throw new NullPointerException( "Client argument cannot be null." );
        }        
        ClientCommandRegistry registry = registries.get( client );
        if ( registry == null ) { // Registry not found, create one.
            registry = new ClientCommandRegistry( client );
            registries.put( client, registry );
        }
        return registry;
        
    }
    
    /**
     * Determines if there is a registry that is linked to the given client.
     *
     * @param client The client to check for.
     * @return true if there is a registry linked to the given client. false otherwise.
     * @throws NullPointerException if the client passed in is null.
     */
    public static boolean hasRegistry( IDiscordClient client ) throws NullPointerException {
        
        if ( client == null ) {
            throw new NullPointerException( "Client argument cannot be null." );
        }
        return registries.containsKey( client );
        
    }
    
    /**
     * Removes the registry that is linked to the given client, if there is one.
     * <p>
     * After removing the registry, the next to call to {@link #getRegistry(IDiscordClient)}
     * using the given client will create a new registry.
     *
     * @param client The client whose linked registry is to be removed.
     * @return The (removed) registry that is linked to the given client, or null if there was no
     *         such registry.
     * @throws NullPointerException if the client passed in is null.
     */
    public static ClientCommandRegistry removeRegistry( IDiscordClient client )
            throws NullPointerException {
        
        if ( client == null ) {
            throw new NullPointerException( "Client argument cannot be null." );
        }
        return registries.remove( client );
        
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
     * Given a class and a name, retrieves the qualified name of the registry that
     * has the given name and is associated to an object of the given type.
     *
     * @param linkedClass The class of objects that the registry type links to.
     * @param name The name of the registry.
     * @return The qualified name of the registry.
     */
    protected static String qualifiedName( Class<?> linkedClass, String name ) {
        
        return qualifiedName( qualifiers.get( linkedClass ), name );
        
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
     * Retrieves the object that the registry is linked to.
     * <p>
     * Will return null if this is a placeholder (placeholders are not
     * linked to any object).
     *
     * @return The linked object.
     */
    public Object getLinkedObject() {
        
        return linkedObject;
        
    }
    
    /**
     * Transfers all subregistries (including placeholders) in the given
     * registry to the calling registry.
     *
     * @param registry The registry to get subregistries from.
     */
    protected void transferSubRegistries( CommandRegistry registry ) {
        
        LOG.trace( "Transferring subregistries and placeholders from \"{}\" to \"{}\".",
                registry.getQualifiedName(), getQualifiedName() );
        
        /* Transfer subregistries */
        Iterator<CommandRegistry> subRegistryIter = registry.subRegistries.values().iterator();
        while ( subRegistryIter.hasNext() ) {
            
            CommandRegistry subRegistry = subRegistryIter.next();
            this.subRegistries.put( subRegistry.getQualifiedName(), subRegistry );
            subRegistry.setRegistry( this ); // Add to this registry.
            subRegistryIter.remove(); // Remove from the last one.
            
        }
        
        /* Transfer placeholders */
        Iterator<PlaceholderCommandRegistry> placeholderIter = registry.placeholders.values()
                .iterator();
        while ( placeholderIter.hasNext() ) {
            
            PlaceholderCommandRegistry placeholder = placeholderIter.next();
            this.placeholders.put( placeholder.getQualifiedName(), placeholder );
            placeholder.setRegistry( this ); // Add to this registry.
            placeholderIter.remove(); // Remove from the last one.
            
        }
        setLastChanged( System.currentTimeMillis() );
        registry.setLastChanged( System.currentTimeMillis() );
        
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
        LOG.info( "Adding subregistry \"{}\" to \"{}\".",
                qualifiedName, getQualifiedName() );
        setLastChanged( System.currentTimeMillis() );
        
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
            LOG.info( "Removing subregistry \"{}\" from \"{}\".",
                    registry.getQualifiedName(), getQualifiedName() );
        }
        setLastChanged( System.currentTimeMillis() );
        
    }
    
    /**
     * Retrieves the subregistry that has the given qualified name, if one exists.
     *
     * @param qualifiedName The qualified name of the subregistry.
     * @return The subregistry with the qualified name, or null if none exist.
     * @throws NullPointerException if the name passed in is null.
     */
    public CommandRegistry getSubRegistry( String qualifiedName ) {
        
        return subRegistries.get( qualifiedName );
        
    }
    
    /**
     * Retrieves the subregistry that is associated to an object of the given 
     * class and has the given name.
     * <p>
     * If no such registry exists, but there is a placeholder for it, retrieves the
     * placeholder.<br>
     * Else, creates a placeholder and retrieves it.
     *
     * @param linkedClass The class of objects that the desired registry links to.
     * @param name The simple name of the subregistry.
     * @return The subregistry with the qualified name (actual or placeholder).
     * @throws NullPointerException if one of the arguments is null.
     * @throws IllegalArgumentException if there is no registry for the given type.
     */
    public CommandRegistry getSubRegistry( Class<?> linkedClass, String name )
            throws NullPointerException, IllegalArgumentException {
        
        if ( ( linkedClass == null ) || ( name == null ) ) {
            throw new NullPointerException( "Arguments cannot be null." );
        }
        
        if ( !registryTypes.containsKey( linkedClass ) ) {
            throw new IllegalArgumentException( "There are no registries for the given class." );
        }
        
        String qualifiedName = qualifiedName( linkedClass, name );
        CommandRegistry registry = subRegistries.get( qualifiedName );
        if ( registry != null ) {
            return registry; // Has an actual registry.
        }
        registry = placeholders.get( qualifiedName );
        if ( registry != null ) {
            return registry; // Has a placeholder.
        }
        LOG.info( "Creating placeholder for \"{}\" in \"{}\".", qualifiedName, getQualifiedName() );
        PlaceholderCommandRegistry placeholder = // Create a new placeholder.
                new PlaceholderCommandRegistry( qualifiers.get( linkedClass ), name );
        placeholders.put( qualifiedName, placeholder );
        placeholder.setRegistry( this );
        return placeholder;
        
    }
    
    /**
     * Deletes all empty placeholder registries starting from the calling registry and
     * going up the registry hierarchy, until hitting either a non-placeholder registry
     * or a non-empty placeholder.
     */
    private void cleanPlaceholders() {
        
        CommandRegistry registry = this;
        while ( ( registry instanceof PlaceholderCommandRegistry ) &&
                ( registry.subRegistries.isEmpty() ) &&
                ( registry.placeholders.isEmpty() ) ) {
            
            CommandRegistry parent = registry.getRegistry();
            LOG.debug( "Removing empty placeholder \"{}\".", registry.getQualifiedName() );
            parent.placeholders.remove( registry );
            registry = parent;
            
        }
        
    }
    
    /**
     * Removes the subregistry with the given name that is linked to an object of the
     * given class, if it exists.
     * <p>
     * If the subregistry has subregistries and/or placeholders, leaves a placeholder
     * for it that keeps the subregistries, so that if the subregistry is recreated,
     * the subregistries (and placeholders) are brought back.<br>
     * Thus, if a registry is found and deleted, it will always be returned with no
     * subregistries or placeholders, as any it might have had are left with the
     * placeholder.<br>
     * If the subregistries should be also deleted/kept with the registry, use 
     * {@link #removeSubRegistryFull(Class, String)}.
     *
     * @param linkedClass The class of objects that the desired registry links to.
     * @param name The simple name of the subregistry.
     * @return The deleted subregistry, or null if no matching subregistry found.
     * @throws NullPointerException if one of the arguments is null.
     * @throws IllegalArgumentException if there is no registry for the given type.
     */
    protected CommandRegistry removeSubRegistry( Class<?> linkedClass, String name )
            throws NullPointerException, IllegalArgumentException {
        
        if ( ( linkedClass == null ) || ( name == null ) ) {
            throw new NullPointerException( "Arguments cannot be null." );
        }
        
        if ( !registryTypes.containsKey( linkedClass ) ) {
            throw new IllegalArgumentException( "There are no registries for the given class." );
        }
        
        String qualifiedName = qualifiedName( linkedClass, name );
        CommandRegistry subRegistry = subRegistries.get( qualifiedName );
        if ( subRegistry == null ) {
            return null; // No subregistry found.
        }
        unregisterSubRegistry( subRegistry );
        if ( subRegistry.subRegistries.isEmpty() && // No sub-subregistries
             subRegistry.placeholders.isEmpty() ) { // or placeholders.
            // Delete all ancestors that are placeholders and became irrelevant.
            cleanPlaceholders();
        } else { // Needs to keep the sub-subregistries and placeholders.
            LOG.debug( "Leaving placeholder." );
            PlaceholderCommandRegistry placeholder = new PlaceholderCommandRegistry(
                    subRegistry.getQualifier(), subRegistry.getName() );
            placeholder.transferSubRegistries( subRegistry );
            placeholders.put( qualifiedName, placeholder );
            placeholder.setRegistry( this );
        }
        return subRegistry;
        
    }
    
    /**
     * Removes the subregistry with the given name that is linked to an object of the
     * given class, if it exists.
     * <p>
     * If the subregistry has subregistries and/or placeholders, they are kept with
     * the registry and thus also deleted.<br>
     * If the subregistries should be kept in a placeholder for when/if the subregistry
     * is recreated, use {@link #removeSubRegistry(Class, String)}.
     *
     * @param linkedClass The class of objects that the desired registry links to.
     * @param name The simple name of the subregistry.
     * @return The deleted subregistry, or null if no matching subregistry found.
     * @throws NullPointerException if one of the arguments is null.
     * @throws IllegalArgumentException if there is no registry for the given type.
     */
    protected CommandRegistry removeSubRegistryFull( Class<?> linkedClass, String name )
            throws NullPointerException, IllegalArgumentException {
        
        if ( ( linkedClass == null ) || ( name == null ) ) {
            throw new NullPointerException( "Arguments cannot be null." );
        }
        
        if ( !registryTypes.containsKey( linkedClass ) ) {
            throw new IllegalArgumentException( "There are no registries for the given class." );
        }
        
        String qualifiedName = qualifiedName( linkedClass, name );
        CommandRegistry subRegistry = subRegistries.get( qualifiedName );
        if ( subRegistry == null ) {
            return null; // No subregistry found.
        }
        unregisterSubRegistry( subRegistry );
        // Delete all ancestors that are placeholders and became irrelevant.
        cleanPlaceholders();
        return subRegistry;
        
    }
    
    /**
     * Retrieves the subregistry linked to a given object under the given type,
     * that has the given name.
     * <p>
     * If no such subregistry is registered, creates one, initializing from a
     * placeholder if there is one.
     *
     * @param linkedObject The object whose linked subregistry should be retrieved.
     * @param linkedClass The specific class that the registry type links to.
     * @param name The name of the registry.
     * @return The subregistry linked to the given object.
     * @throws NullPointerException if one of the arguments is null.
     * @throws IllegalArgumentException if there is no registry for the given type.
     */
    public <T> CommandRegistry getSubRegistry( T linkedObject, Class<? super T> linkedClass,
            String name ) throws NullPointerException, IllegalArgumentException {
        
        if ( ( linkedObject == null ) || ( linkedClass == null ) || ( name == null ) ) {
            throw new NullPointerException( "Arguments cannot be null." );
        }
        
        if ( !registryTypes.containsKey( linkedClass ) ) {
            throw new IllegalArgumentException( "There are no registries for the given class." );
        }
        
        String qualifiedName = qualifiedName( linkedClass, name );
        CommandRegistry registry = subRegistries.get( qualifiedName );
        if ( registry == null ) { // Subregistry not found, create one.
            LOG.info( "Creating subregistry \"{}\" in \"{}\".", qualifiedName, getQualifiedName() );
            Class<? extends CommandRegistry> registryType = registryTypes.get( linkedClass );
            try { // Instantiate the registry.
                registry = registryType.getConstructor( linkedClass )
                        .newInstance( linkedObject );
            } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
                LOG.error( "Could not create subregistry.", e );
                return null; // Error encountered.
            }

            PlaceholderCommandRegistry placeholder = placeholders.remove( qualifiedName );
            if ( placeholder != null ) { // Registry has a placeholder.
                LOG.debug( "Absorbing placeholder." );
                registry.transferSubRegistries( placeholder );
            }
            registerSubRegistry( registry );
        }
        return registry;
        
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
        
        return getSubRegistry( module, IModule.class, module.getName() );
        
    }
    
    /**
     * Determines if there is a subregistry registered in this registry that is linked
     * to the given module.
     *
     * @param module The module to check for.
     * @return true if there is a subregistry linked to the given module. false otherwise.
     * @throws NullPointerException if the module passed in is null.
     */
    public boolean hasSubRegistry( IModule module ) throws NullPointerException {
        
        if ( module == null ) {
            throw new NullPointerException( "Module argument cannot be null." );
        }
        return subRegistries.containsKey( qualifiedName( ModuleCommandRegistry.QUALIFIER, module.getName() ) );
        
    }
    
    /**
     * Removes the subregistry that is linked to the given module from this registry, if there
     * is one.
     * <p>
     * After removing the subregistry, the next to call to {@link #getSubRegistry(IModule)}
     * using the given module will create a new subregistry.
     *
     * @param module The module whose linked subregistry is to be removed.
     * @return The (removed) subregistry that is linked to the given module, or null if there was no
     *         such subregistry.
     * @throws NullPointerException if the module passed in is null.
     */
    public CommandRegistry removeSubRegistry( IModule module ) throws NullPointerException {
        
        if ( module == null ) {
            throw new NullPointerException( "Module argument cannot be null." );
        }
        return removeSubRegistry( IModule.class, module.getName() );
        
    }
    
    /**
     * Retrieves the subregistries registered in this registry.
     *
     * @return The registered subregistries.
     */
    public NavigableSet<CommandRegistry> getSubRegistries() {
        
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
        LOG.debug( "Setting prefix of \"{}\" to \"{}\".", getQualifiedName(), prefix );
        setLastChanged( System.currentTimeMillis() );
        
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
        if ( LOG.isDebugEnabled() ) {
            LOG.debug( "Setting " + getQualifiedName() + " to " +
                    ( ( enabled ) ? "enabled" : "disabled" ) + "." );
        }
        this.enabled = enabled;

    }
    
    @Override
    public boolean isEssential() {
        
        return essential;
        
    }
    
    /**
     * Sets an operation to be ran to determine if this registry is active under
     * the context of a command.
     * <p>
     * By default there is no check operation, so the registry is active for any
     * context. This state can be restored by passing in null to this method.
     * <p>
     * If a registry is not active under a command context, then a command called under that
     * context will not be executed, thus having the same effect as if it was disabled.
     * <p>
     * This can be used to create functionality where commands in certain registries
     * are only available (or not available) if the context, or some other arbitrary internal
     * state of the program, matches a certain criteria.<br>
     * Essentially a runtime-configurable way of making a registry conditionally enabled or
     * disabled depending on the context that its commands are called from and the state of
     * the program.<br>
     * However, this does <i>not</i> replace enabled/disabled functionality. If the registry
     * was set as disabled, no commands from it will be executed, independent of any context checks.
     * The check is only called if the registry is enabled.
     *
     * @param contextCheck The check to run on the context of commands. If null, removes the current
     *                     context check.
     * @see #contextCheck(CommandContext)
     */
    public void setContextCheck( Predicate<CommandContext> contextCheck ) {
        
        this.contextCheck = contextCheck;
        contextChecks.clear();
        if ( contextCheck != null ) { // set context check.
            LOG.debug( "Setting context check for \"{}\".", getQualifiedName() );
            contextChecks.add( contextCheck );
        } else { // Remove context check.
            LOG.debug( "Removing all context checks for \"{}\".", getQualifiedName() );
        }
        
    }
    
    /**
     * Adds a context check to run in addition to the currently set context check.<br>
     * The context check becomes a composition of the currently set context check
     * AND the given context check.
     * <p>
     * If there is no currently set context check, the given check becomes the only one.
     *
     * @param contextCheck The context check to be ran in addition to the current one.
     * @throws NullPointerException if the context check given is null.
     * @see #setContextCheck(Predicate)
     * @see Predicate#and(Predicate)
     */
    public void addContextCheck( Predicate<CommandContext> contextCheck )
            throws NullPointerException {
        
        if ( contextCheck == null ) {
            throw new NullPointerException( "Context check to be added cannot be null." );
        }
        
        LOG.debug( "Adding context check for \"{}\".", getQualifiedName() );
        if ( this.contextCheck == null ) { // This is the first context check.
            this.contextCheck = contextCheck;
        } else { // This is not the first context check.
            this.contextCheck = this.contextCheck.and( contextCheck );
        }
        contextChecks.add( contextCheck );
        
    }
    
    /**
     * Removes one of the context checks being made for this registry. The context
     * check becomes a logical AND of all the remaining context checks.
     * <p>
     * If there are no remaining context checks, the context check is removed.
     *
     * @param contextCheck The context check to be removed.
     * @throws NullPointerException if the context check given is null.
     * @see #addContextCheck(Predicate)
     */
    public void removeContextCheck( Predicate<CommandContext> contextCheck )
            throws NullPointerException {
        
        if ( contextCheck == null ) {
            throw new NullPointerException( "Context check to be removed cannot be null." );
        }
        
        LOG.debug( "Removing context check for \"{}\".", getQualifiedName() );
        
        if ( contextChecks.isEmpty() ) {
            this.contextCheck = null; // No more context checks.
            return;
        } else {
            /* Remakes context check from remaining checks */
            Iterator<Predicate<CommandContext>> iter = contextChecks.iterator();
            this.contextCheck = iter.next();
            while ( iter.hasNext() ) {
                
                this.contextCheck = this.contextCheck.and( contextCheck );
                
            }
        }
        
    }
    
    /**
     * Retrieves the operation being used to determine if this registry is active under
     * the context of a command.
     * <p>
     * If there are multiple context checks set, the returned object is a logical
     * AND of all the set context checks.
     *
     * @return The check being ran on command contexts, or null if no check is set.
     * @see #addContextCheck(Predicate)
     */
    public Predicate<CommandContext> getContextCheck() {
        
        return contextCheck;
        
    }
    
    /**
     * Retrieves the operations being used to determine if this registry is active under
     * the context of a command.
     *
     * @return The list of context checks. May be empty if no check is set.
     */
    public List<Predicate<CommandContext>> getContextChecks() {
        
        return new ArrayList<>( contextChecks );
        
    }
    
    /**
     * Retrieves whether this registry is active under the given context.
     * <p>
     * This method runs the check operation set through {@link #setContextCheck(Predicate)}.<br>
     * If none were set, this is always true.
     * <p>
     * If a registry fails its context check and thus is not active, all the subregistries
     * registered under it are not active.<br>
     * This means that if some registry above this in the
     * registry hierarchy is not active under the given context, this registry is also 
     * not active, independent of the result of its own context check.
     * <p>
     * If a registry is not active under a command context, then a command called under that
     * context will not be executed, thus having the same effect as if it was disabled.
     * 
     * @param context The context to check if the registry is active for.
     * @return true if this registry is active under the given context (passed context check).<br>
     *         false if it is not active (failed context check).
     * @see #setContextCheck(Predicate)
     */
    public boolean contextCheck( CommandContext context ) {
        
        return ( ( contextCheck == null ) || contextCheck.test( context ) ) &&
               ( ( getRegistry() == null ) || getRegistry().contextCheck( context ) );
        
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
     * (placeholder subregistries are also counted).
     * <p>
     * If the command was already registered to another registry (that is not part of the
     * hierarchy of the calling registry), it is unregistered from it first.<br>
     * If it failed to be registered to this registry, it is not unregistered from the previous
     * registry.
     * <p>
     * Only main commands can be registered. Subcommands will simply fail to be added.
     *
     * @param command The command to be registered.
     * @return true if the command was registered successfully, false if it could not be registered.
     * @throws NullPointerException if the command passed in is null.
     */
    public boolean registerCommand( ICommand command ) throws NullPointerException {
        
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Attempting to register command " + getCommandString( command ) + " to \"" +
                    getQualifiedName() + "\"." );
        }
        /* Check for fail cases */
        boolean fail = false;
        if ( command.isSubCommand() ) {
            fail = true; // Sub commands cannot be registered directly.
        } else if ( getRoot().getCommand( command.getName() ) != null ) {
            fail = true; // Check if there is a command in the chain with the same name.
        }
        if ( fail ) {
            LOG.error( "Failed to register command \"{}\".", command.getName() );
            return false; // Error found.
        }
        
        /* Add to main table */
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
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Registered command \"" + command.getName() + "\"." );
        }
        setLastChanged( System.currentTimeMillis() );
        return true;
        
    }
    
    /**
     * Attempts to register all the commands in the given collection to this registry.
     * <p>
     * After the method returns, all the commands that did not fail to be registered
     * will be registered to this registry.<br>
     * They will also be unregistered from their previous registry, if any.
     * <p>
     * Commands that fail to be registered are unchanged.
     *
     * @param commands The commands to be registered.
     */
    public void registerAllCommands( Collection<ICommand> commands ) {
        
        for ( ICommand command : commands ) { // Register each command.
            
            try {
                registerCommand( command );
            } catch ( NullPointerException e ) {
                LOG.error( "Command collection included null.", e );
            }
            
        }
        
    }
    
    /**
     * Attempts to register all the commands in the given registry to this registry.
     * <p>
     * After the method returns, all the commands that did not fail to be registered
     * will be registered to this registry.<br>
     * They will also be unregistered from the given registry.
     * <p>
     * Commands that fail to be registered are unchanged.
     *
     * @param registry The registry to get commands from.
     */
    public void registerAllCommands( CommandRegistry registry ) {
        
        registerAllCommands( registry.getRegisteredCommands() );
        
    }
    
    /**
     * Parses the annotated commands from the given object and registers the parsed main
     * commands into this registry.
     *
     * @param obj The object to parse commands from.
     */
    public void registerAnnotatedCommands( Object obj ) {
        
        AnnotationParser parser = new AnnotationParser( obj );
        registerAllCommands( parser.parse() ); // Parse an add all commands.
        
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
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Attempting to deregister command " + getCommandString( command ) + " from \"" +
                    getQualifiedName() + "\"." );
        }
        if ( commands.get( command.getName() ) != command ) {
            if ( LOG.isInfoEnabled() ) {
                LOG.error( "Failed to deregister command \"" + command.getName() + "\"." );
            }
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
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Deregistered command \"" + command.getName() + "\"." );
        }
        setLastChanged( System.currentTimeMillis() );
        return true;
        
    }
    
    /**
     * Creates a string that describes the essential information of a given command.
     * Includes name, prefix, aliases, description, and usage.
     *
     * @param command The command to be described.
     * @return A string that describes the main info of the command.
     */
    private static String getCommandString( ICommand command ) {
        
        StringBuilder builder = new StringBuilder();
        builder.append( '"' );
        builder.append( command.getName() );
        builder.append( "\"::(" );
        builder.append( ( command.getPrefix() != null ) ? command.getPrefix() : "" );
        builder.append( ')' );
        builder.append( command.getAliases() );
        builder.append( "::<\"" );
        builder.append( command.getDescription() );
        builder.append( "\">::<\"" );
        builder.append( command.getUsage() );
        builder.append( "\">" );
        return builder.toString();
        
    }
    
    /**
     * Deregisters all the commands in the registry.
     */
    public void clear() {
        
        LOG.info( "Clearing all commands in registry {}.", getQualifiedName() );
        commands.clear();
        withPrefix.clear();
        noPrefix.clear();
        
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
    public NavigableSet<ICommand> getRegisteredCommands() {
        
        NavigableSet<ICommand> commands = new TreeSet<>( ( c1, c2 ) -> {
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
     * (eg searches recursively). Placeholder subregistries are also searched.
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
        for ( CommandRegistry placeholder : placeholders.values() ) { // Check each placeholder.
            
            command = placeholder.getCommand( name );
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
    public NavigableSet<ICommand> getCommands() {
        
        NavigableSet<ICommand> commands = getRegisteredCommands();
        for ( CommandRegistry subRegistry : getSubRegistries() ) {
            // Add commands from subregistries.
            commands.addAll( subRegistry.getCommands() );
            
        }
        return commands;
        
    }
    
    /**
     * Retrieves the command, in this registry or one of its subregistries (recursively),
     * whose signature matches the signature given.
     *
     * @param signature Signature to be matched.
     * @param enableLogging Whether log messages should be enabled. Set this to false when doing
     *                      multiple queries programmatically to avoid even more spam at the trace level.
     * @return The command with the given signature, or null if none found in this registry or
     *         one of its subregistries.
     */
    public ICommand parseCommand( String signature, boolean enableLogging ) {
        
        if ( enableLogging ) {
            LOG.trace( "Parsing \"{}\" in registry \"{}\".", signature, getQualifiedName() );
        }
        ICommand command = null;
        
        /* Search this registry */
        PriorityQueue<ICommand> commandQueue = withPrefix.get( signature );
        if ( commandQueue != null ) { // Found a queue of commands with the given signature.
            command = commandQueue.peek(); // Get the first one.
        }
        String registryPrefix = getEffectivePrefix();
        if ( signature.startsWith( registryPrefix ) ) { // Command has the same prefix as the registry.
            // Remove the prefix and check the commands with no specified prefix.
            commandQueue = noPrefix.get( signature.substring( registryPrefix.length() ) );
            if ( commandQueue != null ) { // Found commands with same alias.
                ICommand candidate = commandQueue.peek(); // Get the first one.
                if ( ( candidate != null ) && ( ( command == null ) || ( candidate.compareTo( command ) < 0 ) ) ) {
                    command = candidate; // Keeps it if it has higher precedence than the current command.
                }
            }
        }
        if ( ( command != null ) && !command.isOverrideable() ) {
            if ( enableLogging ) {
                LOG.trace( "Registry \"{}\" found: \"{}\" (not overrideable).", getQualifiedName(),
                        command.getName() );
            }
            return command; // Found a command and it can't be overriden.
        }
        
        /* Check if a subregistry has the command */
        ICommand subCommand = null;
        for ( CommandRegistry subRegistry : getSubRegistries() ) {

            ICommand candidate = subRegistry.parseCommand( signature, enableLogging );
            if ( ( candidate != null ) && ( ( subCommand == null ) || ( candidate.compareTo( subCommand ) < 0 ) ) ) {
                subCommand = candidate; // Keeps it if it has higher precedence than the current command.
            }
            
        }
        if ( subCommand != null ) {
            command = subCommand; // A command from a subregistry always has higher precedence than
        }                         // the current registry.
        
        if ( enableLogging && LOG.isTraceEnabled() ) {
            LOG.trace( "Registry \"{}\" found: {}.", getQualifiedName(),
                    ( command == null ) ? null : ( "\"" + command.getName() + "\"" ) );
        }
        return command; // Returns what was found in this registry. Will be null if not found in this
                        // registry.        
    }
    
    /**
     * Retrieves the command, in this registry or one of its subregistries (recursively),
     * whose signature matches the signature given.
     * <p>
     * Logging is enabled through this method.
     *
     * @param signature Signature to be matched.
     * @return The command with the given signature, or null if none found in this registry or
     *         one of its subregistries.
     * @see #parseCommand(String, boolean)
     */
    public ICommand parseCommand( String signature ) {
        
        return parseCommand( signature, true );
        
    }
    
    /**
     * Sets the last time when this registry or one of its subregistries was changed.
     * <p>
     * Automatically sets the lastChanged value on the registry it is registered to, if there is
     * one.
     *
     * @param lastChanged The time, in milliseconds from epoch, that this registry or one
     *                    of its subregistries had a change.
     * @see #getLastChanged()
     */
    protected void setLastChanged( long lastChanged ) {
        
        this.lastChanged = lastChanged;
        if ( getRegistry() != null ) {
            getRegistry().setLastChanged( lastChanged );
        }
        
    }
    
    /**
     * Gets the last time when this registry or one of its subregistries was changed.
     * <p>
     * Changes counted by this include:
     * <ul>
     *   <li>Registering or de-registering a command;</li>
     *   <li>Registering or de-registering a subregistry.</li>
     *   <li>Changing the prefix of the subregistry.</li>
     * </ul>
     *
     * @return The time of the last change, in milliseconds from epoch.
     * @see System#currentTimeMillis()
     */
    public long getLastChanged() {
        
        return lastChanged;
        
    }
    
}
