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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.api.Executor;
import com.github.thiagotgm.modular_commands.api.FailureHandler;
import com.github.thiagotgm.modular_commands.api.FailureReason;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.CommandBuilder;

import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Provides a way to parse annotated methods to obtain commands.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-19
 */
public final class AnnotatedCommand {
    
    private static final Logger LOG = LoggerFactory.getLogger( AnnotatedCommand.class );
    
    private static final Class<?>[] EXECUTOR_PARAM_TYPES = { CommandContext.class };
    private static final Class<?>[] FAILURE_HANDLER_PARAM_TYPES =
        { CommandContext.class, FailureReason.class };
    
    private static final Map<String, Executor> registeredSuccessHandlers = new HashMap<>();
    private static final Map<String, FailureHandler> registeredFailureHandlers = new HashMap<>();
    
    private final Object obj;
    private final Map<String, ICommand> subCommands;
    private final Map<String, Executor> successHandlers;
    private final Map<String, FailureHandler> failureHandlers;
    private final Set<String> unparsedSubCommands;

    /**
     * Constructs a new instance that extracts annotated commands from the given object.
     * 
     * @param obj The object to parse commands from.
     */
    private AnnotatedCommand( Object obj ) {
        
        this.obj = obj;
        this.subCommands = new HashMap<>();
        this.successHandlers = new HashMap<>();
        this.failureHandlers = new HashMap<>();
        this.unparsedSubCommands = new HashSet<>();

    }
    
    /**
     * Parses a main command from the given method and the given annotation that was present
     * on the method.
     *
     * @param method The method to use as the main command operation.
     * @param annotation The annotation that marked the method.
     * @return The main command described by the given annotation that executes the given method.
     * @throws IllegalArgumentException if the method given or one of the values in the annotation
     *                                  are invalid.
     */
    private ICommand parseMainCommand( Method method, MainCommand annotation )
            throws IllegalArgumentException {
        
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Parsing annotated main command \"" + annotation.name() + "\"." );
        }
        
        if ( !Arrays.equals( method.getParameterTypes(), EXECUTOR_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is static." );
        }
        
        /* Get main command properties */
        CommandBuilder builder = new CommandBuilder( annotation.name() );
        builder.essential( annotation.essential() )
               .withAliases( annotation.aliases() );
        if ( !annotation.prefix().isEmpty() ) {
            builder.withPrefix( annotation.prefix() );
        }
        builder.withDescription( annotation.description() )
               .withUsage( annotation.usage() )
               .onExecute( new MethodOperation( obj, method ) )
               .withOnSuccessDelay( annotation.onSuccessDelay() );
        if ( !annotation.successHandler().isEmpty() ) {
            /* Check if there is a SuccessHandler with the specified name */
            if ( successHandlers.containsKey( annotation.successHandler() ) ) {
                builder.onSuccess( successHandlers.get( annotation.successHandler() ) );
            } else if ( registeredSuccessHandlers.containsKey( annotation.successHandler() ) ) {
                builder.onSuccess( registeredSuccessHandlers.get( annotation.successHandler() ) );
            } else {
                throw new IllegalArgumentException( "Invalid success handler \"" + annotation.successHandler()
                        + "\"." );
            }
        }
        if ( !annotation.failureHandler().isEmpty() ) {
            /* Check if there is a FailureHandler with the specified name */
            if ( failureHandlers.containsKey( annotation.failureHandler() ) ) {
                builder.onFailure( failureHandlers.get( annotation.failureHandler() ) );
            } else if ( registeredFailureHandlers.containsKey( annotation.failureHandler() ) ) {
                builder.onFailure( registeredFailureHandlers.get( annotation.failureHandler() ) );
            } else {
                throw new IllegalArgumentException( "Invalid failure handler \"" + annotation.failureHandler()
                        + "\"." );
            }
        }
        
        /* Get command options */
        builder.replyPrivately( annotation.replyPrivately() )
               .ignorePublic( annotation.ignorePublic() )
               .ignorePrivate( annotation.ignorePrivate() )
               .ignoreBots( annotation.ignoreBots() )
               .deleteCommand( annotation.deleteCommand() )
               .requiresOwner( annotation.requiresOwner() )
               .NSFW( annotation.NSFW() )
               .overrideable( annotation.overrideable() );
        
        /* Get required permissions */
        EnumSet<Permissions> permissions = EnumSet.noneOf( Permissions.class );
        permissions.addAll( Arrays.asList( annotation.requiredPermissions() ) );
        builder.withRequiredPermissions( permissions );
        EnumSet<Permissions> guildPermissions = EnumSet.noneOf( Permissions.class );
        guildPermissions.addAll( Arrays.asList( annotation.requiredGuildPermissions() ) );
        builder.withRequiredGuildPermissions( guildPermissions );
        
        /* Get subcommands and priority */
        if ( annotation.subCommands().length > 0 ) { // Subcommands were specified.
            List<ICommand> subCommands = new ArrayList<>( annotation.subCommands().length );
            for ( String subCommand : annotation.subCommands() ) { // Parse each subcommand.
                
                if ( !this.subCommands.containsKey( subCommand ) ) { // Check subcommand exists.
                    throw new IllegalArgumentException( "Invalid subcommand \"" + subCommand + "\"." );
                }
                if ( LOG.isInfoEnabled() ) {
                    LOG.info( "Registering subcommand \"" + subCommand + "\"." );
                }
                subCommands.add( this.subCommands.get( subCommand ) );
                
            }
            builder.withSubCommands( subCommands );
        }
        builder.canModifySubCommands( annotation.canModifySubCommands() )
               .withPriority( annotation.priority() );
        
        /* Build the command */
        return builder.build();
        
    }
    
    /**
     * Parses a subcommand from the given method and the given annotation that was present
     * on the method.
     *
     * @param method The method to use as the main command operation.
     * @param annotation The annotation that marked the method.
     * @return The subcommand described by the given annotation that executes the given method.
     * @throws IllegalArgumentException if the method given or one of the values in the annotation
     *                                  are invalid.
     */
    private ICommand parseSubCommand( Method method, SubCommand annotation )
            throws IllegalArgumentException {
        
        if ( LOG.isInfoEnabled() ) {
            LOG.info( "Parsing annotated subcommand \"" + annotation.name() + "\"." );
        }
        
        if ( !Arrays.equals( method.getParameterTypes(), EXECUTOR_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is static." );
        }
        
        /* Get main command properties */
        CommandBuilder builder = new CommandBuilder( annotation.name() ).subCommand( true );
        builder.essential( annotation.essential() )
               .withAliases( annotation.aliases() );
        builder.withDescription( annotation.description() )
               .withUsage( annotation.usage() )
               .onExecute( new MethodOperation( obj, method ) )
               .withOnSuccessDelay( annotation.onSuccessDelay() );
        if ( !annotation.successHandler().isEmpty() ) {
            /* Check if there is a SuccessHandler with the specified name */
            if ( successHandlers.containsKey( annotation.successHandler() ) ) {
                builder.onSuccess( successHandlers.get( annotation.successHandler() ) );
            } else if ( registeredSuccessHandlers.containsKey( annotation.successHandler() ) ) {
                builder.onSuccess( registeredSuccessHandlers.get( annotation.successHandler() ) );
            } else {
                throw new IllegalArgumentException( "Invalid success handler \"" + annotation.successHandler()
                        + "\"." );
            }
        }
        if ( !annotation.failureHandler().isEmpty() ) {
            /* Check if there is a FailureHandler with the specified name */
            if ( failureHandlers.containsKey( annotation.failureHandler() ) ) {
                builder.onFailure( failureHandlers.get( annotation.failureHandler() ) );
            } else if ( registeredFailureHandlers.containsKey( annotation.failureHandler() ) ) {
                builder.onFailure( registeredFailureHandlers.get( annotation.failureHandler() ) );
            } else {
                throw new IllegalArgumentException( "Invalid failure handler \"" + annotation.failureHandler()
                        + "\"." );
            }
        }
        
        /* Get command options */
        builder.replyPrivately( annotation.replyPrivately() )
               .ignorePublic( annotation.ignorePublic() )
               .ignorePrivate( annotation.ignorePrivate() )
               .ignoreBots( annotation.ignoreBots() )
               .deleteCommand( annotation.deleteCommand() )
               .requiresOwner( annotation.requiresOwner() )
               .NSFW( annotation.NSFW() )
               .executeParent( annotation.executeParent() )
               .requiresParentPermissions( annotation.requiresParentPermissions() );
        
        /* Get required permissions */
        EnumSet<Permissions> permissions = EnumSet.noneOf( Permissions.class );
        permissions.addAll( Arrays.asList( annotation.requiredPermissions() ) );
        builder.withRequiredPermissions( permissions );
        EnumSet<Permissions> guildPermissions = EnumSet.noneOf( Permissions.class );
        guildPermissions.addAll( Arrays.asList( annotation.requiredGuildPermissions() ) );
        builder.withRequiredGuildPermissions( guildPermissions );
        
        /* Get subcommands and priority */
        if ( annotation.subCommands().length > 0 ) { // Subcommands were specified.
            List<ICommand> subCommands = new ArrayList<>( annotation.subCommands().length );
            for ( String subCommand : annotation.subCommands() ) { // Parse each subcommand.
                
                if ( subCommand.equals( annotation.name() ) ) {
                    throw new IllegalArgumentException( "Subcommand cannot be its own subcommand." );
                }
                if ( !this.subCommands.containsKey( subCommand ) ) { // Check subcommand exists.
                    throw new IllegalArgumentException( "Invalid subcommand \"" + subCommand + "\"." );
                }
                if ( LOG.isInfoEnabled() ) {
                    LOG.info( "Registering subcommand \"" + subCommand + "\"." );
                }
                subCommands.add( this.subCommands.get( subCommand ) );
                
            }
            builder.withSubCommands( subCommands );
        }
        builder.canModifySubCommands( annotation.canModifySubCommands() )
               .withPriority( annotation.priority() );
        
        /* Build the command */
        return builder.build();
        
    }
    
    /**
     * Executor/FailureHandler that uses a Method as the operation/task.
     * <p>
     * This class can be used as either an Executor or a FailureHandler, but the underlying method
     * will only support one.
     *
     * @version 1.0
     * @author ThiagoTGM
     * @since 2017-07-19
     * @see Executor
     * @see FailureHandler
     */
    private static class MethodOperation implements Executor, FailureHandler {
        
        private final Object obj;
        private final Method method;
        
        /**
         * Constructs an instace that uses the given method on the given object as the operation
         * to be executed.
         *
         * @param obj The object that the method should be called on.
         * @param method The method that should be used as the operation.
         */
        public MethodOperation( Object obj, Method method ) {
            
            this.obj = obj;
            this.method = method;
            
        }

        @Override
        public void accept( CommandContext context )
                throws RateLimitException, MissingPermissionsException, DiscordException {

            call( context );
            
        }
        
        @Override
        public void accept( CommandContext context, FailureReason reason )
                throws RateLimitException, MissingPermissionsException, DiscordException {

            call( context, reason );
            
        }
        
        /**
         * Calls the method with the given arguments, filtering out errors beyond the expected
         * exceptions or unchecked exceptions.
         *
         * @param args Args to call the method with.
         * @throws RateLimitException if the method threw a RateLimitException.
         * @throws MissingPermissionsException if the method threw a MissingPermissionsException.
         * @throws DiscordException if the method threw a DiscordException.
         * @throws RuntimeException if the method threw an unchecked exception.
         */
        private void call( Object... args )
                throws RateLimitException, MissingPermissionsException, DiscordException, RuntimeException {
            
            try {
                method.invoke( obj, args );
            } catch ( InvocationTargetException e ) {
                /* Identify the type of exception and rethrow it if possible */
                Throwable cause = e.getCause();
                if ( cause instanceof RateLimitException ) {
                    throw (RateLimitException) cause;
                }
                if ( cause instanceof MissingPermissionsException ) {
                    throw (MissingPermissionsException) cause;
                }
                if ( cause instanceof DiscordException ) {
                    throw (DiscordException) cause;
                }
                if ( cause instanceof RuntimeException ) {
                    throw (RuntimeException) cause;
                }
                LOG.error( "Unexpected checked exception.", e );
            } catch ( IllegalAccessException | IllegalArgumentException e ) {
                 LOG.error( "Could not call execution method.", e );
            }
            
        }
        
    }

}
