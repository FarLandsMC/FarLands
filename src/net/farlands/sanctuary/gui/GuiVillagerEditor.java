package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.ReflectionHelper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftVillager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static org.bukkit.entity.Villager.Profession.*;

import java.util.ArrayList;
import java.util.List;

public class GuiVillagerEditor extends Gui {
    private final CraftVillager villager;
    private final List<List<ItemStack>> screens;
    private int screen;

    private static final String[] SCREEN_NAMES = {"Trades Page 1", "Trades Page 2", "Trades Page 3", "Trades Page 4", "Settings"};

    private void init() {
        // This is important
        FarLands.getDataHandler().getPluginData().addSpawnTrader(villager.getUniqueId());

        // Initialize the trade editing windows
        for(int i = 0;i < 4;++ i) {
            List<ItemStack> screen = new ArrayList<>();
            // 12 trades per window
            for(int j = i * 12;j < Math.min(villager.getRecipeCount(), 12 * (i + 1));++ j) {
                MerchantRecipe recipe = villager.getRecipe(j);
                screen.add(recipe.getIngredients().get(0));
                screen.add(recipe.getIngredients().size() == 2 ? recipe.getIngredients().get(1) : null);
                screen.add(recipe.getResult());
            }
            screens.add(screen);
        }

        // Initialize some variables
        villager.getHandle().setInvulnerable(true);
        villager.setVillagerLevel(5);
    }

    public GuiVillagerEditor(CraftVillager villager) {
        super("Villager Editor", SCREEN_NAMES[0], 54);
        this.villager = villager;
        this.screens = new ArrayList<>();
        this.screen = 0;
        init();
    }

    private void changeScreen(int newScreen) {
        saveScreen();
        screen = newScreen;
        if(screen == screens.size())
            newInventory(27, SCREEN_NAMES[SCREEN_NAMES.length - 1]);
        else
            newInventory(54, SCREEN_NAMES[newScreen]);
    }

    @Override
    protected void populateInventory() {
        int size = inv.getSize();

        // Add the screen changers
        addActionItem(size - 9, Material.DIAMOND, ChatColor.GOLD + SCREEN_NAMES[0], () -> changeScreen(0));
        addActionItem(size - 7, Material.EMERALD, ChatColor.GOLD + SCREEN_NAMES[1], () -> changeScreen(1));
        addActionItem(size - 5, Material.GOLD_INGOT, ChatColor.GOLD + SCREEN_NAMES[2], () -> changeScreen(2));
        addActionItem(size - 3, Material.IRON_INGOT, ChatColor.GOLD + SCREEN_NAMES[3], () -> changeScreen(3));
        addActionItem(size - 1, Material.PAPER, ChatColor.GOLD + SCREEN_NAMES[4], () -> changeScreen(screens.size()));

        if(screen == screens.size()) { // Settings
            // Profession options
            addActionItem(0, Material.WHEAT, ChatColor.GOLD + "Farmer", () -> villager.setProfession(FARMER));
            addActionItem(1, Material.ENCHANTED_BOOK, ChatColor.GOLD + "Librarian", () -> villager.setProfession(LIBRARIAN));
            addActionItem(2, Material.EXPERIENCE_BOTTLE, ChatColor.GOLD + "Priest", () -> villager.setProfession(CLERIC));
            addActionItem(3, Material.CHAINMAIL_CHESTPLATE, ChatColor.GOLD + "Armorer", () -> villager.setProfession(ARMORER));
            addActionItem(4, Material.IRON_SWORD, ChatColor.GOLD + "Blacksmith", () -> villager.setProfession(WEAPONSMITH));
            addActionItem(5, Material.BRICK, ChatColor.GOLD + "Mason", () -> villager.setProfession(MASON));
            addActionItem(6, Material.PORKCHOP, ChatColor.GOLD + "Butcher", () -> villager.setProfession(BUTCHER));
            addActionItem(7, Material.LEATHER, ChatColor.GOLD + "Leatherworker", () -> villager.setProfession(LEATHERWORKER));
            addActionItem(8, Material.DIRT, ChatColor.GOLD + "Nitwit", () -> villager.setProfession(NITWIT));

            // Other settings
            addActionItem(9, Material.MAP, ChatColor.GOLD + "No AI", () -> {
                villager.getHandle().setNoAI(!villager.getHandle().isNoAI());
                setLore(9, "Value: " + villager.getHandle().isNoAI());
            }, "Value: " + villager.getHandle().isNoAI());
            addActionItem(10, Material.ELYTRA, ChatColor.GOLD + "No Gravity", () -> {
                villager.getHandle().setNoGravity(!villager.getHandle().isNoGravity());
                setLore(10, "Value: " + villager.getHandle().isNoGravity());
            }, "Value: " + villager.getHandle().isNoGravity());
            addActionItem(11, Material.GLASS, ChatColor.GOLD + "Invisible", () -> {
                if(villager.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    villager.removePotionEffect(PotionEffectType.INVISIBILITY);
                    setLore(11, "Value: false");
                }else{
                    villager.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), true);
                    setLore(11, "Value: true");
                }
            }, "Value: " + villager.hasPotionEffect(PotionEffectType.INVISIBILITY));
        }else{
            // Add the labels at the top
            for(int i = 0;i < 9;i += 3) {
                addLabel(i, Material.DIAMOND, ChatColor.BLUE + "Buy 1");
                addLabel(i + 1, Material.DIAMOND, ChatColor.BLUE + "Buy 2");
                addLabel(i + 2, Material.LEATHER_HELMET, ChatColor.BLUE + "Sell", "Right-click the sold item for a trade", "to remove that trade.");
            }
            // Add the trade items to the window
            populateTrades(screens.get(screen));
        }
    }

    private void populateTrades(List<ItemStack> stacks) {
        for(int i = 9;i < stacks.size() + 9;++ i)
            inv.setItem(i, stacks.get(i - 9));
    }

    @Override
    public void onItemClick(InventoryClickEvent event) {
        super.onItemClick(event);

        int slot = event.getRawSlot();
        if(event.isCancelled() || slot > 44 || screen == screens.size())
            return;
        if(event.isRightClick() && slot % 3 == 2) { // Remove the current trade
            inv.setItem(slot - 2, null);
            inv.setItem(slot - 1, null);
            inv.setItem(slot, null);
        }
    }

    private void saveScreen() { // Take the items from the inventory and save them in the item lists
        if(screen == screens.size())
            return;
        screens.get(screen).clear();
        for(int i = 9;i < 45;++ i)
            screens.get(screen).add(inv.getItem(i));
    }

    @Override
    protected void onClose() {
        saveScreen();

        // Compound all the trades into one list
        List<ItemStack> screen = screens.get(0);
        for(int i = 1;i < screens.size();++ i)
            screen.addAll(screens.get(i));

        // Convert the items into trades
        List<MerchantRecipe> recipes = new ArrayList<>();
        for(int i = 0;i < screen.size();i += 3) {
            ItemStack buy = screen.get(i), buyB = screen.get(i + 1), sell = screen.get(i + 2);
            if(buy == null || sell == null)
                continue;
            MerchantRecipe recipe = new MerchantRecipe(sell, 0, 9999999, false);
            recipe.addIngredient(buy);
            if(buyB != null)
                recipe.addIngredient(buyB);
            recipes.add(recipe);
        }
        villager.setRecipes(recipes);

        // Now, since spigot is dumb, it ignores the rewardExp flag in the MerchantRecipe object, so we have to set it manually
        villager.getHandle().setSilent(true);
        villager.getHandle().getOffers().forEach(recipe -> ReflectionHelper.setFieldValue("rewardExp",
                net.minecraft.server.v1_16_R2.MerchantRecipe.class, recipe, false));
    }
}
