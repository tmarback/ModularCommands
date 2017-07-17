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

/**
 * Identifies the possible reasons why a command might fail to be fully executed.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public enum FailureReason {
    
    /** 
     * The bot does not have the permissions required to execute the command.
     */
    BOT_MISSING_PERMISSIONS,
    
    /**
     * The calling user does not have the (channel-overridden) permissions required to call the command.
     */
    USER_MISSING_PERMISSIONS,
    
    /**
     * The calling user does not have the (guild-wide) permissions required to call the command.
     */
    USER_MISSING_GUILD_PERMISSIONS,
    
    /**
     * The calling user is not the owner of the bot, but the command requires it.
     */
    USER_NOT_OWNER,
    
    /**
     * A miscellaneous Discord error (DiscordException) was encountered while executing the command.
     */
    DISCORD_ERROR,
    
    /**
     * The command is marked as NSFW, but the channel isn't.
     */
    CHANNEL_NOT_NSFW

}
