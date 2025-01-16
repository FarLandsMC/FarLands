package net.farlands.sanctuary.util;

import com.kicas.rp.util.ReflectionHelper;
import net.kyori.adventure.util.Index;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.farlands.sanctuary.util.FLUtils.getCraftBukkitClass;

// TODO: Types for components... somehow

public class ItemUtils {
    /**
     * Convert from a byte[] to an {@link ItemStack}
     */
    public static ItemStack itemStackFromNBT(byte[] bytes) {
        return bytes == null ? null : ItemStack.deserializeBytes(bytes);
    }

    /**
     * Convert from an {@link ItemStack} to a byte[]
     */
    public static byte[] itemStackToNBT(ItemStack stack) {
        return stack.serializeAsBytes();
    }

    /**
     * Give a player an item, optionally send messages
     */
    public static void giveItem(Player player, ItemStack stack, boolean sendMessage) {
        giveItem(player, player.getInventory(), player.getLocation(), stack, sendMessage);
    }

    /**
     * Give a player an item, attempting to place it in the provided inventory and then dropping it at the provided location if full
     */
    public static void giveItem(CommandSender recipient, Inventory inv, Location location, ItemStack stack, boolean sendMessage) {
        if (inv.firstEmpty() > -1) {
            inv.addItem(stack.clone());
        } else {
            location.getWorld().dropItem(location, stack);
            if (sendMessage) {
                recipient.sendMessage(ComponentColor.red("Your inventory was full, so you dropped the item."));
            }
        }
    }

    /**
     * Attempt to apply damage to the item, taking Unbreaking level into account
     * @param item The item to apply the damage (this is mutated)
     * @param amount The amount of damage to attempt to apply
     */
    public static void damageItem(ItemStack item, int amount) {
        if (!(item.getItemMeta() instanceof Damageable dmg)) return;

        for (int i = 0; i < amount; ++i) {
            double chance = 1 / (double) (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1);
            if (FLUtils.RNG.nextDouble() <= chance) dmg.setDamage(dmg.getDamage() + 1);
        }
        item.setItemMeta(dmg);
        if (dmg.getDamage() >= item.getType().getMaxDurability()) item.setAmount(0);
    }

    static final Class<?>                            CRAFT_ITEM_STACK_CLASS = getCraftBukkitClass("inventory.CraftItemStack");
    static final Index<DataComponentType<?>, String> COMPONENT_TYPE_INDEX;

    static {
    }

    private static net.minecraft.world.item.ItemStack toMC(ItemStack is) {
        return ((net.minecraft.world.item.ItemStack) ReflectionHelper.invoke("asNMSCopy", CRAFT_ITEM_STACK_CLASS, null, is));
    }

    private static ItemStack fromMC(net.minecraft.world.item.ItemStack is) {
        return (ItemStack) ReflectionHelper.invoke("asBukkitCopy", CRAFT_ITEM_STACK_CLASS, null, is);
    }

    public static DataComponentMap getComponents(ItemStack stack) {
        var is = toMC(stack);
        return is.getComponents();
    }

    /**
     * Check whether a component is in the {@code custom_data} of an ItemStack
     * @param stack The ItemStack
     * @param tag The tag
     * @return Whether the tag appears in the item's {@code custom_data}
     */
    public static boolean hasComponent(@NotNull ItemStack stack, @NotNull String tag) {
        DataComponentMap comps = getComponents(stack);
        CustomData c = comps.get(DataComponents.CUSTOM_DATA);
        if (c == null) return false;
        AtomicBoolean out = new AtomicBoolean(false);
        c.update(t -> out.set(t.contains(tag)));
        return out.get();
    }

    /**
     * Get the {@code custom_data} of an ItemStack
     * @param stack the ItemStack
     * @return A copy of the {@code custom_data}
     */
    public static @Nullable CompoundTag getCustomData(@NotNull ItemStack stack) {
        DataComponentMap comps = getComponents(stack);
        CustomData c = comps.get(DataComponents.CUSTOM_DATA);
        if (c == null) return null;
        AtomicReference<CompoundTag> tag = new AtomicReference<>();
        c.update(tag::set);
        return tag.get();
    }

    public static void updateCustomData(@NotNull ItemStack stack, Consumer<CompoundTag> consumer) {
        DataComponentMap comps = getComponents(stack);
        CustomData c = comps.get(DataComponents.CUSTOM_DATA);
        c = c.update(consumer);
        var is = toMC(stack);

        is.applyComponents(DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, c).build());

    }

    static {
        COMPONENT_TYPE_INDEX = Index.create(
            cat_str -> switch (cat_str) {
                case "custom_data" -> DataComponents.CUSTOM_DATA;
                case "max_stack_size" -> DataComponents.MAX_STACK_SIZE;
                case "max_damage" -> DataComponents.MAX_DAMAGE;
                case "damage" -> DataComponents.DAMAGE;
                case "unbreakable" -> DataComponents.UNBREAKABLE;
                case "custom_name" -> DataComponents.CUSTOM_NAME;
                case "item_name" -> DataComponents.ITEM_NAME;
                case "lore" -> DataComponents.LORE;
                case "rarity" -> DataComponents.RARITY;
                case "enchantments" -> DataComponents.ENCHANTMENTS;
                case "can_place_on" -> DataComponents.CAN_PLACE_ON;
                case "can_break" -> DataComponents.CAN_BREAK;
                case "attribute_modifiers" -> DataComponents.ATTRIBUTE_MODIFIERS;
                case "custom_model_data" -> DataComponents.CUSTOM_MODEL_DATA;
                case "hide_additional_tooltip" -> DataComponents.HIDE_ADDITIONAL_TOOLTIP;
                case "hide_tooltip" -> DataComponents.HIDE_TOOLTIP;
                case "repair_cost" -> DataComponents.REPAIR_COST;
                case "creative_slot_lock" -> DataComponents.CREATIVE_SLOT_LOCK;
                case "enchantment_glint_override" -> DataComponents.ENCHANTMENT_GLINT_OVERRIDE;
                case "intangible_projectile" -> DataComponents.INTANGIBLE_PROJECTILE;
                case "food" -> DataComponents.FOOD;
                //case "fire_resistant" -> DataComponents.FIRE_RESISTANT;
                case "tool" -> DataComponents.TOOL;
                case "stored_enchantments" -> DataComponents.STORED_ENCHANTMENTS;
                case "dyed_color" -> DataComponents.DYED_COLOR;
                case "map_color" -> DataComponents.MAP_COLOR;
                case "map_id" -> DataComponents.MAP_ID;
                case "map_decorations" -> DataComponents.MAP_DECORATIONS;
                case "map_post_processing" -> DataComponents.MAP_POST_PROCESSING;
                case "charged_projectiles" -> DataComponents.CHARGED_PROJECTILES;
                case "bundle_contents" -> DataComponents.BUNDLE_CONTENTS;
                case "potion_contents" -> DataComponents.POTION_CONTENTS;
                case "suspicious_stew_effects" -> DataComponents.SUSPICIOUS_STEW_EFFECTS;
                case "writable_book_content" -> DataComponents.WRITABLE_BOOK_CONTENT;
                case "written_book_content" -> DataComponents.WRITTEN_BOOK_CONTENT;
                case "trim" -> DataComponents.TRIM;
                case "debug_stick_state" -> DataComponents.DEBUG_STICK_STATE;
                case "entity_data" -> DataComponents.ENTITY_DATA;
                case "bucket_entity_data" -> DataComponents.BUCKET_ENTITY_DATA;
                case "block_entity_data" -> DataComponents.BLOCK_ENTITY_DATA;
                case "instrument" -> DataComponents.INSTRUMENT;
                case "ominous_bottle_amplifier" -> DataComponents.OMINOUS_BOTTLE_AMPLIFIER;
                case "recipes" -> DataComponents.RECIPES;
                case "lodestone_tracker" -> DataComponents.LODESTONE_TRACKER;
                case "firework_explosion" -> DataComponents.FIREWORK_EXPLOSION;
                case "fireworks" -> DataComponents.FIREWORKS;
                case "profile" -> DataComponents.PROFILE;
                case "note_block_sound" -> DataComponents.NOTE_BLOCK_SOUND;
                case "banner_patterns" -> DataComponents.BANNER_PATTERNS;
                case "base_color" -> DataComponents.BASE_COLOR;
                case "pot_decorations" -> DataComponents.POT_DECORATIONS;
                case "container" -> DataComponents.CONTAINER;
                case "block_state" -> DataComponents.BLOCK_STATE;
                case "bees" -> DataComponents.BEES;
                case "lock" -> DataComponents.LOCK;
                case "container_loot" -> DataComponents.CONTAINER_LOOT;
                default -> throw new RuntimeException("Invalid category: " + cat_str);
            },
            "custom_data",
            "max_stack_size",
            "max_damage",
            "damage",
            "unbreakable",
            "custom_name",
            "item_name",
            "lore",
            "rarity",
            "enchantments",
            "can_place_on",
            "can_break",
            "attribute_modifiers",
            "custom_model_data",
            "hide_additional_tooltip",
            "hide_tooltip",
            "repair_cost",
            "creative_slot_lock",
            "enchantment_glint_override",
            "intangible_projectile",
            "food",
            //"fire_resistant",
            "tool",
            "stored_enchantments",
            "dyed_color",
            "map_color",
            "map_id",
            "map_decorations",
            "map_post_processing",
            "charged_projectiles",
            "bundle_contents",
            "potion_contents",
            "suspicious_stew_effects",
            "writable_book_content",
            "written_book_content",
            "trim",
            "debug_stick_state",
            "entity_data",
            "bucket_entity_data",
            "block_entity_data",
            "instrument",
            "ominous_bottle_amplifier",
            "recipes",
            "lodestone_tracker",
            "firework_explosion",
            "fireworks",
            "profile",
            "note_block_sound",
            "banner_patterns",
            "base_color",
            "pot_decorations",
            "container",
            "block_state",
            "bees",
            "lock",
            "container_loot"
        );
    }

}
