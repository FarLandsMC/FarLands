package net.farlands.sanctuary.data.struct;

import net.kyori.adventure.text.Component;

/**
 * Represents a shared home.
 */
public record ShareHome(String sender, Component message, Home home) {
}
