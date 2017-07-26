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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.FailureReason;
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
            .onExecute( ( context ) -> { return true; } ).build();
    
    private final String name;
    
    private boolean essential;
    private String prefix;
    private Collection<String> aliases;
    private boolean subCommand;
    private String description;
    private String usage;
    private Predicate<CommandContext> commandOperation;
    private long onSuccessDelay;
    private Consumer<CommandContext> onSuccessOperation;
    private BiConsumer<CommandContext, FailureReason> onFailureOperation;
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
     * @see ICommand#getName()
     */
    public CommandBuilder( String name ) throws NullPointerException, IllegalArgumentException {
        
        if ( name == null ) {
            throw new NullPointerException( "Command name cannot be null." );
        }
        
        if ( name.isEmpty() ) {
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
     * The builder's initial {@link ICommand#execute(CommandContext) execute},
     * {@link ICommand#onSuccess(CommandContext) onSuccess}, and
     * {@link ICommand#onFailure(CommandContext, FailureReason) onFailure} operations will be
     * set to the same {@link Predicate}, {@link Consumer}, and {@link BiConsumer} instances
     * as the given builder.
     *
     * @param name The name of the command to be built.
     * @param cb The builder to copy property values from.
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is the empty string.
     * @see ICommand#getName()
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
     * @see ICommand#getName()
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
        this.commandOperation = ( context ) -> { return c.execute( context ); };
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
    
    /**
     * Sets whether the command being built is essential.
     *
     * @param essential Whether the command is essential.
     * @return This builder.
     * @see ICommand#isEssential()
     */
    public CommandBuilder essential( boolean essential ) {
        
        this.essential = essential;
        return this;
        
    }
    
    /**
     * Sets the prefix for the command being built.
     * <p>
     * Can only be used if the command being built is a main command.
     *
     * @param prefix The prefix to be used.
     * @return This builder.
     * @throws IllegalStateException if the command being built is a subcommand.
     * @see ICommand#getPrefix()
     */
    public CommandBuilder withPrefix( String prefix ) throws IllegalStateException {
        
        if ( subCommand ) {
            throw new IllegalStateException( "A subcommand cannot specify a prefix." );
        }
        
        this.prefix = prefix;
        return this;
        
    }
    
    /**
     * Sets the aliases that the built command should have.
     *
     * @param aliases The aliases for the built command.
     * @return This builder.
     * @throws NullPointerException if the given aliases collection is null.
     * @throws IllegalArgumentException if null or the empty string was included as an alias.
     * @see ICommand#getAliases()
     */
    public CommandBuilder withAliases( Collection<String> aliases )
            throws NullPointerException, IllegalArgumentException {
        
        if ( aliases == null ) {
            throw new NullPointerException( "Alias collection cannot be null." );
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
     * If it is set as a subcommand, the prefix is reset to <b>null</b>.
     *
     * @param subCommand Whether the command being built is a subcommand.
     * @return This builder.
     * @see ICommand#isSubCommand()
     */
    public CommandBuilder subCommand( boolean subCommand ) {
        
        this.subCommand = subCommand;
        if ( subCommand ) {
            this.prefix = null;
        }
        return this;
        
    }
    
    /**
     * Sets the description of the command being built.
     *
     * @param description The description of the command.
     * @return This builder.
     * @throws NullPointerException If the description given is null.
     * @see ICommand#getDescription()
     */
    public CommandBuilder withDescription( String description ) throws NullPointerException {
        
        if ( description == null ) {
            throw new NullPointerException( "Description cannot be null." );
        }
        
        this.description = description;
        return this;
        
    }
    
    /**
     * Sets the usage of the command being built.
     *
     * @param usage The usage of the command.
     * @return This builder.
     * @throws NullPointerException If the usage given is null.
     * @see ICommand#getUsage()
     */
    public CommandBuilder withUsage( String usage ) throws NullPointerException {
        
        if ( usage == null ) {
            throw new NullPointerException( "Usage cannot be null." );
        }
        
        this.usage = usage;
        return this;
        
    }
    
    /**
     * Sets the operation that should be performed when the command is executed.
     *
     * @param operation The operation to be performed.
     * @return This builder.
     * @throws NullPointerException if the given operation is null.
     * @see ICommand#execute(CommandContext)
     */
    public CommandBuilder onExecute( Predicate<CommandContext> operation ) throws NullPointerException {
        
        if ( operation == null ) {
            throw new NullPointerException( "The command operation cannot be null." );
        }
        
        this.commandOperation = operation;
        return this;
        
    }
    
    /**
     * Sets the time that should be waited between a successful execution of
     * the command and the call to the onSuccess operation.
     *
     * @param delay The time delay, in milliseconds.
     * @return This builder.
     * @throws IllegalArgumentException if the value given is negative.
     * @see ICommand#getOnSuccessDelay()
     */
    public CommandBuilder withOnSuccessDelay( long delay ) throws IllegalArgumentException {
        
        if ( delay < 0 ) {
            throw new IllegalArgumentException( "onSuccess delay cannot be a negative value." );
        }
        
        this.onSuccessDelay = delay;
        return this;
        
    }
    
    /**
     * Convenience method for setting the onSuccess delay in an arbitrary time unit.
     *
     * @param delay The time delay.
     * @param unit The unit that the delay is expressed in.
     * @return This builder.
     * @throws IllegalArgumentException if the value given is negative.
     * @see #withOnSuccessDelay(long)
     */
    public CommandBuilder withOnSuccessDelay( long delay, TimeUnit unit )
            throws IllegalArgumentException {
        
        return withOnSuccessDelay( TimeUnit.MILLISECONDS.convert( delay, unit ) );
        
    }
    
    /**
     * Sets the operation that should be performed after a successful execution of the command
     * and after the onSuccess time delay.
     *
     * @param operation The operation to be performed.
     * @return This builder.
     * @throws NullPointerException if the given operation is null.
     * @see ICommand#onSuccess(CommandContext)
     */
    public CommandBuilder onSuccess( Consumer<CommandContext> operation ) throws NullPointerException {
        
        if ( operation == null ) {
            throw new NullPointerException( "The onSuccess operation cannot be null." );
        }
        
        this.onSuccessOperation = operation;
        return this;
        
    }
    
    /**
     * Sets the operation that should be performed after a failed execution of the
     * command (for one of the reasons specified on
     * {@link com.github.thiagotgm.modular_commands.api.FailureReason}).
     *
     * @param operation The operation to be performed.
     * @return This builder.
     * @throws NullPointerException if the given operation is null.
     * @see ICommand#onFailure(CommandContext, FailureReason)
     */
    public CommandBuilder onFailure( BiConsumer<CommandContext, FailureReason> operation )
            throws NullPointerException {
        
        if ( operation == null ) {
            throw new NullPointerException( "The onFailure operation cannot be null." );
        }
        
        this.onFailureOperation = operation;
        return this;
        
    }
    
    /**
     * Sets whether the command should always reply to the caller on a private channel.
     *
     * @param replyPrivately Whether the command should always reply privately.
     * @return This builder.
     * @see ICommand#replyPrivately()
     */
    public CommandBuilder replyPrivately( boolean replyPrivately ) {
        
        this.replyPrivately = replyPrivately;
        return this;
        
    }
    
    /**
     * Sets whether the command should ignore calls to it from public channels.
     *
     * @param ignorePublic Whether the command should ignore public calls.
     * @return This builder.
     * @see ICommand#ignorePublic()
     */
    public CommandBuilder ignorePublic( boolean ignorePublic ) {
        
        this.ignorePublic = ignorePublic;
        return this;
        
    }
    
    /**
     * Sets whether the command should ignore calls to it from private channels.
     *
     * @param ignorePrivate Whether the command should ignore private calls.
     * @return This builder.
     * @see ICommand#ignorePrivate()
     */
    public CommandBuilder ignorePrivate( boolean ignorePrivate ) {
        
        this.ignorePrivate = ignorePrivate;
        return this;
        
    }
    
    /**
     * Sets whether the command should ignore calls to it made by bot users.
     *
     * @param ignoreBots Whether the command should ignore calls by bots.
     * @return This builder.
     * @see ICommand#ignoreBots()
     */
    public CommandBuilder ignoreBots( boolean ignoreBots ) {
        
        this.ignoreBots = ignoreBots;
        return this;
        
    }
    
    /**
     * Sets whether the command should delete the message that called it after a successful execution.
     *
     * @param deleteCommand Whether the calling message should be deleted.
     * @return This builder.
     * @see ICommand#deleteCommand()
     */
    public CommandBuilder deleteCommand( boolean deleteCommand ) {
        
        this.deleteCommand = deleteCommand;
        return this;
        
    }
    
    /**
     * Sets whether the command can only be called by the owner of the bot account.
     *
     * @param requiresOwner Whether the command can only be called by the bot owner.
     * @return This builder.
     * @see ICommand#requiresOwner()
     */
    public CommandBuilder requiresOwner( boolean requiresOwner ) {
    
        this.requiresOwner = requiresOwner;
        return this;
        
    }
    
    /**
     * Sets whether the command can only be called from a channel marked as NSFW.
     *
     * @param NSFW Whether the command can only be called on a NSFW-marked channel.
     * @return This builder.
     * @see ICommand#isNSFW()
     */
    public CommandBuilder NSFW( boolean NSFW ) {
        
        this.NSFW = NSFW;
        return this;
        
    }
    
    /**
     * Sets whether the command can be overriden by a command in a subregistry of the registry it
     * is registered to.
     *
     * @param overrideable Whether the command can be overriden.
     * @return This builder.
     * @see ICommand#isOverrideable()
     */
    public CommandBuilder overrideable( boolean overrideable ) {
        
        this.overrideable = overrideable;
        return this;
        
    }
    
    /**
     * Sets whether the parent of the command should be executed before it.
     *
     * @param executeParent Whether the parent should be executed.
     * @return This builder.
     * @see ICommand#executeParent()
     */
    public CommandBuilder executeParent( boolean executeParent ) {
        
        this.executeParent = executeParent;
        return this;
        
    }
    
    /**
     * Sets whether the command requires that the permission requirements for its parent command
     * be also satisfied in addition to its own.
     *
     * @param requiresParentPermissions Whether the parent's permissions are also required.
     * @return This builder.
     * @see ICommand#requiresParentPermissions()
     */
    public CommandBuilder requiresParentPermissions( boolean requiresParentPermissions ) {
        
        this.requiresParentPermissions = requiresParentPermissions;
        return this;
        
    }
    
    /**
     * Sets the (channel-overriden) permissions that a user must have on the channel in order
     * to call the command.
     *
     * @param permissions The required permissions.
     * @return This builder.
     * @throws NullPointerException if the permission set given is null.
     * @see ICommand#getRequiredPermissions()
     */
    public CommandBuilder withRequiredPermissions( EnumSet<Permissions> permissions )
            throws NullPointerException {
        
        if ( permissions == null ) {
            throw new NullPointerException( "Required permissions set cannot be null." );
        }
        
        this.requiredPermissions = EnumSet.copyOf( permissions );
        return this;
        
    }
    
    /**
     * Sets the (guild-wide) permissions that a user must have on the guild in order
     * to call the command.
     *
     * @param permissions The required permissions.
     * @return This builder.
     * @throws NullPointerException if the permission set given is null.
     * @see ICommand#getRequiredGuildPermissions()
     */
    public CommandBuilder withRequiredGuildPermissions( EnumSet<Permissions> permissions )
            throws NullPointerException {
        
        if ( permissions == null ) {
            throw new NullPointerException( "Required guild permissions set cannot be null." );
        }
        
        this.requiredGuildPermissions = EnumSet.copyOf( permissions );
        return this;
        
    }
    
    /**
     * Sets the subcommands of the command being built.
     *
     * @param subCommands The subcommands to be used.
     * @return This builder.
     * @throws NullPointerException if the given command collection is null.
     * @throws IllegalArgumentException if there are two or more subcommands with the same name.
     * @see ICommand#getSubCommands()
     */
    public CommandBuilder withSubCommands( Collection<ICommand> subCommands )
            throws NullPointerException, IllegalArgumentException {
        
        if ( subCommands == null ) {
            throw new NullPointerException( "Subcommand collection cannot be null." );
        }
        
        Set<String> usedNames = new HashSet<>(); // Keeps tracked of already used subcommand names.
        for ( ICommand command : subCommands ) { // Check each subcommand.
            
            if ( usedNames.contains( command.getName() ) ) { // Repeated name found.
                throw new IllegalArgumentException( "Two subcommands cannot have the same name." );
            }
            usedNames.add( command.getName() ); // Name is not repeated. Add it to the list.
            
        }
        
        this.subCommands = new ArrayList<>( subCommands );
        return this;
        
    }
    
    /**
     * Sets whether the command being built should allow modifying the subcommand set after
     * being built.
     *
     * @param canModify Whether the subcommand set can be modified after building.
     * @return This builder.
     * @see Command
     */
    public CommandBuilder canModifySubCommands( boolean canModify ) {
        
        this.canModifySubCommands = canModify;
        return this;
        
    }
    
    /**
     * Sets the priority of the command being built.
     *
     * @param priority The priority of the command.
     * @return This builder.
     * @see ICommand#getPriority()
     */
    public CommandBuilder withPriority( int priority ) {
        
        this.priority = priority;
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
