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

package com.github.thiagotgm.modular_commands.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import com.github.thiagotgm.modular_commands.api.Executor;
import com.github.thiagotgm.modular_commands.api.FailureHandler;
import com.github.thiagotgm.modular_commands.api.ICommand;

import sx.blah.discord.handle.obj.Permissions;

/**
 * Builder for creating new commands with set properties.
 *
 * @version 1.0.0
 * @author ThiagoTGM
 * @since 2017-07-18
 */
public class CommandBuilder {
    
    /**
     * Subcommand that does nothing, just used to test if an ICommand can support removing subcommands.
     */
    private static final ICommand SAMPLE = new CommandBuilder( "_*^Sample-Command^*_" )
            .subCommand( true ).withAliases( new String[] { "hi" } )
            .onExecute( ( context ) -> {} ).build();
    
    private final String name;
    
    private boolean essential;
    private String prefix;
    private Collection<String> aliases;
    private boolean subCommand;
    private String description;
    private String usage;
    private Executor commandOperation;
    private int onSuccessDelay;
    private Executor onSuccessOperation;
    private FailureHandler onFailureOperation;
    private boolean replyPrivately;
    private boolean ignorePublic;
    private boolean ignorePrivate;
    private boolean ignoreBots;
    private boolean deleteCommand;
    private boolean requiresOwner;
    private boolean NSFW;
    private boolean overrideable;
    private boolean executeParent;
    private boolean requiresParentPermissions;
    private EnumSet<Permissions> requiredPermissions;
    private EnumSet<Permissions> requiredGuildPermissions;
    private Collection<ICommand> subCommands;
    private boolean canModifySubCommands;
    private int priority;

    /**
     * Constructs a new builder with default values for all properties (that have default values)
     * and the given command name.
     * <p>
     * By default:
     * <ul>
     *   <li>Is a main command;</li>
     *   <li>Is not essential;</li>
     *   <li>Supports modifying the subcommand set;</li>
     *   <li>The onSuccess and onFailure operations do nothing.</li>
     * </ul>
     * 
     * @param name The name of the command to be built.
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is the empty string.
     */
    public CommandBuilder( String name ) throws NullPointerException, IllegalArgumentException {
        
        if ( name == null ) {
            throw new NullPointerException( "Command name cannot be null." );
        }
        
        if ( name.equals( "" ) ) {
            throw new IllegalArgumentException( "The name cannot be an empty string." );
        }
        
        this.name = name;
        
        /* Set default properties */
        this.essential = false;
        this.prefix = null;
        this.aliases = null;
        this.subCommand = false;
        this.description = "";
        this.usage = "";
        this.commandOperation = null;
        this.onSuccessDelay = 0;
        this.onSuccessOperation = ( context ) -> {};
        this.onFailureOperation = ( context, reason ) -> {};
        this.replyPrivately = false;
        this.ignorePublic = false;
        this.ignorePrivate = false;
        this.ignoreBots = true;
        this.deleteCommand = false;
        this.requiresOwner = false;
        this.NSFW = false;
        this.overrideable = true;
        this.executeParent = false;
        this.requiresParentPermissions = true;
        this.requiredPermissions = EnumSet.noneOf( Permissions.class );
        this.requiredGuildPermissions = EnumSet.noneOf( Permissions.class );
        this.subCommands = new ArrayList<>();
        this.canModifySubCommands = true;
        this.priority = 0;
        
    }
    
    /**
     * Constructs a new builder with the same set values as the given builder and
     * the given command name.
     * <p>
     * The builder's initial
     * {@link ICommand#execute(com.github.thiagotgm.modular_commands.api.CommandContext) execute},
     * {@link ICommand#onSuccess(com.github.thiagotgm.modular_commands.api.CommandContext) onSuccess}, and
     * {@link ICommand#onFailure(com.github.thiagotgm.modular_commands.api.CommandContext,
     *  com.github.thiagotgm.modular_commands.api.FailureReason) onFailure} operations will be
     * set to the same {@link Executor} and {@link FailureHandler} instances as the given builder.
     *
     * @param name The name of the command to be built.
     * @param cb The builder to copy property values from.
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is the empty string.
     */
    public CommandBuilder( String name, CommandBuilder cb )
            throws NullPointerException, IllegalArgumentException {
        
        this( name ); // Set name.
        
        /* Copy properties */
        this.essential = cb.essential;
        this.prefix = cb.prefix;
        this.aliases = new ArrayList<>( cb.aliases );
        this.subCommand = cb.subCommand;
        this.description = cb.description;
        this.usage = cb.usage;
        this.commandOperation = cb.commandOperation;
        this.onSuccessDelay = cb.onSuccessDelay;
        this.onSuccessOperation = cb.onSuccessOperation;
        this.onFailureOperation = cb.onFailureOperation;
        this.replyPrivately = cb.replyPrivately;
        this.ignorePublic = cb.ignorePublic;
        this.ignorePrivate = cb.ignorePrivate;
        this.ignoreBots = cb.ignoreBots;
        this.deleteCommand = cb.deleteCommand;
        this.requiresOwner = cb.requiresOwner;
        this.NSFW = cb.NSFW;
        this.overrideable = cb.overrideable;
        this.executeParent = cb.executeParent;
        this.requiresParentPermissions = cb.requiresParentPermissions;
        this.requiredPermissions = EnumSet.copyOf( cb.requiredPermissions );
        this.requiredGuildPermissions = EnumSet.copyOf( cb.requiredGuildPermissions );
        this.subCommands = new ArrayList<>( cb.subCommands );
        this.canModifySubCommands = cb.canModifySubCommands;
        this.priority = cb.priority;
        
    }
    
    /**
     * Constructs a new builder with the same set values as the given command and
     * the given command name.
     * <p>
     * The builder's initial
     * {@link ICommand#execute(com.github.thiagotgm.modular_commands.api.CommandContext) execute},
     * {@link ICommand#onSuccess(com.github.thiagotgm.modular_commands.api.CommandContext) onSuccess}, and
     * {@link ICommand#onFailure(com.github.thiagotgm.modular_commands.api.CommandContext,
     *  com.github.thiagotgm.modular_commands.api.FailureReason) onFailure} operations will be
     * set to just call the equivalent operations of the given command.
     * <p>
     * If the given command supports modifying the subcommand set (tested by calling
     * {@link ICommand#removeSubCommand(ICommand)} with a sample subcommand and checking if it
     * threw an {@link UnsupportedOperationException}), the builder is initially set to build
     * a command that also allows modifying the subcommand set. Else it is set to not allow it.
     *
     * @param name The name of the command to be built.
     * @param c The command to copy property values from.
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is the empty string.
     */
    public CommandBuilder( String name, ICommand c )
            throws NullPointerException, IllegalArgumentException {
        
        this( name ); // Set name.
        
        /* Copy properties */
        this.essential = c.isEssential();
        this.prefix = c.getPrefix();
        this.aliases = new ArrayList<>( c.getAliases() );
        this.subCommand = c.isSubCommand();
        this.description = c.getDescription();
        this.usage = c.getUsage();
        this.commandOperation = ( context ) -> { c.execute( context ); };
        this.onSuccessDelay = c.getOnSuccessDelay();
        this.onSuccessOperation = ( context ) -> { c.onSuccess( context ); };
        this.onFailureOperation = ( context, reason ) -> { c.onFailure( context, reason ); };
        this.replyPrivately = c.replyPrivately();
        this.ignorePublic = c.ignorePublic();
        this.ignorePrivate = c.ignorePrivate();
        this.ignoreBots = c.ignoreBots();
        this.deleteCommand = c.deleteCommand();
        this.requiresOwner = c.requiresOwner();
        this.NSFW = c.isNSFW();
        this.overrideable = c.isOverrideable();
        this.executeParent = c.executeParent();
        this.requiresParentPermissions = c.requiresParentPermissions();
        this.requiredPermissions = EnumSet.copyOf( c.getRequiredPermissions() );
        this.requiredGuildPermissions = EnumSet.copyOf( c.getRequiredGuildPermissions() );
        this.subCommands = new ArrayList<>( c.getSubCommands() );
        try { // Check if command can support modifying subcommand set.
            c.removeSubCommand( SAMPLE );
            this.canModifySubCommands = true; // Did not throw exception, supports it.
        } catch ( UnsupportedOperationException e ) {
            this.canModifySubCommands = false; // Threw exception, does not support it.
        }
        this.priority = c.getPriority();
        
    }
    
    // TODO essential and prefix
    
    /**
     * Sets the aliases that the built command should have.
     *
     * @param aliases The aliases for the built command.
     * @return This builder.
     * @throws NullPointerException if the given aliases collection is null.
     * @throws IllegalArgumentException if null or the empty string was included as an alias.
     */
    public CommandBuilder withAliases( Collection<String> aliases )
            throws NullPointerException, IllegalArgumentException {
        
        if ( aliases == null ) {
            throw new NullPointerException( "Aliases collection cannot be null." );
        }
        
        if ( aliases.contains( "" ) ) {
            throw new IllegalArgumentException( "The empty string cannot be an alias." );
        }
        try {
            if ( aliases.contains( null ) ) {
                throw new IllegalArgumentException( "null cannot be an alias." );
            }
        } catch ( NullPointerException e ) {
            // Collection does not allow null in the first place.
        }
        
        this.aliases = new ArrayList<>( aliases );
        return this;
        
    }
    
    /**
     * Convenience method for setting aliases through an array instead of a Collection.
     *
     * @param aliases The aliases for the built command.
     * @return This builder.
     * @throws NullPointerException if the given aliases array is null.
     * @throws IllegalArgumentException if null or the empty string was included as an alias.
     * @see #withAliases(Collection)
     */
    public CommandBuilder withAliases( String[] aliases )
            throws NullPointerException, IllegalArgumentException {
        
        if ( aliases == null ) {
            throw new NullPointerException( "Aliases array cannot be null." );
        }
        
        return withAliases( Arrays.asList( aliases ) );
        
    }
    
    /**
     * Sets whether the command to be built is a subcommand.
     * <p>
     * If it is set as a subcommand, the prefix is reset to <b>null</b> and the </i>overrideable</i>
     * property is set to false.<br>
     * If it is set as a main command, the <i>executeParent</i> and <i>requiresParentPermissions</i>
     * properties are set to false.
     *
     * @param subCommand Whether the command being built is a subcommand.
     * @return This builder.
     */
    public CommandBuilder subCommand( boolean subCommand ) {
        
        this.subCommand = subCommand;
        if ( subCommand ) {
            this.prefix = null;
            this.overrideable = false;
        } else {
            this.executeParent = false;
            this.requiresParentPermissions = false;
        }
        return this;
        
    }
    
    // TODO description and usage
    
    /**
     * Sets the operation that should be performed when the command is executed.
     *
     * @param operation The operation to be performed.
     * @return This builder.
     * @throws NullPointerException if the given operation is null.
     */
    public CommandBuilder onExecute( Executor operation ) throws NullPointerException {
        
        if ( operation == null ) {
            throw new NullPointerException( "The command operation cannot be null." );
        }
        
        this.commandOperation = operation;
        return this;
        
    }
    
    /**
     * Builds a command with the current property values.
     * <p>
     * All properties that do not specify a default value must be specified one before the
     * command can be built.
     *
     * @return The built command.
     * @throws IllegalStateException If a value was not specified for a property that does not
     *                               have a default value.
     */
    public Command build() throws IllegalStateException {
        
        if ( aliases == null ) {
            throw new IllegalStateException( "Cannot build a command without specifying an alias." );
        }
        if ( commandOperation == null ) {
            throw new IllegalStateException( "Cannot build a command without specifying an operation." );
        }
        
        return new Command( essential,
                            prefix,
                            name,
                            aliases,
                            subCommand,
                            description,
                            usage,
                            commandOperation,
                            onSuccessDelay,
                            onSuccessOperation,
                            onFailureOperation,
                            replyPrivately,
                            ignorePublic,
                            ignorePrivate,
                            ignoreBots,
                            deleteCommand,
                            requiresOwner,
                            NSFW,
                            overrideable,
                            executeParent,
                            requiresParentPermissions,
                            requiredPermissions,
                            requiredGuildPermissions,
                            subCommands,
                            canModifySubCommands,
                            priority );
        
    }

}
