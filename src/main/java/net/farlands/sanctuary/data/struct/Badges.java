package net.farlands.sanctuary.data.struct;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;

import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.object.ObjectContents;

/**
 * Bitflag object to represent a list of badges on a player
 */
public record Badges(int flags) implements Iterable<Badges.Badge>, ComponentLike {
    public Badges() {
        this(0);
    }

    public static Badges empty() {
        return new Badges();
    }

    public static Badges of(Badge... badges) {
        int flags = 0;
        for (Badge b : badges) {
            flags |= b.bit;
        }
        return new Badges(flags);
    }

    public boolean has(Badge badge) {
        return (this.flags & badge.bit) != 0;
    }

    public Badges with(Badge badge) {
        return new Badges(this.flags | badge.bit);
    }

    public Badges without(Badge badge) {
        return new Badges(this.flags & ~badge.bit);
    }

    public Badges toggle(Badge badge) {
        return new Badges(this.flags ^ badge.bit);
    }

    public Stream<Badge> badges() {
        return Arrays.stream(Badge.values())
            .filter(b -> this.has(b));
    }

	@Override
	public Iterator<Badge> iterator() {
        return this.badges().iterator();
	}

    @Override
    public final String toString() {
        String b = "";
        int i = 0;
        for (Badge badge : this) {
            if (i++ > 0) b += ", ";

            b += badge.name();
        }

        return "[" + b + "]";
    }

	@Override
	public @NotNull Component asComponent() {
        return Component.join(JoinConfiguration.noSeparators(), this.badges().toArray(ComponentLike[]::new));
	}

    public enum Badge implements ComponentLike {
        // XXX: DO NOT MOVE ENUM VARIANTS.  Their order determins the bit position.
        //
        // Add new variants at the BOTTOM of the list.
        //
        // When adding a new variant, update BadgesTest#variantsNotMoved to include the new variant
        VOTER(
            "Voter", "This player has the most votes of all players on FarLands this month",
            "items", "item/emerald"
        ),
        BIRTHDAY(
            "Birthday", "It's this player's birthday!",
            "items", "item/cake"
        ),
        ;

        private final int bit = 1 << ordinal();
        private final Component component;

        Badge(String title, String description, String atlas, String sprite) {
            Component base = Component.object()
                .contents(ObjectContents.sprite(NamespacedKey.minecraft(atlas), Key.key(sprite)))
                .shadowColor(ShadowColor.none())
                .color(NamedTextColor.WHITE)
                .build();

            Component hover = ComponentColor.white(
                "{}\n{:gray}",
                title,
                description
            );

            this.component = ComponentUtils.hover(base, hover);
        }

		@Override
		public @NotNull Component asComponent() {
            return this.component;
		}
    }
}
