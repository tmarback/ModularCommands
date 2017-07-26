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

package com.github.thiagotgm.modular_commands.included;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.Disableable;
import com.github.thiagotgm.modular_commands.api.FailureReason;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.FailureHandler;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;

import sx.blah.discord.util.MessageBuilder;

/**
 * Command for enabling a command or registry.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-26
 */
public class EnableCommand {
    
    public static final String COMMAND_NAME = "Enable Command";
    private static final String SUBCOMMAND_NAME = "Enable Registry";
    private static final String SUCCESS_HANDLER = "Success";
    private static final String FAILURE_HANDLER = "Failure";
    
    private static final Map<String, String> types;
    
    static { // Build command-type map.
        
        Map<String, String> tempTypes = new HashMap<>();
        tempTypes.put( COMMAND_NAME, "Command" );
        tempTypes.put( SUBCOMMAND_NAME, "Registry" );
        
        types = Collections.unmodifiableMap( tempTypes );
        
    }
    
    /** Specifies the reason why the command might fail. */
    private enum Reason {
        
        /** Command/Registry specified was not found. */
        NOT_FOUND( " not found." ),
        
        /** Command/Registry specified is already enabled. */
        ALREADY_ENABLED( " is already enabled." );
        
        private final String message;
        
        private Reason( String message ) {
            
            this.message = message;
            
        }
        
        /**
         * Retrieves the message that should be printed for the failure reason.
         * <p>
         * The type of object the command was called on should be prepended to the returned
         * string.
         *
         * @return The message.
         */
        public String getMessage() {
            
            return message;
            
        }
        
    };
    
    /**
     * Tries to enable the given Disableable instance.
     *
     * @param toEnable The instance to enabled.
     * @return The reason why it could not be enabled, or null if it was enabled successfully.
     */
    private Reason enable( Disableable toEnable ) {
        
        if ( toEnable.isEnabled() ) { // Already enabled.
            return Reason.ALREADY_ENABLED;
        }
        
        toEnable.enable(); // Can be enabled. Disable it.
        return null;
        
    }

    @MainCommand(
            name = COMMAND_NAME,
            aliases = { "enable" },
            description = "Enables a command.",
            usage = "{}enable <command signature>",
            requiresOwner = true,
            essential = true,
            overrideable = false,
            canModifySubCommands = false,
            subCommands = SUBCOMMAND_NAME,
            successHandler = SUCCESS_HANDLER,
            failureHandler = FAILURE_HANDLER
            )
    public boolean disableCommand( CommandContext context ) {
        
        if ( context.getArgs().isEmpty() ) {
            return false; // No arguments given.
        }
        
        CommandRegistry registry = CommandRegistry.getRegistry( context.getEvent().getClient() );
        Iterator<String> args = context.getArgs().iterator();
        ICommand command = registry.parseCommand( args.next(), false );
        if ( command == null ) {
            context.setHelper( Reason.NOT_FOUND );
            return false; // No command with that signature.
        }
        
        while ( args.hasNext() ) { // Identify subcommands.
            
            command = command.getSubCommand( args.next() );
            if ( command == null ) {
                context.setHelper( Reason.NOT_FOUND );
                return false; // No subcommand with the argument alias.
            }
            
        }
        
        Reason reason = enable( command ); // Attempt to enable.
        if ( reason != null ) { // Could not enable.
            context.setHelper( reason );
            return false;
        }
        
        return true; // Enabled successfully.
        
    }
    
    @SubCommand(
            name = SUBCOMMAND_NAME,
            aliases = { "registry" },
            description = "Enables a registry. "
                    + "The registry type and name (for both parent registries and the target registry "
                    + "itself) should be just as shown in the registry list. All parent "
                    + "registries must be included in order. If there is a space in a "
                    + "registry name, put the whole qualified name (type:name) between "
                    + "double-quotes.",
            usage = "{}enable registry [parent registries...] <registry type>:<registry name>",
            requiresOwner = true,
            essential = true,
            canModifySubCommands = false,
            successHandler = SUCCESS_HANDLER,
            failureHandler = FAILURE_HANDLER
            )
    public boolean disableRegistry( CommandContext context ) {
        
        if ( context.getArgs().isEmpty() ) {
            return false; // No arguments given.
        }
        
        /* Get target registry */
        CommandRegistry target = CommandRegistry.getRegistry( context.getEvent().getClient() );
        Iterator<String> args = context.getArgs().iterator();               // Start with root.
        if ( !args.next().equals( target.getQualifiedName() ) ) {
            context.setHelper( Reason.NOT_FOUND );
            return false; // First arg not root registry.
        }
        while ( args.hasNext() ) {
            
            target = target.getSubRegistry( args.next() );
            if ( target == null ) {
                context.setHelper( Reason.NOT_FOUND );
                return false; // No registry with the argument name.
            }
            
        }
        
        Reason reason = enable( target ); // Attempt to enable.
        if ( reason != null ) { // Could not enable.
            context.setHelper( reason );
            return false;
        }
        
        return true; // Enabled successfully.
        
    }
    
    /**
     * Gets the type of object that the command with the given name was called on.
     *
     * @param command The command (from this class) that was called.
     * @return The type the command acts on, or an error string if the command is not
     *         recognized.
     */
    private static String getType( ICommand command ) {
        
        String type = types.get( command.getName() );
        return ( type != null ) ? type : "<error>";
        
    }
    
    @SuccessHandler( SUCCESS_HANDLER )
    public void disabledSuccessFully( CommandContext context ) {
        
        context.getReplyBuilder().withContent( getType( context.getCommand() ) + " enabled." ).build();
        
    }
    
    @FailureHandler( FAILURE_HANDLER )
    public void couldNotDisable( CommandContext context, FailureReason reason ) {
        
        switch ( reason ) {
            
            case COMMAND_OPERATION_FAILED:
                MessageBuilder builder = context.getReplyBuilder();
                if ( context.getHelper().isPresent() ) {
                    builder.withContent( getType( context.getCommand() ) );
                    builder.appendContent( ( (Reason) context.getHelper().get() ).getMessage() );
                } else { // No arguments given.
                    builder.withContent( "Missing arguments." );
                }
                builder.build();
                break;
                
            default:
                // Do nothing.
            
        }
        
    }

}
