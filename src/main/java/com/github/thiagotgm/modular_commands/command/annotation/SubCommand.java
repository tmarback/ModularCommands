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

package com.github.thiagotgm.modular_commands.command.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.FailureReason;
import com.github.thiagotgm.modular_commands.command.CommandBuilder;

import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Annotation that marks a method that can be turned into an ICommand that is a
 * subcommand.<br>
 * The annotated method must take a single parameter of type
 * {@link CommandContext}.<br>
 * It may throw exceptions of type {@link RateLimitException},
 * {@link MissingPermissionsException}, and {@link DiscordException}.
 * <p>
 * Command properties can be specified within the annotation.
 * <p>
 * The method annotated by this will become the command's
 * {@link ICommand#execute(CommandContext)} method.
 * <p>
 * <b>NOTE:</b> Rate-limited executions of this method are automatically
 * re-attempted as a whole. As such, it may not be safe to perform multiple
 * potentially rate-limited operations in this method, particularly on the same
 * endpoint.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-19
 * @see ICommand
 */
@Documented
@Target( METHOD )
@Repeatable( SubCommands.class )
@Retention( RUNTIME )
public @interface SubCommand {

    /**
     * Retrieves the name of the command.
     * <p>
     * Must be specified.
     *
     * @return The name of the command.
     * @see ICommand#getName()
     */
    String name();

    /**
     * Retrieves whether the command is essential.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command is essential.
     * @see ICommand#isEssential()
     */
    boolean essential() default false;

    /**
     * Retrieves the aliases of the command.
     * <p>
     * Must be specified.
     *
     * @return The aliases of the command.
     * @see ICommand#getAliases()
     */
    String[] aliases();

    /**
     * Retrieves the description of the command.
     * <p>
     * By default, returns an empty string.
     *
     * @return The description of the command.
     * @see ICommand#getDescription()
     */
    String description() default "";

    /**
     * Retrieves the usage of the command.
     * <p>
     * By default, returns an empty string.
     *
     * @return The usage of the command.
     * @see ICommand#getUsage()
     */
    String usage() default "";

    /**
     * Retrieves how long after a successful execution that
     * {@link ICommand#onSuccess(CommandContext)} should be called.
     * <p>
     * By default, returns 0.
     *
     * @return The time delay before the onSuccess call, in milliseconds.
     * @see ICommand#getOnSuccessDelay()
     */
    long onSuccessDelay() default 0;

    /**
     * Retrieves the name of the operation to be performed after a successful
     * execution of the command and the specified onSuccess time delay.
     * <p>
     * By default, returns the empty string (no operation).
     *
     * @return The name of the onSuccess operation, or an empty string if none.
     * @see ICommand#onSuccess(CommandContext)
     * @see ICommand#STANDARD_SUCCESS_HANDLER
     * @see SuccessHandler
     */
    String successHandler() default "";

    /**
     * Retrieves the name of the operation to be performed after a failed call to
     * the command (when failed due to expected reasons).
     * <p>
     * By default, returns the empty string (no operation).
     *
     * @return The name of the onFailure operation, or an empty string if none.
     * @see ICommand#onFailure(CommandContext,FailureReason)
     * @see ICommand#STANDARD_FAILURE_HANDLER
     * @see FailureHandler
     */
    String failureHandler() default "";

    /**
     * Retrieves whether the command should always reply to the caller on a private
     * channel instead of the same channel the command was called from.
     * <p>
     * By default, returns false.
     *
     * @return Whether the reply should be done on a private channel.
     * @see ICommand#replyPrivately()
     */
    boolean replyPrivately() default false;

    /**
     * Retrieves whether the command should ignore calls made from public channels.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command should ignore public calls.
     * @see ICommand#ignorePublic()
     */
    boolean ignorePublic() default false;

    /**
     * Retrieves whether the command should ignore calls made from private channels.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command should ignore private calls.
     * @see ICommand#ignorePrivate()
     */
    boolean ignorePrivate() default false;

    /**
     * Retrieves whether calls to the command made by bot users should be ignored.
     * <p>
     * By default, returns true.
     *
     * @return Whether calls to the command made by bots should be ignored.
     * @see ICommand#ignoreBots()
     */
    boolean ignoreBots() default true;

    /**
     * Retrieves whether the message that called the command should be deleted after
     * a successful execution.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command message should be deleted after successfully
     *         executing.
     * @see ICommand#deleteCommand()
     */
    boolean deleteCommand() default false;

    /**
     * Retrieves whether only the owner of the bot account is allowed to call the
     * command.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command can only be called by the bot owner.
     * @see ICommand#requiresOwner()
     */
    boolean requiresOwner() default false;

    /**
     * Retrieves whether the command can only be executed in a channel marked as
     * NSFW.
     * <p>
     * By default, returns false.
     *
     * @return Whether the command requires a NSFW-marked channel.
     * @see ICommand#isNSFW()
     */
    boolean NSFW() default false;

    /**
     * Retrieves whether the parent command of the command should be executed before
     * executing the command.
     * <p>
     * By default, returns false.
     *
     * @return Whether the parent command should be executed.
     * @see ICommand#executeParent()
     */
    boolean executeParent() default false;

    /**
     * Retrieves whether the permission requirements for the parent command must be
     * satisfied in addition to the command's own requirements.
     * <p>
     * By default, returns true.
     *
     * @return Whether the parent command's permissions are also required.
     * @see ICommand#requiresParentPermissions()
     */
    boolean requiresParentPermissions() default true;

    /**
     * Retrieves the (channel-overriden) permissions that the calling user must have
     * in order to execute the command.
     * <p>
     * By default, returns an empty array.
     *
     * @return The required permissions.
     * @see ICommand#getRequiredPermissions()
     */
    Permissions[] requiredPermissions() default {};

    /**
     * Retrieves the (guild-wide) permissions that the calling user must have in
     * order to execute the command.
     * <p>
     * By default, returns an empty array.
     *
     * @return The required permissions.
     * @see ICommand#getRequiredGuildPermissions()
     */
    Permissions[] requiredGuildPermissions() default {};

    /**
     * Retrieves the names of the subcommands of the command.
     * <p>
     * By default, returns an empty array.
     *
     * @return The names of the subcommands.
     * @see ICommand#getSubCommands()
     */
    String[] subCommands() default {};

    /**
     * Retrieves whether the command supports altering the subcommand set.
     * <p>
     * By default returns true.
     *
     * @return Whether the subcommand set can be modified.
     * @see CommandBuilder#canModifySubCommands(boolean)
     */
    boolean canModifySubCommands() default true;

    /**
     * Retrieves the priority of the command.
     * <p>
     * By default, returns 0.
     *
     * @return The priority of the command.
     * @see ICommand#getPriority()
     */
    int priority() default 0;

}
