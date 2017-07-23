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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.MessageBuilder;

/**
 * Encapsulates a command being executed and the context that caused it to be executed
 * (e.g. a received message that triggered it).<br>
 * Provides convenience methods for directly obtaining relevant information about
 * how the command was invoked, as well as a pre-configured message builder for a
 * message reply configured according to the command settings.
 * <p>
 * Also allows storing a helper object, that can be retrieved by any subcommands that
 * are called later in the execution chain (since all subcommands being executed share
 * the same Context).
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-12
 */
public class CommandContext {
    
    private final ICommand command;
    private final MessageReceivedEvent event;
    private final List<String> args;
    private final IMessage message;
    private final IUser author;
    private final IChannel channel;
    private final IGuild guild;
    private final MessageBuilder replyBuilder;
    private Optional<?> helper;

    /**
     * Builds the context for a given command triggered by the given message event.
     *
     * @param event The event that triggered the command.
     * @param command The command being executed.
     * @param args The arguments passed in to the command.
     */
    public CommandContext( MessageReceivedEvent event, ICommand command, List<String> args ) {

        this.command = command;
        this.event = event;
        this.args = new ArrayList<>( args );
        this.message = event.getMessage();
        this.author = event.getAuthor();
        this.channel = event.getChannel();
        this.guild = event.getGuild();
        this.replyBuilder = new MessageBuilder( event.getClient() );
        if ( command.replyPrivately() ) { // Reply to author on private channel.
            this.replyBuilder.withChannel( this.author.getOrCreatePMChannel() );
        } else { // Reply on original channel.
            this.replyBuilder.withChannel( this.channel );
        }
        
    }
    
    /**
     * Retrieves the command being executed in this context.
     *
     * @return The command being executed.
     */
    public ICommand getCommand() {
        
        return command;
        
    }
    
    /**
     * Retrieves the event that triggered the command.
     *
     * @return The triggering event.
     */
    public MessageReceivedEvent getEvent() {
        
        return event;
        
    }
    
    /**
     * Retrieves the arguments passed in to the command.
     *
     * @return The list of command arguments.
     */
    public List<String> getArgs() {
        
        return args;
        
    }
    
    /**
     * Retrieves the message that contained the command.
     *
     * @return The message.
     */
    public IMessage getMessage() {
        
        return message;
        
    }
    
    /**
     * Retrieves the user that invoked the command.
     *
     * @return The calling user.
     */
    public IUser getAuthor() {
        
        return author;
        
    }
    
    /**
     * Retrieves the channel that the command was invoked from.
     *
     * @return The channel.
     */
    public IChannel getChannel() {
        
        return channel;
        
    }
    
    /**
     * Retrieves the guild that the command was invoked from.
     *
     * @return The guild.
     */
    public IGuild getGuild() {
        
        return guild;
        
    }
    
    /**
     * Retrieves the prepared builder for the reply to the command.
     * <p>
     * If the command specifies a private reply, the builder is set to send a direct
     * message to the user. Else, the builder is set to reply on the same channel that
     * the command message came from.
     *
     * @return The builder for the command reply.
     */
    public MessageBuilder getReplyBuilder() {
        
        return replyBuilder;
        
    }
    
    /**
     * Stores a helper object in this Context.<br>
     * It can be later retrieved by anything that has access to this
     * Context, such as a subcommand being executed later in the execution chain
     * or a success/failure handler.
     * <p>
     * If there is already a helper object stored, it is replaced by the given one.
     *
     * @param helper Helper object to be stored. If null, deletes the object currently stored.
     */
    public void setHelper( Object helper ) throws NullPointerException {
        
        this.helper = Optional.ofNullable( helper );
        
    }
    
    /**
     * Retrieves the helper object currently stored in this Context.
     *
     * @return The helper object currently stored.
     * @see #setHelper(Object)
     */
    public Optional<?> getHelper() {
        
        return helper;
        
    }

}
