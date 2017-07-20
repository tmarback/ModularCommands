package com.github.thiagotgm.modular_commands.annotation;

import com.github.thiagotgm.modular_commands.api.CommandContext;
import com.github.thiagotgm.modular_commands.command.annotation.MainCommand;

public class AnnotatedCommands {

    @MainCommand(
            name = "Annotated main command",
            aliases = { "ano" }
            )
    public void reply( CommandContext context ) {
        
        context.getReplyBuilder().withContent( "Annotated!" ).build();
        
    }

}
