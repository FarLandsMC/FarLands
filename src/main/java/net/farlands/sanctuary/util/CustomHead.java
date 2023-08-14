package net.farlands.sanctuary.util;


import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public enum CustomHead {
    REFRESH("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjk3ZDZkN2JlOTg1ZDA2MjJhNDhlOTA2OThlOTA3M2Y3ZmY4ODEzMjkyODEyZWJkMTczMGRiYTBlMDFjZjE4ZiJ9fX0="),
    ARROW_RIGHT("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJmM2EyZGZjZTBjM2RhYjdlZTEwZGIzODVlNTIyOWYxYTM5NTM0YThiYTI2NDYxNzhlMzdjNGZhOTNiIn19fQ=="),
    ARROW_LEFT("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIwZjZlOGFmNDZhYzZmYWY4ODkxNDE5MWFiNjZmMjYxZDY3MjZhNzk5OWM2MzdjZjJlNDE1OWZlMWZjNDc3In19fQ=="),
    REDSTONE("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWVhOWU4ODVlOTNmOTY0ZTAwNzVhNzVlOWFlMjVjZGFiZGEyZmZhNWQxMmZlZWRmYWIwZjg4OWIzZWRiYmU2YiJ9fX0="),
    ;

    private final ItemStack itemStack;

    CustomHead(String base64) {
        this.itemStack = getHead(base64);
    }

    public ItemStack asItemStack() {
        return this.itemStack.clone();
    }

    private static ItemStack getHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) {
            return head;
        }
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64));
        headMeta.setPlayerProfile(profile);
        head.setItemMeta(headMeta);
        return head;
    }
}
