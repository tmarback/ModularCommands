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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.Executor;
import com.github.thiagotgm.modular_commands.api.FailureHandler;
import com.github.thiagotgm.modular_commands.api.FailureReason;
import com.github.thiagotgm.modular_commands.api.ICommand;

import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Default implementation of the ICommand interface.<br>
 * All the settings are specified on construction, and they are permanent.
 * <p>
 * May optionally allow adding or removing subcommands after construction (specified through
 * an argument to the constructor).<br>
 * Also may be specified as essential or non-essential in the constructor.
 * <p>
 * The set returned by {@link #getAliases()} is immutable, so attempting to modify them throws
 * an exception.<br>
 * The set returned by {@link #getSubCommands()} is the same if, on construction,
 * it was specified that the subcommand set cannot be altered (the {@link #addSubCommand(ICommand)}
 * and {@link #removeSubCommand(ICommand)} methods will also throw exceptions). However, if on
 * construction it was specified that the subcommand set <i>can</i> be altered, the set returned
 * will be modifiable (and synchronized), and the add and remove methods can be used. Using the add
 * and remove methods is strongly recommended over altering the set directly.<br>
 * The EnumSets returned by {@link #getRequiredPermissions()} and
 * {@link #getRequiredGuildPermissions()}, on the other hand, return copies of the permission
 * sets, so they can always be modified, with any modifications not reflecting on the internal
 * permission sets.<br>
 * This way, subsequent calls to these methods are guaranteed to <b>always</b> return the
 * exact same sets, with the exception of the subcommands set if modifications to it are allowed.
 *
 * @version 1.0.0
 * @author ThiagoTGM
 * @since 2017-07-17
 */
public class Command implements ICommand {
    
    private volatile boolean enabled;
    private final boolean essential;
    private final String prefix;
    private volatile CommandRegistry registry;
    private final String name;
    private final SortedSet<String> aliases;
    private final boolean subCommand;
    private final String description;
    private final String usage;
    private final Executor commandOperation;
    private final long onSuccessDelay;
    private final Executor onSuccessOperation;
    private final FailureHandler onFailureOperation;
    private final boolean replyPrivately;
    private final boolean ignorePublic;
    private final boolean ignorePrivate;
    private final boolean ignoreBots;
    private final boolean deleteCommand;
    private final boolean requiresOwner;
    private final boolean NSFW;
    private final boolean overrideable;
    private final boolean executeParent;
    private final boolean requiresParentPermissions;
    private final EnumSet<Permissions> requiredPermissions;
    private final EnumSet<Permissions> requiredGuildPermissions;
    private final SortedSet<ICommand> subCommands;
    private final boolean canModifySubCommands;
    private final int priority;

    /**
     * Constructs a new Command with the given settings.
     * 
     * @param essential Whether the command is {@link #isEssential() essential}
     *                  (essential commands cannot be disabled).
     * @param prefix The prefix used to call the command. If <b>null</b>, the command will use the
     *               registry's prefix.
     * @param name The name of the command.
     * @param aliases The aliases used to call the command.
     * @param subCommand Whether the command is a subcommand.
     * @param description The description of this command.
     * @param usage An usage example of this command.
     * @param commandOperation The operation that should be executed when the command is executed.
     * @param onSuccessDelay The delay between a successful execution and the execution of the
     *                       onSuccess operation.
     * @param onSuccessOperation The operation to be executed after a successful execution of the command.
     * @param onFailureOperation The operation to be executed if the command could not be
     *                           successfully executed.
     * @param replyPrivately Whether the command should always send a reply to the caller on a private
     *                       channel instead of the channel that the command came from.
     * @param ignorePublic Whether the command should ignore calls made from public channels.
     * @param ignorePrivate Whether the command should ignore calls made from private channels.
     * @param ignoreBots Whether the command should ignore calls made by bot users.
     * @param deleteCommand Whether the message that deleted the command should be deleted after a
     *                      successful execution.
     * @param requiresOwner Whether only the owner of the bot account can call the command.
     * @param NSFW Whether the command can only be called from a NSFW-marked channel.
     * @param overrideable Whether the command can be overriden by a command in a subregistry.
     * @param executeParent Whether the parent command of the command should be executed before the
     *                      command is executed.
     * @param requiresParentPermissions Whether the permission requirements of the command's parent
     *                                  command also need to be satisfied to execute the command.
     * @param requiredPermissions The <i>channel-overriden</i> permissions that a user needs to
     *                            have to call the command.
     * @param requiredGuildPermissions The <i>server-wide</i> permissions that a user needs to
     *                                 have to call the command.
     * @param subCommands The subcommands of the command.
     * @param canModifySubCommands Whether the subcommand set is allowed to be modified after
     *                             construction.
     * @param priority The priority of the command.
     * @throws NullPointerException if any of the non-primitive arguments is null.
     * @throws IllegalArgumentException if one of these cases is true:
     *         <ul>
     *           <li>The command is a subcommand, and it specifies a prefix (prefix is non-null)
     *               (main command-only property);</li>
     *           <li>The name is an empty string;</li>
     *           <li>No alias was specified;</li>
     *           <li>The empty string or <b>null</b> was included as an alias;</li>
     *           <li><b>null</b> was included as a subcommand;</li>
     *           <li>There are subcommands with the same name.</li>
     *           <li>onSuccessDelay is a negative value.</li>
     *         </ul>
     */
    public Command( boolean essential,
                    String prefix,
                    String name,
                    Collection<String> aliases,
                    boolean subCommand,
                    String description,
                    String usage,
                    Executor commandOperation,
                    long onSuccessDelay,
                    Executor onSuccessOperation,
                    FailureHandler onFailureOperation,
                    boolean replyPrivately,
                    boolean ignorePublic,
                    boolean ignorePrivate,
                    boolean ignoreBots,
                    boolean deleteCommand,
                    boolean requiresOwner,
                    boolean NSFW,
                    boolean overrideable,
                    boolean executeParent,
                    boolean requiresParentPermissions,
                    EnumSet<Permissions> requiredPermissions,
                    EnumSet<Permissions> requiredGuildPermissions,
                    Collection<ICommand> subCommands,
                    boolean canModifySubCommands,
                    int priority )
                            throws NullPointerException, IllegalArgumentException {
        
        if ( ( name == null ) || ( aliases == null ) || ( description == null ) || ( usage == null ) ||
             ( commandOperation == null ) || ( onSuccessOperation == null ) || ( onFailureOperation == null ) ||
             ( requiredPermissions == null ) || ( requiredGuildPermissions == null ) ||
             ( subCommands == null ) ) {
            throw new NullPointerException( "No argument other than prefix can be null." );
        }
        
        if ( subCommand && ( prefix != null ) ) {
            throw new IllegalArgumentException( "Subcommand cannot specify a prefix." );
        }
        
        if ( name.equals( "" ) ) {
            throw new IllegalArgumentException( "The name cannot be an empty string." );
        }
        
        if ( aliases.isEmpty() ) {
            throw new IllegalArgumentException( "At least one alias must be specified." );
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
        
        try {
            if ( subCommands.contains( null ) ) {
                throw new IllegalArgumentException( "null cannot be a subcommand." );
            }
        } catch ( NullPointerException e ) {
            // Collection does not allow null in the first place.
        }
        Set<String> usedNames = new HashSet<>(); // Keeps track of already used subcommand names.
        for ( ICommand command : subCommands ) { // Check each subcommand.
            
            if ( usedNames.contains( command.getName() ) ) { // Repeated name found.
                throw new IllegalArgumentException( "Two subcommands cannot have the same name." );
            }
            usedNames.add( command.getName() ); // Name is not repeated. Add it to the list.
            
        }
        
        if ( onSuccessDelay < 0 ) {
            throw new IllegalArgumentException( "onSuccess delay cannot be a negative value." );
        }
        
        this.enabled = true;
        this.essential = essential;
        this.prefix = prefix;
        this.name = name;
        this.aliases = Collections.unmodifiableSortedSet( new TreeSet<>( aliases ) );
        this.subCommand = subCommand;
        this.description = description;
        this.usage = usage;
        this.commandOperation = commandOperation;
        this.onSuccessDelay = onSuccessDelay;
        this.onSuccessOperation = onSuccessOperation;
        this.onFailureOperation = onFailureOperation;
        this.replyPrivately = replyPrivately;
        this.ignorePublic = ignorePublic;
        this.ignorePrivate = ignorePrivate;
        this.ignoreBots = ignoreBots;
        this.deleteCommand = deleteCommand;
        this.requiresOwner = requiresOwner;
        this.NSFW = NSFW;
        this.overrideable = overrideable;
        this.executeParent = executeParent;
        this.requiresParentPermissions = requiresParentPermissions;
        this.requiredPermissions = EnumSet.copyOf( requiredPermissions );
        this.requiredGuildPermissions = EnumSet.copyOf( requiredGuildPermissions );
        SortedSet<ICommand> subCommandSet = new TreeSet<>( subCommands );
        if ( canModifySubCommands ) {
            this.subCommands = Collections.synchronizedSortedSet( subCommandSet );
        } else {
            this.subCommands = Collections.unmodifiableSortedSet( subCommandSet );
        }
        this.canModifySubCommands = canModifySubCommands;
        this.priority = priority;
        
    }
    
    /**
     * Constructs a new Command with the same properties as the given Command.
     * <p>
     * OBS: The same Executer and FailureHandler instances are used for execute(), onSuccess(),
     * and onFailure().
     *
     * @param c The Command to copy the properties of.
     */
    public Command( Command c ) {
        
        this.enabled = true;
        this.essential = c.essential;
        this.prefix = c.prefix;
        this.name = c.name;
        this.aliases = Collections.unmodifiableSortedSet( new TreeSet<>( c.aliases ) );
        this.subCommand = c.subCommand;
        this.description = c.description;
        this.usage = c.usage;
        this.commandOperation = c.commandOperation;
        this.onSuccessDelay = c.onSuccessDelay;
        this.onSuccessOperation = c.onSuccessOperation;
        this.onFailureOperation = c.onFailureOperation;
        this.replyPrivately = c.replyPrivately;
        this.ignorePublic = c.ignorePublic;
        this.ignorePrivate = c.ignorePrivate;
        this.ignoreBots = c.ignoreBots;
        this.deleteCommand = c.deleteCommand;
        this.requiresOwner = c.requiresOwner;
        this.NSFW = c.NSFW;
        this.overrideable = c.overrideable;
        this.executeParent = c.executeParent;
        this.requiresParentPermissions = c.requiresParentPermissions;
        this.requiredPermissions = EnumSet.copyOf( c.requiredPermissions );
        this.requiredGuildPermissions = EnumSet.copyOf( c.requiredGuildPermissions );
        SortedSet<ICommand> subCommandSet = new TreeSet<>( c.subCommands );
        if ( c.canModifySubCommands ) {
            this.subCommands = Collections.synchronizedSortedSet( subCommandSet );
        } else {
            this.subCommands = Collections.unmodifiableSortedSet( subCommandSet );
        }
        this.canModifySubCommands = c.canModifySubCommands;
        this.priority = c.priority;
        
    }

    @Override
    public boolean isEnabled() {

        return enabled;
        
    }

    @Override
    public void setEnabled( boolean enabled ) throws IllegalStateException {

        if ( essential ) {
            throw new IllegalStateException( "Cannot disable an essential Command." );
        }
        this.enabled = enabled;
        
    }

    @Override
    public boolean isEssential() {

        return essential;
        
    }

    @Override
    public String getPrefix() {

        return prefix;
        
    }

    @Override
    public CommandRegistry getRegistry() {

        return registry;
        
    }

    @Override
    public void setRegistry( CommandRegistry registry ) {

        this.registry = registry;
        
    }

    @Override
    public String getName() {

        return name;
        
    }

    @Override
    public SortedSet<String> getAliases() {

        return aliases;
        
    }

    @Override
    public boolean isSubCommand() {

        return subCommand;
        
    }

    @Override
    public String getDescription() {

        return description;
        
    }

    @Override
    public String getUsage() {

        return usage;
        
    }

    @Override
    public void execute( CommandContext context )
            throws RateLimitException, MissingPermissionsException, DiscordException {

        commandOperation.accept( context );
        
    }

    @Override
    public long getOnSuccessDelay() {

        return onSuccessDelay;
        
    }

    @Override
    public void onSuccess( CommandContext context )
            throws RateLimitException, MissingPermissionsException, DiscordException {

        onSuccessOperation.accept( context );
        
    }

    @Override
    public void onFailure( CommandContext context, FailureReason reason )
            throws RateLimitException, MissingPermissionsException, DiscordException {

        onFailureOperation.accept( context, reason );
        
    }

    @Override
    public boolean replyPrivately() {

        return replyPrivately;
        
    }

    @Override
    public boolean ignorePublic() {

        return ignorePublic;
        
    }

    @Override
    public boolean ignorePrivate() {

        return ignorePrivate;
        
    }

    @Override
    public boolean ignoreBots() {

        return ignoreBots;
        
    }

    @Override
    public boolean deleteCommand() {

        return deleteCommand;
        
    }

    @Override
    public boolean requiresOwner() {

        return requiresOwner;
        
    }

    @Override
    public boolean isNSFW() {

        return NSFW;
        
    }

    @Override
    public boolean isOverrideable() {

        return overrideable;
        
    }

    @Override
    public boolean executeParent() {

        return executeParent;
        
    }

    @Override
    public boolean requiresParentPermissions() {

        return requiresParentPermissions;
        
    }

    @Override
    public EnumSet<Permissions> getRequiredPermissions() {

        return EnumSet.copyOf( requiredPermissions );
        
    }

    @Override
    public EnumSet<Permissions> getRequiredGuildPermissions() {

        return EnumSet.copyOf( requiredGuildPermissions );
        
    }

    @Override
    public synchronized SortedSet<ICommand> getSubCommands() {

        return subCommands;
        
    }

    @Override
    public synchronized boolean addSubCommand( ICommand subCommand ) 
            throws UnsupportedOperationException, IllegalArgumentException {

        if ( !canModifySubCommands ) {
            throw new UnsupportedOperationException( "Command does not allow altering subcommand set." );
        }
        if ( !subCommand.isSubCommand() ) {
            throw new IllegalArgumentException( "Argument is not a subcommand." );
        }
        if ( getSubCommandByName( subCommand.getName() ) != null ) {
            return false;
        }
        return subCommands.add( subCommand );
        
    }

    @Override
    public synchronized boolean removeSubCommand( ICommand subCommand )
            throws UnsupportedOperationException, IllegalArgumentException {

        if ( !canModifySubCommands ) {
            throw new UnsupportedOperationException( "Command does not allow altering subcommand set." );
        }
        if ( !subCommand.isSubCommand() ) {
            throw new IllegalArgumentException( "Argument is not a subcommand." );
        }
        return subCommands.remove( subCommand );
        
    }

    @Override
    public int getPriority() {

        return priority;
        
    }

    @Override
    public int hashCode() {

        return name.hashCode();
        
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append( '"' );
        builder.append( name );
        builder.append( "\"::" );
        if ( !subCommand ) {
            builder.append( '(' );
            builder.append( ( prefix != null ) ? prefix : "" );
            builder.append( ')' );
        }
        builder.append( aliases );
        builder.append( "::<\"" );
        builder.append( description );
        builder.append( "\">::<\"" );
        builder.append( usage );
        builder.append( "\">" );
        return builder.toString();
        
    }

}
