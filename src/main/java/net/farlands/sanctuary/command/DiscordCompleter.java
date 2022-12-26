package net.farlands.sanctuary.command;

@FunctionalInterface
public interface DiscordCompleter {

    String[] apply(String interactionName, String partial);

}
