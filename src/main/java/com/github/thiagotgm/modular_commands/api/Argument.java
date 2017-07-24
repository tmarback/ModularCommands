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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IInvite;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

/**
 * Represents an argument to a command. Identifies what type of argument it is (a mention,
 * an invite, an emoji, etc) and provides the object represented by the argument (the mentioned
 * user/role/channel, the invite, the emoji, etc).
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-24
 */
public class Argument {
    
    private static final String USER_PREFIX_1 = "@";
    private static final String USER_PREFIX_2 = "@!";
    private static final String USER_PREFIX_REGEX = "@!?";
    
    private static final String ROLE_PREFIX = "@&";
    
    private static final String CHANNEL_PREFIX = "#";
    
    private static final String PREFIX_REGEX = String.join( "|", ROLE_PREFIX, USER_PREFIX_REGEX,
            CHANNEL_PREFIX );
    private static final String MENTION_REGEX = "<(" + PREFIX_REGEX + ")(\\d+)>";
    private static final Pattern MENTION = Pattern.compile( MENTION_REGEX );
    
    private static final String CUSTOM_EMOJI_REGEX = "<:(\\w{2,}):(\\d+)>";
    private static final Pattern CUSTOM_EMOJI = Pattern.compile( CUSTOM_EMOJI_REGEX );
    
    private static final String INVITE_REGEX = "https://discord\\.gg/([\\w-]+)";
    private static final Pattern INVITE = Pattern.compile( INVITE_REGEX );
    
    /**
     * Enum that identifies the type of the argument.
     * <p>
     * Can be used to identify what is the type of the object returned by
     * {@link Argument#getArgument()}.
     *
     * @version 1.0
     * @author ThiagoTGM
     * @since 2017-07-24
     */
    public enum Type {
        
        /**
         * Argument is just text. {@link Argument#getArgument() getArgument()} will return a
         * {@link String}.
         */
        TEXT,
        
        /**
         * Argument is a mention to an user. {@link Argument#getArgument() getArgument()} will
         * return an {@link IUser}.
         */
        USER_MENTION,
        
        /**
         * Argument is a mention to a role. {@link Argument#getArgument() getArgument()} will
         * return an {@link IRole}.
         */
        ROLE_MENTION,
        
        /**
         * Argument is a mention to a role. {@link Argument#getArgument() getArgument()} will
         * return an {@link IChannel}.
         */
        CHANNEL_MENTION,
        
        /**
         * Argument is an Unicode emoji. {@link Argument#getArgument() getArgument()} will
         * return an {@link Emoji}.
         */
        UNICODE_EMOJI,
        
        /**
         * Argument is a custom emoji. {@link Argument#getArgument() getArgument()} will
         * return an {@link IEmoji}.
         * <p>
         * OBS: Only custom emojis that can be accessed by the bot (eg ones that come from
         * guilds that the bot is part of) can be recognized. Custom emojis that cannot be
         * accessed by the bot are treated as text.
         */
        CUSTOM_EMOJI,
        
        /**
         * Argument is an invite into a channel. {@link Argument#getArgument() getArgument()}
         * will return an {@link IInvite}.
         */
        INVITE
    }
    
    private final String text;
    private final Object argument;
    private final Type type;

    /**
     * Constructs an instance for the given argument, identifying the type of argument it is
     * and parsing the represented value.
     *
     * @param arg The argument to be parsed and represented.
     * @param client The client to use for parsing.
     */
    Argument( String arg, IDiscordClient client ) {
        
        this.text = arg;

        Matcher matcher = MENTION.matcher( arg );
        if ( matcher.matches() ) { // Check if arg is a mention.
            long id;
            try {
                id = Long.parseUnsignedLong( matcher.group( 2 ) ); // Get ID of the mentioned.
            } catch ( NumberFormatException e ) { // Not a valid long value.
                this.argument = arg; // Invalid mention. Treat as text.
                this.type = Type.TEXT;
                return;
            }
            
            switch ( matcher.group( 1 ) ) { // Attempt to get the mentioned object.
                
                case USER_PREFIX_1: // Mentioned an user.
                case USER_PREFIX_2:
                    IUser user = client.getUserByID( id );
                    if ( user != null ) {
                        this.argument = user; // Store mentioned user.
                        this.type = Type.USER_MENTION;
                        return;
                    }
                    break;
                    
                case ROLE_PREFIX: // Mentioned a role.
                    IRole role = client.getRoleByID( id );
                    if ( role != null ) {
                        this.argument = role; // Store mentioned role.
                        this.type = Type.ROLE_MENTION;
                        return;
                    }
                    break;
                    
                case CHANNEL_PREFIX: // Mentioned a channel.
                    IChannel channel = client.getChannelByID( id );
                    if ( channel != null ) {
                        this.argument = channel; // Store mentioned channel.
                        this.type = Type.CHANNEL_MENTION;
                        return;
                    }
                    break;
                
            }
            
            this.argument = arg;   // Could not obtain mentioned object.
            this.type = Type.TEXT; // Thus mention is invalid. Treat as text.
            return;
        }
        
        if ( EmojiManager.isEmoji( arg ) ) { // Arg is a unicode emoji.
            this.argument = EmojiManager.getByUnicode( arg );
            this.type = Type.UNICODE_EMOJI;
            return;
        }
        
        matcher = CUSTOM_EMOJI.matcher( arg );
        if ( matcher.matches() ) { // Check if arg is a custom emoji.
            long id;
            try {
                id = Long.parseUnsignedLong( matcher.group( 2 ) ); // Get ID of the emoji.
            } catch ( NumberFormatException e ) { // Not a valid long value.
                this.argument = arg; // Invalid emoji. Treat as text.
                this.type = Type.TEXT;
                return;
            }
            
            IEmoji emoji = null;
            for ( IGuild guild : client.getGuilds() ) { // Try to find the emoji in each
                                                    // of the guilds that the bot is in.
                emoji = guild.getEmojiByID( id );
                if ( emoji != null ) {
                    break; // Found emoji, stop searching.
                }
                
            }
            if ( emoji != null ) { // Found emoji.
                this.argument = emoji;
                this.type = Type.CUSTOM_EMOJI;
            } else { // Could not find emoji. Treat as text.
                this.argument = arg;
                this.type = Type.TEXT;
            }
            return;
        }
        
        matcher = INVITE.matcher( arg );
        if ( matcher.matches() ) { // Check if arg is an invite.
            String code = matcher.group( 1 ); // Try to get the invite by its code.
            IInvite invite = client.getInviteForCode( code );
            if ( invite != null ) { // Found invite.
                this.argument = invite;
                this.type = Type.INVITE;
            } else { // Could not find invite. Treat as text.
                this.argument = arg;
                this.type = Type.TEXT;
            }
            return;
        }
        
        this.argument = arg; // No types of argument matched. Treat as text.
        this.type = Type.TEXT;
        
    }
    
    /**
     * Retrieves the text form of the argument.
     *
     * @return The text form of the argument.
     */
    public String getText() {
        
        return text;
        
    }
    
    /**
     * Retrieves the argument.
     * <p>
     * The type of the object returned depends on the type of the argument, as
     * given by {@link #getType()}.
     *
     * @return The argument.
     */
    public Object getArgument() {
        
        return argument;
        
    }
    
    /**
     * Retrieves the type of this argument.
     *
     * @return The type of argument.
     */
    public Type getType() {
        
        return type;
        
    }

}
