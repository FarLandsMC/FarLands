package net.farlands.sanctuary.data.struct;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Used for creating player skulls.
 */
public class SkullCreator {

    /**
     * Creates a new player head item stack.
     *
     * @return Player head.
     */
    public static ItemStack createSkull() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    /**
     * Creates a player skull item with the skin based on a player's name.
     *
     * @param name The player's name.
     * @return The head of the player.
     */
    public static ItemStack skullFromName(String name) {
        return itemWithName(createSkull(), name);
    }

    /**
     * Creates a player skull item with the skin based on a player's UUID.
     *
     * @param id The player's UUID.
     * @return The head of the player.
     */
    public static ItemStack skullFromUuid(UUID id) {
        return itemWithUuid(createSkull(), id);
    }

    /**
     * Modifies a skull to use the skin of the player with a given name.
     *
     * @param item The item to apply the name to. Must be a player skull.
     * @param name The player's name.
     * @return The head of the player.
     */
    public static ItemStack itemWithName(ItemStack item, String name) {
        notNull(item, "item");
        notNull(name, "name");
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Modifies a skull to use the skin of the player with a given UUID.
     *
     * @param item The item to apply the name to. Must be a player skull.
     * @param id   The player's UUID.
     * @return The head of the player.
     */
    public static ItemStack itemWithUuid(ItemStack item, UUID id) {
        notNull(item, "item");
        notNull(id, "id");
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        item.setItemMeta(meta);
        return item;
    }

    private static void notNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " should not be null!");
        }
    }
}
