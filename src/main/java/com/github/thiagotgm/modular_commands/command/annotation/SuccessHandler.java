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

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.api.CommandContext;

import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Annotation that marks a method that can be used as the
 * {@link ICommand#onSuccess(CommandContext) onSuccess} operation of an ICommand.<p>
 * The annotated method must take a single parameter of type {@link CommandContext}.<br>
 * It may throw exceptions of type {@link RateLimitException},
 * {@link MissingPermissionsException}, and {@link DiscordException}.
 *
 * @version 1.0.0
 * @author ThiagoTGM
 * @since 2017-07-19
 */
@Documented
@Target( METHOD )
public @interface SuccessHandler {
    
    /**
     * Retrieves the name of the handler.
     *
     * @return The name of the handler.
     */
    String value();

}
