package net.farlands.sanctuary.discord.markdown;

import java.util.regex.Matcher;

/**
 * A replacement.
 */
public interface Replacement {
    String replacement(Matcher m);
}
