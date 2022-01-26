package net.farlands.sanctuary.command;

import java.util.Collections;
import java.util.List;

public final class CommandConfig {

    public String             name         = null;
    public String             description  = null;
    public String             usage        = null;
    public List<String>       aliases     = Collections.emptyList();
    public CommandRequirement requirement = null;
    public Category           category    = Category.STAFF;
    public boolean            requireAlias = false;

    public static CommandConfig builder() {
        return new CommandConfig();
    }

    private CommandConfig() {
    }

    public List<String> aliasesList() {
        return aliases.stream().map(String::toLowerCase).toList();
    }

    public CommandConfig name(String name) {
        this.name = name;
        return this;
    }

    public CommandConfig description(String description) {
        this.description = description;
        return this;
    }

    public CommandConfig usage(String usage) {
        this.usage = usage;
        return this;
    }

    public CommandConfig aliases(String... aliases) {
        this.aliases = List.of(aliases);
        return this;
    }

    public CommandConfig requirement(CommandRequirement req) {
        this.requirement = req;
        return this;
    }

    public CommandConfig category(Category category) {
        this.category = category;
        return this;
    }

    public CommandConfig requireAlias() {
        this.requireAlias = true;
        return this;
    }

}
