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

package com.github.thiagotgm.modular_commands.annotation;

import com.github.thiagotgm.modular_commands.api.Argument;
import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;
import com.vdurmont.emoji.Emoji;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IInvite;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.MessageBuilder;

public class MoreAnnotatedCommands {

    @MainCommand(
            name = "Argument Checker",
            aliases = { "args", "arguments" },
            successHandler = "done"
            )
    public void argument( CommandContext context ) {
        
        MessageBuilder builder = context.getReplyBuilder().withContent( "Arguments:" );
        for ( Argument arg : context.getArguments() ) {
            
            builder.appendContent( "\n\"`" + arg.getArgument() + "`\" (" + arg.getType() +
                    ") -> " );
            String str = "";
            switch ( arg.getType() ) {
                
                case TEXT:
                    str = (String) arg.getArgument();
                    break;
                case USER_MENTION:
                    str = ( (IUser) arg.getArgument() ).mention();
                    break;
                case ROLE_MENTION:
                    str = ( (IRole) arg.getArgument() ).mention();
                    break;
                case CHANNEL_MENTION:
                    str = ( (IChannel) arg.getArgument() ).mention();
                    break;
                case UNICODE_EMOJI:
                    str = ( (Emoji) arg.getArgument() ).getUnicode();
                    break;
                case CUSTOM_EMOJI:
                    IEmoji emoji = (IEmoji) arg.getArgument();
                    str = emoji + " from " + emoji.getGuild().getName();
                    break;
                case UNRECOGNIZED_CUSTOM_EMOJI:
                    str = ":" + arg.getArgument() + ":";
                    break;
                case INVITE:
                    IInvite invite = (IInvite) arg.getArgument();
                    str = "Invite to " + invite.getChannel() +
                            " in " + invite.getGuild().getName() + " by " +
                            invite.getInviter();
                    break;
                
            }
            builder.appendContent( str );
            
        }
        builder.build();
        
    }
    
    @SuccessHandler( "done" )
    public void handler( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "====[ Done ]====" ).build();
        
    }

}
