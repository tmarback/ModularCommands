package com.github.thiagotgm.modular_commands.annotation;

import com.github.thiagotgm.modular_commands.api.CommandRegistry;
import com.github.thiagotgm.modular_commands.api.ICommand;
import com.github.thiagotgm.modular_commands.command.annotation.AnnotationParser;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;


public class AnnotationModule implements IModule {

    @Override
    public boolean enable( IDiscordClient client ) {

        CommandRegistry reg = CommandRegistry.getRegistry( client ).getSubRegistry( this );
        for ( ICommand command : new AnnotationParser( new AnnotatedCommands() ).parse() ) {
            reg.registerCommand( command );
        }
        return true;
    }

    @Override
    public void disable() {

        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {

        return "Annotation module";
        
    }

    @Override
    public String getAuthor() {

        return "ThiagoTGM";
        
    }

    @Override
    public String getVersion() {

        return "1.0.0";
 
    }

    @Override
    public String getMinimumDiscord4JVersion() {

        return "2.8.4";
        
    }

}
