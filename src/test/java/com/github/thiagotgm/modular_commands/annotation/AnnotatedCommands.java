package com.github.thiagotgm.modular_commands.annotation;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SubCommand;
import com.github.thiagotgm.modular_commands.command.annotation.SuccessHandler;

public class AnnotatedCommands {

    @MainCommand(
            name = "Annotated main command",
            aliases = { "ano" },
            subCommands = { "Annotated sub command" },
            successHandler = "handle"
            )
    public void reply( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SubCommand(
            name = "Annotated sub command",
            aliases = { "hana" },
            onSuccessDelay = 5000,
            successHandler = "handle"
            )
    public void sub( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "*cries*" ).build();
        
    }
    
    @MainCommand(
            name = "Unloadable main command",
            aliases = { "ano" },
            subCommands = { "null" }
            )
    public void fail( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SubCommand(
            name = "Unloadable sub command",
            aliases = { "ano" },
            subCommands = { "null" }
            )
    public void fail2( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }
    
    @SuccessHandler( "handle" )
    public void success( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Success!" ).build();
        
    }

}
