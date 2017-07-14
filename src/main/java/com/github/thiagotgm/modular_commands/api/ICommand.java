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

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * A command that can be invoked in a text channel and executed by the bot.
 * <p>
 * An instance of this type should be able to be registered to only a single registry at a time.
 * <p>
 * It is recommended to override {@link Object#hashCode()} to use {@link String#hashCode() the name's hash code}.
 *
 * @version 1.1
 * @author ThiagoTGM
 * @since 2017-07-13
 */
public interface ICommand extends Disableable, Prefixed, Comparable<ICommand> {
    
    /**
     * Retrieves the name of the command.
     *
     * @return The name of the command.
     */
    abstract String getName();
    
    /**
     * Retrieves the aliases of the command.
     * <p>
     * These are used to call the command from a chat message.
     *
     * @return The aliases of the command.
     */
    abstract SortedSet<String> getAliases();
    
    /**
     * Retrieves the signatures that can be used to call the command.
     * <p>
     * The prefix used is given by {@link #getEffectivePrefix()}, so if the command does not declare a
     * specific prefix and the inherited prefix changes, the signatures change as well.
     *
     * @return The signatures of the command, in sorted order.
     * @throws IllegalStateException if called when the command is not registered to any registry.
     */
    default List<String> getSignatures() throws IllegalStateException {
        
        String prefix = getEffectivePrefix();
        List<String> signatures = new LinkedList<>();
        for ( String alias : getAliases() ) {
            
            signatures.add( prefix + alias );
            
        }
        return signatures;
        
    }
    
    /**
     * Identifies if the command is a subcommand.
     *
     * @return If the calling instance is a subcommand, returns true.<br>
     *         If it is a main command, returns false.
     */
    abstract boolean isSubCommand();
    
    /**
     * Retrieves the description of the command.
     * <p>
     * By default, returns an empty string.
     *
     * @return The description of the command.
     */
    default String getDescription() { return ""; }
    
    /**
     * Retrieves the usage of the command.
     * <p>
     * By default, returns an empty string.
     *
     * @return The usage of the command.
     */
    default String getUsage() { return ""; }
    
    /**
     * Executes the command on a given context.
     *
     * @param context The context where the command was called.
     * @throws RateLimitException if the operation failed due to rate limiting.
     * @throws MissingPermissionsException If the bot does not have the required permissions.
     * @throws DiscordException If a miscellaneous error was encountered.
     */
    abstract void execute( CommandContext context )
            throws RateLimitException, MissingPermissionsException, DiscordException;
    
    /**
     * Retrieves the time delay to be left between a successful execution and a
     * call to {@link #onSuccess(CommandContext)}.
     * <p>
     * By default, returns 0.
     *
     * @return The time delay, in milliseconds.
     */
    default int getOnSuccessDelay() { return 0; }
    
    /**
     * Executes a post-processing operation after a successful execution of the command and after
     * the time delay given by {@link #getOnSuccessDelay()}.
     * <p>
     * By default, does nothing.
     *
     * @param context Context where the command was called from.
     * @throws RateLimitException if the operation failed due to rate limiting.
     * @throws MissingPermissionsException If the bot does not have the required permissions.
     * @throws DiscordException If a miscellaneous error was encountered.
     */
    default void onSuccess( CommandContext context )
            throws RateLimitException, MissingPermissionsException, DiscordException {}
    
    /**
     * Executes a post-processing operation after a failed invocation of the command.
     * <p>
     * By default, does nothing.
     *
     * @param context Context where the command was called from.
     * @param reason Reason why the command invocation failed.
     * @throws RateLimitException if the operation failed due to rate limiting.
     * @throws MissingPermissionsException If the bot does not have the required permissions.
     * @throws DiscordException If a miscellaneous error was encountered.
     */
    default void onFailure( CommandContext context, FailureReason reason )
            throws RateLimitException, MissingPermissionsException, DiscordException {}
    
    /**
     * Retrieves whether the command reply to the caller should be done on a private channel.
     * <p>
     * By default, returns false.
     *
     * @return true if the reply to the command caller should be on a private channel.<br>
     *         false if the reply should be on the channel where the command was called from.
     * @see CommandContext#getReplyBuilder()
     */
    default boolean replyPrivately() { return false; }
    
    /**
     * Retrieves whether invocations of the command from public channels should be ignored.
     * <p>
     * By default, returns false.
     *
     * @return true if invocations from public channels should be ignored.<br>
     *         false otherwise.
     */
    default boolean ignorePublic() { return false; }
    
    /**
     * Retrieves whether invocations of the command from private channels should be ignored.
     * <p>
     * By default, returns false.
     *
     * @return true if invocations from private channels should be ignored.<br>
     *         false otherwise.
     */
    default boolean ignorePrivate() { return false; }
    
    /**
     * Retrieves whether invocations of the command from bot users should be ignored.
     * <p>
     * By default, returns true.
     *
     * @return true if invocations from bot users should be ignored.<br>
     *         false otherwise.
     */
    default boolean ignoreBots() { return true; }
    
    /**
     * Retrieves whether the message that invoked the command should be deleted after a successful execution.
     * <p>
     * By default, returns false.
     *
     * @return true if the invoking message should be deleted after a successful execution.
     *         false otherwise.
     */
    default boolean deleteCommand() { return false; }
    
    /**
     * Retrieves whether only the owner of the bot account is allowed to invoke this command.
     * <p>
     * By default, returns false.
     * 
     * @return true if only the bot owner can call this command.
     *         false otherwise.
     */
    default boolean requiresOwner() { return false; }
    
    /**
     * Retrieves whether the parent of this command should be executed before executing
     * this command.<br>
     * If this command is a main command, this should return false.
     * <p>
     * By default, returns false.
     * <p>
     * OBS: The parent's {@link #onSuccess(CommandContext) onSuccess} and
     * {@link #onFailure(CommandContext, FailureReason) onFailure} operations are not called.<br>
     * OBS 2: If the parent also specifies that its parent should be executed, it will be executed before it
     * (eg this behaviour can be chained), until reaching the main command or the first ancestor
     * that specifies its parent should not be executed.
     *
     * @return true if the parent of this command should be executed before executing this.<br>
     *         false if it should not be executed.
     */
    default boolean executeParent() { return false; }
    
    /**
     * Retrieves the <i>post-override</i> permissions that the calling user needs to have in order to be able to invoke
     * this command.
     * <p>
     * By default, returns an empty set.
     *
     * @return The required <i>post-override</i> permissions.
     */
    default EnumSet<Permissions> getRequiredPermissions() { return EnumSet.noneOf( Permissions.class ); }
    
    /**
     * Retrieves the <i>guild-wide</i> permissions that the calling user needs to have in order to be able to invoke this
     * command.
     * <p>
     * By default, returns an empty set.
     *
     * @return The required <i>guild-wide</i> permissions.
     */
    default EnumSet<Permissions> getRequiredGuildPermissions() { return EnumSet.noneOf( Permissions.class ); }
    
    /**
     * Retrieves the subcommands of this command.
     * <p>
     * By default, returns an empty set.
     *
     * @return The subcommands of this command.
     */
    default SortedSet<ICommand> getSubCommands() { return new TreeSet<ICommand>(); }
    
    /**
     * Adds a subcommand to this command, if supported.
     * <p>
     * By default, this operation is not supported and throws an exception.
     *
     * @param subCommand The subcommand to be added.
     * @throws UnsupportedOperationException if the calling ICommand does not support adding new subcommands.
     * @throws IllegalArgumentException if given ICommand is not a subcommand.
     * @see #isSubCommand()
     */
    default void addSubCommand( ICommand subCommand ) throws UnsupportedOperationException, IllegalArgumentException {
        
        throw new UnsupportedOperationException( "This ICommand does not support adding subcommands." );
        
    }
    
    /**
     * Retrieves the priority of this command. Used to break ties between commands with equal signatures.
     * <p>
     * By default, returns 0.
     *
     * @return The priority of this command.
     */
    default int getPriority() { return 0; }
    
    /**
     * Compares this ICommand with the specified ICommand for their precedence.<br>
     * A command having a higher precedence means it should be the one to be executed
     * in case both have the same signature and that signature is invoked.
     * <p>
     * Comparison is first made using their {@link #getPriority() priorities} (higher
     * priority gets higher precedence). If both have the same priority, compares their
     * names lexicographically (the one with the name that comes first lexicographically
     * has higher precedence).
     *
     * @param c The command to compare this to.
     * @return A negative number if this has a higher precedence than the given command;<br>
     *         A positive number if this has a lower precedence than the given command;<br>
     *         Zero if both have the same precedence.
     * @throws NullPointerException if the command given is null.
     */
    @Override
    default int compareTo( ICommand c ) throws NullPointerException {
        
        if ( getPriority() != c.getPriority() ) {
            return c.getPriority() - getPriority();
        }
        return getName().compareTo( c.getName() );
        
    }

}
