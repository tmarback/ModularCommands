# ModularCommands [![](https://jitpack.io/v/ThiagoTGM/ModularCommands.svg)](https://jitpack.io/#ThiagoTGM/ModularCommands)
Framework for creating and managing chat commands for Discord bots that use Discord4J.
This framework focuses on offering the greatest flexibility for creating and managing commands in a bot as effortlessly as possible.

The javadocs are available at https://jitpack.io/com/github/ThiagoTGM/ModularCommands/@VERSION@/javadoc/, where `@VERSION@` should be replaced by the desired version. This README can't possibly explain everything in full detail, so definitely check them for more detailed information (particularly the `ICommand` interface). [latest](https://jitpack.io/com/github/ThiagoTGM/ModularCommands/0.1.0/javadoc/)

## How to Use
There are 2 ways to include this framework in your bot:

1. Download the .jar of any release and place it into the `modules` folder, so Discord4J automatically loads it;
2. Import the framework as a dependency of your project. Again two ways of doing it:
    1. Download the .jar and add it as a dependency through the IDE you are using;
    2. Add the dependency to the project manager.
       If using Maven, add the following to your `pom.xml`:
       ```xml
       ...
       <dependencies>
           ...
            <dependency>
                <groupId>com.github.ThiagoTGM</groupId>
                <artifactId>ModularCommands</artifactId>
                <version>@VERSION@</version>
            </dependency>
        </dependencies>
        ...
        <repositories>
            <repository>
                <id>jitpack.io</id>
                <url>https://jitpack.io</url>
            </repository>
            ...
        </repositories>
        ...
        ```
        
        Or, if using Gradle, add the following to your `build.gradle`:
        ```groovy
        ...
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
        ...
        dependencies {
            ...
            compile 'com.github.ThiagoTGM:ModularCommands:@VERSION@'
        }
        ...
        ```
        Where `@VERSION@` should be replaced with the desired version.
        
     After adding the dependency, make sure to load the module manually:
     ```java
     client.getModuleLoader().loadModule( new ModularCommandsModule() );
     ```
        
## Creating commands

There are 3 ways of creating commands:

1. Implementing the `ICommand` interface:

    ```java
    public class PingCommand implements ICommand {

        private boolean enabled;
        private CommandRegistry registry;
        private final SortedSet<String> aliases;

        public PingCommand() {
            this.enabled = true;
            this.aliases = new TreeSet<>( Arrays.asList( new String[] { "ping" } ) );
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled( boolean enabled ) throws IllegalStateException {
            this.enabled = enabled;
        }

        @Override
        public CommandRegistry getRegistry() {
            return registry;
        }

        @Override
        public void setRegistry( CommandRegistry registry ) {
            this.registry = registry;
        }

        @Override
        public String getName() {
            return "Ping Command (interface)";
        }

        @Override
        public SortedSet<String> getAliases() {
            return aliases;
        }

        @Override
        public boolean isSubCommand() {
            return false;
        }
        
        @Override
        public String getPrefix() { // Would not be necessary if this was a subcommand,
            return "?";             // or if the registry's prefix should be used.
        }

        @Override
        public void execute( CommandContext context )
                throws RateLimitException, MissingPermissionsException, DiscordException {
            context.getReplyBuilder().withContent( "pong!" ).build();
        }

    }
    ```
    This creates a command that replies with "pong!" whenever the bot receives a message that starts with "?ping".
    
    These are the minimum methods that need to be implemented. There are a lot more default methods that can be overriden to change the settings of your command.
    
    The downside of this way is that it takes a bit more work due to some of the basic functionality also needing to be implemented (like keeping track of enabled and registry). The upside is that if you want these operations to have side effects or any other arbitrary stuff, you're free to do so, as long as the final result still follows the contract of the interface!

2. Using a `CommandBuilder`:
    
    ```java
    ICommand pingCommand = new CommandBuilder( "Ping Command (builder)" )
                               .withAliases( new String[] { "ping" } ) // A Collection<String> object can also be used!
                               .withPrefix( "?" ) // Do not use if building a subcommand!
                               .onExecute( (context) -> {
                                   context.getReplyBuilder().withContent( "pong!" ).build();
                               })
                               .build();
   ```
   This creates the exact same command as the one above, except it took way less lines. This uses the default implementation of `ICommand`, `Command` (refer to the Javadocs for some specifics on what this implementation allows).
    
3. Using annotations:
    `// TODO: Upcoming!`

## Command Registries
In order to add a command, you're first going to need a `CommandRegistry` to add it to.
You can get the root registry using your `IDiscordClient`:
```java
CommandRegistry root = CommandRegistry.getRegistry( client );
```
If you want to, you can just add commands here, and that's fine. However, if you want to add some modularity to your commands, you can get a registry made specifically for each of your modules:

```java
CommandRegistry subRegistry = root.getSubRegistry( module );
```

The advantage of using subregistries is that, this way, your commands become _much_ more easy to manage. As you probably noticed in the example of making a command with the interface, commands can be enabled or disabled at will. But, so can registries. So, if you added your commands to separate registries for each of your modules, and for some reason you decided that you want to disable all the commands from that module:

```java
subRegistry.disable();
```

And voila! Now all the commands in that registry are disabled (and so cannot be called) until you enable it again. Do note, however, that if a registry (or a command, for that matter) is set to be `essential`, it cannot be disabled, and so this would throw an exception. The root registry is an example of such registry.

It would also be useful if you happen to need to disable the module. Instead of removing your commands one by one, just remove the entire subregistry:

```java
root.removeSubRegistry( module );
```

And you're not limited to getting subregistries only from the root registry. If, for example, you have another module that adds some commands that add more functionality to the first module, you can put those in their own subregistry:

```java
CommandRegistry subSubRegistry = subRegistry.getSubRegistry( otherModule );
```

On that note, you can use these subregistries for another use: _overriding_ commands. If a certain registry has a command with the alias `loot` and prefix `!`, but then one of its subregistries also has a command with the alias `loot` and prefix `!`, the one that would be executed whenever someone used the command `!loot` would be the one in the subregistry (unless the command in the original registry had the `overrideable` property set to false). This way, you could add functionality to commands of other modules without having a headache if you ever need to restore the original one.

OBS: it is possible to have more than one subregistry have commands with the same signature. For those moments, you can specify a `priority` to your commands, and in those cases the one with the highest priority will be chosen (in case of priority ties, the one whose `name` comes first lexicographically is picked). The same applies if there are multiple commands in the same registry with the same signature (this also means that, even if a command is set as not overrideable, it could still be replaced by a command in the same registry).

Another useful thing about subregistries is that each one can specify its own prefix. If a certain command does not specify a prefix, it will use the prefix of the registry it is registered to. If the registry does not specify a prefix, it will use the prefix of the registry it is registered to. This "prefix inheritance" can go on until the root registry, which will use the default prefix, `?`, if it does not have its own prefix. This way, if you want all your commands to have the same prefix, or set the prefix of all commands in a module at once, you just need to do one line and forget about it. And yet you can still choose to make certain commands have specific, immutable prefixes if you need to.

Also, if you need to run some kind of extra check to determine if the commands in a registry should really be executed, you can use the `CommandRegistry#setContextCheck(Predicate<CommandContext>)` method to add in your own checking operation. If the `Predicate` you specified returns false for a command's context (check the `Command Execution` section for more information on `CommandContext`), it is the same as if that registry was disabled, and the command won't be executed. This way, you can run any kind of external check, such as check for specific channels/servers/users/etc, among other things.
```java
// Makes the commands in the registry (and its subregistries) only callable from channels named "general".
// For any other channel, it is as if it was disabled.
registry.setContextCheck( (context) -> {
    return context.getChannel().getName().equals("general");
});
```

Now, once you have the registry you want to add your command to, adding the command is really simple:
```java
registry.registerCommand( new PingCommand() );
registry.registerCommand( pingCommand );
// TODO: Annotation way
```

OBS: While multiple commands can have the same `alias`, the `name` of each command _must_ be unique within the registry hierarchy (the root registry and all its subregistries).

OBS 2: If a command has one or more of its signatures overriden or otherwise has lower precedence than another command in a signature conflict, it effectively loses that signature (other signatures it may have that are not part of the conflict are not affected), even if the command that replaces it is disabled. It can only be restored by de-registering the command that replaces it.

## Subcommands
If you want your command to behave in particular ways when a certain argument is used, you can make that into a `subcommand`.
Like normal commands, subcommands have their own aliases, but instead of being activated through `prefix`+`alias`, a subcommand will be triggered if a certain _main command_ (any command that is not a subcommand) that has the subcommand in its subcommand list is called and its first argument is an alias of that subcommand.

To create a subcommand, you just create a normal command, but specify that it is a subcommand (override `isSubCommand()` if implementing the interface, use `builder#isSubCommand(true)` if using the builder, or use `@SubCommand` instead of `@MainCommand` if using annotations). Then, to add them to a main command:

1. If the main command implements the interface, include the subcommand in the return set of `ICommand#getSubCommands()`:
```java
public class MainCommand implements ICommand {
    ...
    private final SortedSet<ICommand> subCommands;
    public MainCommand() {
        ...
        subCommands = new TreeSet<>();
        subCommands.add( new SubCommand() );
    }
    ...
    @Override
    public SortedSet<ICommand> getSubCommands() { return subCommands; }
}
```
2. If the main command is being done through a CommandBuilder, the subcommands must be provided before building:

```java
...
mainCommandBuilder.withSubCommands( Arrays.asList( new ICommand[] { new SubCommand() } ) );
...
```
OBS: All subcommands must be provided at once. Calling `withSubCommands` again will replace the previously given subcommands.

3. `// TODO: Annotation way`

NOTE: Annotation-based subcommands can only be used with annotation-based main commands, and vice versa.

Also worth noting that subcommands can specify their own subcommands, which work in the same way, but using the subcommand's args.

By default, if one or more subcommands are identified, only the last subcommand is executed. So if the message was `?do stuff here right now`, and there are (sub)commands for `?do`, `?do stuff`, `?do stuff here`, `?do stuff here right`, and `?do stuff here right now` (note that the last is a subcommand of the second to last, which is a subcommand of the one before, so on so forth), only the latter one would be executed. However, a subcommand can have the `executeParent` property be true to specify that, whenever it is called, its parent is also called. This behaviour is chained, so if its parent also has this property as true, its parent would also be called, and so on so forth. In these cases, the first to be called would be the first ancestor of the last subcommand that has the `executeParent` property as false (or the main command if all subcommands have it as true). So, in the example mentioned, if both `?do stuff here right` and `?do stuff here right now` had the `executeParent` property as true, but not `?do stuff here`, the commands that would end up being executed would be `?do stuff here`, `?do stuff here right`, and `?do stuff here right now`, in that order. All the properties that would be used, however, are the ones set in the most specific subcommand (`?do stuff here right now`).

If there are more than one subcommand with an alias that matches the first argument, the one with the highest `priority` will be given precedence, and in case of a tie the one whose `name` comes first lexicographically is given precedence (same as how precedence is given in conflicts between main commands).

OBS: Like for main commands, if a subcommand has lower precedence than another subcommand in a signature conflict, it effectively loses that signature (other signatures it may have that are not part of the conflict are not affected), even if the subcommand that replaces it is disabled.

## Command Execution
Whenever a command is triggered by a message and executed, its `ICommand#execute(CommandContext)` method will be called. If the command was made through a `CommandBuilder`, this means that the `Executor` specified with `CommandBuilder#onExecute(Executor)` is called.
(`// TODO: what replaces execute() in the annotation way`)

The `CommandContext` given can provide all the important information about the command, such as who called it, where it was called from, its args, etc (it also provides the MessageReceivedEvent and corresponding IMessage that triggered the command, and the called ICommand itself, if you need it). It also provides a ready-made `MessageBuilder` for a reply (whether it is set to the same channel the message came from or a private channel to the message sender depends on the `replyPrivately` setting of the command that was called [more precisely, the most specific subcommand]). If the parent commands of a subcommand are also executed (see the `Subcommands` section), they will all receive the _exact same_ CommandContext as the most specific subcommand. This means that the args will not include any of their aliases, and they are all given the same `MessageBuilder`. However, the `CommandContext` also provides a way to store any `Object` inside it and retrieve it later, so if you want to do some common processing in a certain command and get the results in its subcommands, you can store that result in the `CommandContext` as a single object and retrieve it later (don't forget to cast it back) when the subcommand is executed! (also don't forget to specify in the subcommands that the parent should be executed).

If, in your execution method, you need to call a method that throws `RateLimitException`, `MissingPermissionsException`, or `DiscordException`, you are encouraged to just let it float up (note that the `execute` method declares all those exceptions). Particularly for `RateLimitException`s, the command is called through Discord4J's request builder, if you let it float up the execution will be reattempted automatically (so seriously, don't bother catching those). The other exceptions are logged automatically.

If the command fails for some expected reason (`MissingPermissions` or `Discord` exceptions, user that called the command does not have all required permissions, etc), the command's `ICommand#onFailure(CommandContext,FailureReason)` method will be called, being given the context of the command and a value of the `FailureReason` enum that identifies why exactly it failed, and you can use that if you want to do something in case of those expected failures. Oppositely, if the command is successfully executed, the `ICommand#onSuccess(CommandContext)` method will be called (after the delay given by the command's `onSuccessDelay` property), so you can use it if you need to do some post-processing when the command was successfully executed. Again, don't catch `RateLimitException`s for the same reason, and you can let `MissingPermissions` and `Discord` exceptions float up to be auto-logged. With the `CommandBuilder`, these operations can be specified with `CommandBuilder#onSuccess(Executor)` and `CommandBuilder#onFailure(FailureHandler)`.

OBS: The `onFailure` and `onSuccess` operations are only called for the most specific subcommand. So even if the command says that its parent (and maybe other ancestors) should be excuted, only its own success and failure handlers will be used.

## Command Properties
A command can specify several properties. The interface has methods that can be overriden, the builder has chainable methods, and the annotations have fields for all of them too.

- `name`: The name of the command. For main commands, this must be unique within the registry hierarchy. Default: none
- `aliases`: The aliases that can be used to call the command in a text message. Default: none
- `prefix`: Main command only. The prefix to be used before one of the aliases in order to call this command. If `null`, the prefix is inherited from the registry where this command is registered. Default: `null`
- `description`: The description of what the command does. Default: `""`
- `usage`: A string that shows how to call the command. For example, `"!message [user] [message]"`. Default: `""`
- `onSuccessDelay`: How long after a successful execution of the command that the `onSuccess` handler should be called, in milliseconds. Default: `0`
- `replyPrivately`: Whether the reply builder in the `CommandContext` should always be set to send a private message to the user that called the command. If false, it will be set to the same channel that the command was called from. Default: `false`
- `ignorePublic`: Whether calls to the command made from  public channels should be ignored. Default: `false`
- `ignorePrivate`: Whether calls to the command made from private channels (eg private messages) should be ignored. Default: `false`
- `ignoreBots`: Whether calls to the command made by a bot user should be ignored. Default: `true`
- `deleteCommand`: Whether the message that called the command should be deleted after the command is executed (only in case of successful execution. Happens right after the command executes and before the delay for the `onSuccess` call). Default: `false`
- `requiresOwner`: Whether only the user that owns the bot account is allowed to call the command. Default: `false`
- `NSFW`: Whether the command can only be executed in channels marked as NSFW. Default: `false`
- `overrideable`: Main command only. Whether the command can be overriden by a command in a subregistry that has the same signature. See the `Registries` section for more details. Default: `true`
- `executeParent`: Subcommand only. Whether the immediate parent command of the subcommand should be executed before it when the subcommand is called. If the parent is also a subcommand, it may also specify this as true to chain this behavior. Default: `false`
- `requiresParentPermissions`: Subcommand only. Whether the subcommand requires that the calling user satisfy the permission requirements for its parent command in addition to its own. If the parent is also a subcommand, it may also specify this as true to chain this behavior. Default: `true`
- `requiredPermissions`: The (channel-overriden) permissions that the calling user must have in the channel in order to call the command. Default: `none`
- `requiredGuildPermissions`: The (guild-wide) permissions that the calling user must have in the guild in order to call the command. Default: `none`
- `subCommands`: The subcommands of this command. If this command is called and the first argument matches the alias of one of the subcommands, that subcommand is called instead. More info on the `Subcommands` section. Default: `none`
- `priority`: If there is a situation where this command has the same signature/alias of another command and one does not override the other (both are in the same registry, both come from different subregistries of a registry, both are subcommands of the same command, etc), the one with the highest `priority` value will be given precedence. If both have the same priority, the one whose `name` comes first lexicographically is given precedence. Default: `0`

OBS: "none" means that there is no default value (a value must _always_ be specified). "`none`" means an empty set/list/etc. "`null`" means the value `null`, literally.

## 3rd-party Libraries Used

This framework uses libraries including:
- [Discord4J](https://github.com/austinv11/Discord4J), licensed under the [LGPL 3.0](https://www.gnu.org/licenses/lgpl-3.0.en.html)
- [SLF4J](https://www.slf4j.org/), licensed under the [MIT License](https://www.slf4j.org/license.html)

For testing:
- [Logback-classic](https://logback.qos.ch/), licensed under the [LGPL 2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)

Inspiration for some of the features here was drawn from the existing Discord4J command frameworks (all of which are great), such as [Discordinator](https://github.com/kvnxiao/Discordinator) and [Commands4J](https://github.com/Discord4J-Addons/Commands4J).
