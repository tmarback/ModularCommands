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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * <p>
 * An instance of this class can parse all the annotated main commands, subcommands, success handlers,
 * and failure handlers of a single object instance. The commands and handlers parsed will call the methods
 * from the instance they were parsed from whenever they are invoked.
 * <p>
 * Handlers can be registered to be used across multiple Objects by marking their annotated methods static 
 * and registering them with {@link #registerAnnotatedHandlers(Class)}.
 *
 * @version 1.0
 * @author ThiagoTGM
 * @since 2017-07-19
 */
public final class AnnotationParser {
    
    private static final Logger LOG = LoggerFactory.getLogger( AnnotationParser.class );
    
    private static final Class<?>[] EXECUTOR_PARAM_TYPES = { CommandContext.class };
    private static final Class<?>[] FAILURE_HANDLER_PARAM_TYPES =
        { CommandContext.class, FailureReason.class };
    
    private static final Map<String, Executor> registeredSuccessHandlers = new HashMap<>();
    private static final Map<String, FailureHandler> registeredFailureHandlers = new HashMap<>();
    
    private final Object obj;
    private final Map<String, ICommand> subCommands;
    private final Map<String, Executor> successHandlers;
    private final Map<String, FailureHandler> failureHandlers;
    private volatile boolean done;
    private final List<ICommand> mainCommands;
    
    private final Map<String, Method> toParseMethods;
    private final Map<String, SubCommand> toParseAnnotations;

    /**
     * Constructs a new instance that extracts annotated commands from the given object.
     * 
     * @param obj The object to parse commands from.
     */
    public AnnotationParser( Object obj ) {
        
        this.obj = obj;
        this.subCommands = new HashMap<>();
        this.successHandlers = new HashMap<>();
        this.failureHandlers = new HashMap<>();
        this.done = false;
        this.mainCommands = new LinkedList<>();
        
        this.toParseMethods = new HashMap<>();
        this.toParseAnnotations = new HashMap<>();

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
        
        LOG.trace( "Parsing annotated main command \"{}\".", annotation.name() );
        
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
               .onExecute( ( context ) -> {
                   
                   call( method, obj, context );
                   
               })
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
                LOG.info( "\"{}\": Registering subcommand \"{}\".", annotation.name(), subCommand );
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
        
        LOG.trace( "Parsing annotated subcommand \"{}\".", annotation.name() );
        
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
               .onExecute( ( context ) -> {
                   
                   call( method, obj, context );
                   
               })
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
                LOG.info( "\"{}\" (subcommand): Registering subcommand \"{}\".", annotation.name(),
                        subCommand );
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
     * Parses a success handler from the given method and the given annotation that was present
     * on the method.
     *
     * @param method The method to use as the handler operation.
     * @param annotation The annotation that marked the method.
     * @return An Executor that runs the given method.
     * @throws IllegalArgumentException if the method is invalid.
     */
    private Executor parseSuccessHandler( Method method, SuccessHandler annotation )
            throws IllegalArgumentException {
        
        LOG.trace( "Parsing annotated success handler \"{}\".", annotation.value() );
        
        if ( !Arrays.equals( method.getParameterTypes(), EXECUTOR_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is static." );
        }
        
        return ( context ) -> {
            
            call( method, obj, context );
            
        };
        
    }
    
    /**
     * Parses a failure handler from the given method and the given annotation that was present
     * on the method.
     *
     * @param method The method to use as the handler operation.
     * @param annotation The annotation that marked the method.
     * @return A FailureHandler that runs the given method.
     * @throws IllegalArgumentException if the method is invalid.
     */
    private FailureHandler parseFailureHandler( Method method,
            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler annotation )
            throws IllegalArgumentException {
        
        LOG.trace( "Parsing annotated failure handler \"{}\".", annotation.value() );
        
        if ( !Arrays.equals( method.getParameterTypes(), FAILURE_HANDLER_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is static." );
        }
        
        return ( context, reason ) -> {
            
            call( method, obj, context, reason );
            
        };
        
    }
    
    /**
     * Parses all the main commands in the object.
     */
    private void parseMainCommands() {
        
        LOG.trace( "Parsing annotated main commands." );
        
        for ( Method method : obj.getClass().getDeclaredMethods() ) {
            /* Find each @MainCommand in the class and parse the commands that they declare */
            for ( MainCommand annotation : method.getDeclaredAnnotationsByType( MainCommand.class ) ) {
                
                try {
                    ICommand mainCommand = parseMainCommand( method, annotation );
                    mainCommands.add( mainCommand ); // Add command if successfully parsed.
                } catch ( IllegalArgumentException e ) {
                    if ( LOG.isErrorEnabled() ) {
                        LOG.error( "\"" + annotation.name() + "\": Could not parse main command.", e );
                    }
                }
                
            }
            
        }
        
    }
    
    /**
     * Parses all the subcommands in the object.
     */
    private void parseSubCommands() {
        
        LOG.trace( "Parsing annotated subcommands." );
        
        /* Get each subcommand that needs to be parsed */
        for ( Method method : obj.getClass().getDeclaredMethods() ) {
 
            for ( SubCommand annotation : method.getDeclaredAnnotationsByType( SubCommand.class ) ) {
                
                if ( toParseMethods.containsKey( annotation.name() ) ) {
                    LOG.error( "Subcommand with repeated name :\"{}\".", annotation.name() );
                } else {
                    toParseMethods.put( annotation.name(),  method );
                    toParseAnnotations.put( annotation.name(), annotation );
                }
                
            }
            
        }
        
        /* Keep parsing subcommands until no more to parse */
        while ( !toParseMethods.isEmpty() ) {
            
            String next = toParseMethods.keySet().iterator().next(); // Get next subcommand to parse.
            Method method = toParseMethods.remove( next );
            SubCommand annotation = toParseAnnotations.remove( next );
            parseWithDependencies( method, annotation );
            
        }
        
    }
    
    /**
     * Parses a subcommand, and all of its own subcommands (dependencies), recursively.
     * <p>
     * e.g., solves the dependencies of the given subcommand, and if successful, parses the
     * subcommand.
     *
     * @param method The method that is marked as the subcommand.
     * @param annotation The annotation that specifies the subcommand.
     * @return true if the subcommand (and thus all of its dependencies) were parsed sucessfully.<br>
     *         false if the subcommand could not be parsed (either due to an issue with the subcommand
     *         itself or one of its dependencies).
     */
    private boolean parseWithDependencies( Method method, SubCommand annotation ) {
        
        /* Parse sub-subcommands (dependencies) */
        for ( String subCommandName : annotation.subCommands() ) {
            
            if ( subCommands.containsKey( subCommandName ) ) {
                continue; // Subcommand already parsed.
            }
            if ( !toParseMethods.containsKey( subCommandName ) ) {
                LOG.error( "\"{}\": Invalid subcommand \"{}\".", annotation.name(),
                        subCommandName );
                return false; // Subcommand is not in the to-parse list, thus it doesn't exist
            }                 // or a dependency loop exists.
            Method subCommandMethod = toParseMethods.remove( subCommandName );
            SubCommand subCommandAnnotation = toParseAnnotations.remove( subCommandName );
            if ( !parseWithDependencies( subCommandMethod, subCommandAnnotation ) ) { // Parse subcommand.
                LOG.error( "\"{}\": Failed to parse subcommand \"{}\".", annotation.name(),
                        subCommandName );
                return false; // Could not parse sub-subcommand.
            }
            
        }
        
        /* Sub-subcommands parsed successfully, now try parsing subcommand */
        try {
            ICommand command = parseSubCommand( method, annotation );
            subCommands.put( annotation.name(), command );
        } catch ( IllegalArgumentException e ) { // Subcommand method/annotation is invalid.
            if ( LOG.isErrorEnabled() ) {
                LOG.error( "\"" + annotation.name() + "\": Could not parse subcommand.", e );
            }
            return false;
        }
        
        return true; // Parsed successfully.
        
    }
    
    /**
     * Parses all the success handlers in the object.
     */
    private void parseSuccessHandlers() {
        
        LOG.trace( "Parsing annotated success handlers." );
        /* Check each method */
        for ( Method method : obj.getClass().getDeclaredMethods() ) {
            
            if ( Modifier.isStatic( method.getModifiers() ) ) {
                continue; // Static methods are used for registrable handlers.
            }
            
            SuccessHandler annotation = method.getDeclaredAnnotation( SuccessHandler.class );
            if ( annotation != null ) { // Method has the annotation.
                
                if ( successHandlers.containsKey( annotation.value() ) ) {
                    LOG.error( "Success handler with repeated name :\"{}\".", annotation.value() );
                } else {
                    try { // Try to parse the handler.
                        Executor handler = parseSuccessHandler( method, annotation );
                        successHandlers.put( annotation.value(), handler );
                    } catch ( IllegalArgumentException e ) {
                        LOG.error( "Could not parse success handler.", e );
                    }
                }
                
            }
            
        }
        
    }
    
    /**
     * Parses all the failure handlers in the object.
     */
    private void parseFailureHandlers() {
        
        LOG.trace( "Parsing annotated failure handlers." );
        /* Check each method */
        for ( Method method : obj.getClass().getDeclaredMethods() ) {
            
            if ( Modifier.isStatic( method.getModifiers() ) ) {
                continue; // Static methods are used for registrable handlers.
            }
            
            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler annotation =
                    method.getDeclaredAnnotation(
                            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler.class );
            if ( annotation != null ) { // Method has the annotation.
                
                if ( failureHandlers.containsKey( annotation.value() ) ) {
                    LOG.error( "Failure handler with repeated name :\"{}\".", annotation.value() );
                } else {
                    try { // Try to parse the handler.
                        FailureHandler handler = parseFailureHandler( method, annotation );
                        failureHandlers.put( annotation.value(), handler );
                    } catch ( IllegalArgumentException e ) {
                        LOG.error( "Could not parse failure handler.", e );
                    }
                }
                
            }
            
        }
        
    }
    
    /**
     * Parses all the commands and handlers in the object, retrieving the main commands that were
     * parsed.
     * <p>
     * After the first time this is called, this can be called again without any processing required
     * (the parsed commands and handlers are buffered).
     *
     * @return The main commands parsed from this object.
     */
    public synchronized List<ICommand> parse() {
        
        if ( done ) {
            return new ArrayList<>( mainCommands );
        }
        
        LOG.debug( "Parsing annotated members of instance of class {}.", obj.getClass().getName() );
        
        parseFailureHandlers();
        parseSuccessHandlers();
        parseSubCommands();
        parseMainCommands();
        
        LOG.debug( "Finished parsing annotated members." );
        
        done = true;
        return new ArrayList<>( mainCommands );
        
    }
    
    /* Code for registering handlers for later use */
    
    /**
     * Registers a method as a success handler to be used while parsing ICommands.
     * <p>
     * After this, if an ICommand is being parsed and it specifies the name of this handler as
     * a success handler, and there is no other success handler with the same name in the object
     * that the command is being parsed from, this handler will then be used.
     * <p>
     * The method given will be called whenever the handler is invoked.
     *
     * @param method The method to be called by the success handler. Must be static.
     * @param annotation The annotation that marked the method.
     * @throws IllegalArgumentException if there is already a registered success handler with the specified
     *                                  name or the method is invalid.
     */
    private static void registerSuccessHandler( Method method, SuccessHandler annotation )
            throws IllegalArgumentException {
        
        LOG.info( "Registering annotated success handler \"{}\".", annotation.value() );
        
        if ( !Arrays.equals( method.getParameterTypes(), EXECUTOR_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( !Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is not static." );
        }
        if ( registeredSuccessHandlers.containsKey( annotation.value() ) ) {
            throw new IllegalArgumentException(
                    "There is already a registered success handler with this name." );
        }
        
        registeredSuccessHandlers.put( annotation.value(), ( context ) -> {
            
            call( method, null, context );
            
        });
        
    }
    
    /**
     * Registers a method as a failure handler to be used while parsing ICommands.
     * <p>
     * After this, if an ICommand is being parsed and it specifies the name of this handler as
     * a failure handler, and there is no other failure handler with the same name in the object
     * that the command is being parsed from, this handler will then be used.
     * <p>
     * The method given will be called whenever the handler is invoked.
     *
     * @param method The method to be called by the failure handler. Must be static.
     * @param annotation The annotation that marked the method.
     * @throws IllegalArgumentException if there is already a registered failure handler with the specified
     *                                  name or the method is invalid.
     */
    private static void registerFailureHandler( Method method,
            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler annotation )
            throws IllegalArgumentException {
        
        LOG.info( "Registering annotated failure handler \"{}\".", annotation.value() );
        
        if ( !Arrays.equals( method.getParameterTypes(), FAILURE_HANDLER_PARAM_TYPES ) ) {
            throw new IllegalArgumentException( "Method parameters are not valid." );
        }
        if ( !Modifier.isStatic( method.getModifiers() ) ) {
            throw new IllegalArgumentException( "Method is not static." );
        }
        if ( registeredFailureHandlers.containsKey( annotation.value() ) ) {
            throw new IllegalArgumentException(
                    "There is already a registered failure handler with this name." );
        }
        
        registeredFailureHandlers.put( annotation.value(), ( context, reason ) -> {
            
            call( method, null, context, reason );
            
        });
        
    }
    
    /**
     * Registers all the success handlers in the given class.
     * 
     * @param target Class to parse handlers from.
     */
    private static void registerSuccessHandlers( Class<?> target ) {
        /* Check each method */
        for ( Method method : target.getClass().getDeclaredMethods() ) {
            
            if ( !Modifier.isStatic( method.getModifiers() ) ) {
                continue; // Instance methods are used for parseable handlers.
            }
            
            SuccessHandler annotation = method.getDeclaredAnnotation( SuccessHandler.class );
            if ( annotation != null ) { // Method has the annotation.
                
                try { // Try to parse the handler.
                    registerSuccessHandler( method, annotation );
                } catch ( IllegalArgumentException e ) {
                    LOG.error( "Could not register success handler.", e );
                }
                
            }
            
        }
        
    }
    
    /**
     * Registers all the failure handlers in the given class.
     * 
     * @param target Class to parse handlers from.
     */
    private static void registerFailureHandlers( Class<?> target ) {
        /* Check each method */
        for ( Method method : target.getClass().getDeclaredMethods() ) {
            
            if ( !Modifier.isStatic( method.getModifiers() ) ) {
                continue; // Instance methods are used for parseable handlers.
            }
            
            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler annotation =
                    method.getDeclaredAnnotation(
                            com.github.thiagotgm.modular_commands.command.annotation.FailureHandler.class );
            if ( annotation != null ) { // Method has the annotation.
                
                try { // Try to parse the handler.
                    registerFailureHandler( method, annotation );
                } catch ( IllegalArgumentException e ) {
                    LOG.error( "Could not register failure handler.", e );
                }
                
            }
            
        }
        
    }
    
    /**
     * Parses static methods marked as SuccessHandlers or FailureHandlers to use them when parsing
     * methods.
     * <p>
     * After registering a, if an ICommand is being parsed and it specifies the name of the registered
     * handler as a handler, and there is no other handler of the appropriate type (success or
     * failure) with the same name in the object that the command is being parsed from, the registered
     * handler will then be used.
     * <p>
     * The methods annotated as handlers will be called whenever the associated handler is invoked.
     *
     * @param target The class to get annotated handlers from.
     */
    public static void registerAnnotatedHandlers( Class<?> target ) {
        
        registerSuccessHandlers( target );
        registerFailureHandlers( target );
        
    }
    
    /* Support method for calling methods and specifying thrown exceptions */
    
    /**
     * Calls the method on the given object with the given arguments, filtering out errors beyond the
     * expected exceptions.
     *
     * @param method Method to be called.
     * @param obj Object to call the method on.
     * @param args Args to call the method with.
     * @return If the method returned a boolean value, returns that value.<br>
     *         If it had no return or any other type or return, returns true (as long as
     *         the method was invoked successfully/without any exceptions).
     * @throws RateLimitException if the method threw a RateLimitException.
     * @throws MissingPermissionsException if the method threw a MissingPermissionsException.
     * @throws DiscordException if the method threw a DiscordException.
     * @throws RuntimeException if the method threw an unexpected unchecked exception, throws that
     *                          exception again. If it threw an unexpected checked exception, or
     *                          {@link Method#invoke(Object, Object...)} threw a
     *                          {@link IllegalAccessException} or {@link IllegalArgumentException},
     *                          throws a RuntimeException whose cause is the exception thrown by
     *                          {@link Method#invoke(Object, Object...) invoke()}.
     */
    private static boolean call( Method method, Object obj, Object... args )
            throws RateLimitException, MissingPermissionsException, DiscordException, RuntimeException {
        
        Object returnValue;
        try { // Try calling the method.
            returnValue = method.invoke( obj, args );
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
            throw new RuntimeException( "Unexpected checked exception.", e );
        } catch ( IllegalAccessException | IllegalArgumentException e ) {
            throw new RuntimeException( "Could not call execution method.", e );
        }
        
        if ( returnValue instanceof Boolean ) { // Returned a boolean value, so return that too.
            return ( (Boolean) returnValue ).booleanValue();
        } else { // Returned something else (or didn't return anything), but finished executing sucessfully.
            return true;
        }
        
    }

}
