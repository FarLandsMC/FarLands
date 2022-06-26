package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.entity.Villager.Profession.*;

/**
 * Villager editor GUI
 */
public class GuiVillagerEditor extends Gui {

    private final Villager              villager; // Selected villager
    private final List<List<ItemStack>> screens;
    private       int                   screen;

    private static final String[] SCREEN_NAMES = { "Trades Page 1", "Trades Page 2", "Trades Page 3", "Trades Page 4", "Settings" };

    private void init() {
        FarLands.getDataHandler().getPluginData().addSpawnTrader(this.villager.getUniqueId()); // Add the villager to the spawn trader list, so events can happen properly

        // Initialize the trade editing windows
        for (int i = 0; i < 4; i++) {
            List<ItemStack> screen = new ArrayList<>();
            // 12 trades per window
            for (int j = i * 12; j < Math.min(this.villager.getRecipeCount(), 12 * (i + 1)); j++) {
                MerchantRecipe recipe = this.villager.getRecipe(j);
                screen.add(recipe.getIngredients().get(0));
                screen.add(recipe.getIngredients().size() == 2 ? recipe.getIngredients().get(1) : null);
                screen.add(recipe.getResult());
            }
            this.screens.add(screen);
        }

        // Initialize some variables
        this.villager.setInvulnerable(true);
        this.villager.setVillagerLevel(5);
    }

    /**
     * Create a new GUI for a given villager
     *
     * @param villager Villager to edit
     */
    public GuiVillagerEditor(CraftVillager villager) {
        super(Component.text(SCREEN_NAMES[0]), 54);
        this.villager = villager;
        this.screens = new ArrayList<>();
        this.screen = 0;
        init();
    }

    /**
     * Set the current screen for the GUI
     */
    private void changeScreen(int newScreen) {
        saveScreen();
        this.screen = newScreen;
        if (this.screen == this.screens.size()) {
            newInventory(27, Component.text(SCREEN_NAMES[SCREEN_NAMES.length - 1]));
        } else {
            newInventory(54, Component.text(SCREEN_NAMES[newScreen]));
        }
    }

    @Override
    protected void populateInventory() {
        int size = this.inventory.getSize();

        // Add the screen changers
        addActionItem(size - 9, Material.DIAMOND, ComponentColor.gold(SCREEN_NAMES[0]), () -> changeScreen(0));
        addActionItem(size - 7, Material.EMERALD, ComponentColor.gold(SCREEN_NAMES[1]), () -> changeScreen(1));
        addActionItem(size - 5, Material.GOLD_INGOT, ComponentColor.gold(SCREEN_NAMES[2]), () -> changeScreen(2));
        addActionItem(size - 3, Material.IRON_INGOT, ComponentColor.gold(SCREEN_NAMES[3]), () -> changeScreen(3));
        addActionItem(size - 1, Material.PAPER, ComponentColor.gold(SCREEN_NAMES[4]), () -> changeScreen(screens.size()));

        if (this.screen == this.screens.size()) { // Settings
            // Profession options
            addActionItem(0, Material.WHEAT, ComponentColor.gold("Farmer"), () -> villager.setProfession(FARMER));
            addActionItem(1, Material.ENCHANTED_BOOK, ComponentColor.gold("Librarian"), () -> villager.setProfession(LIBRARIAN));
            addActionItem(2, Material.EXPERIENCE_BOTTLE, ComponentColor.gold("Priest"), () -> villager.setProfession(CLERIC));
            addActionItem(3, Material.CHAINMAIL_CHESTPLATE, ComponentColor.gold("Armorer"), () -> villager.setProfession(ARMORER));
            addActionItem(4, Material.IRON_SWORD, ComponentColor.gold("Blacksmith"), () -> villager.setProfession(WEAPONSMITH));
            addActionItem(5, Material.BRICK, ComponentColor.gold("Mason"), () -> villager.setProfession(MASON));
            addActionItem(6, Material.PORKCHOP, ComponentColor.gold("Butcher"), () -> villager.setProfession(BUTCHER));
            addActionItem(7, Material.LEATHER, ComponentColor.gold("Leatherworker"), () -> villager.setProfession(LEATHERWORKER));
            addActionItem(8, Material.DIRT, ComponentColor.gold("Nitwit"), () -> villager.setProfession(NITWIT));

            // Other settings
            addActionItem(9, Material.MAP, ComponentColor.gold("No AI"), () -> {
                this.villager.setAI(!this.villager.hasAI());
                setLore(9, Component.text("Value: " + !this.villager.hasAI()));
            }, Component.text("Value: " + !this.villager.hasAI()));

            addActionItem(10, Material.ELYTRA, ComponentColor.gold("No Gravity"), () -> {
                this.villager.setGravity(!this.villager.hasGravity());
                setLore(10, Component.text("Value: " + !this.villager.hasGravity()));
            }, Component.text("Value: " + !this.villager.hasGravity()));

            addActionItem(11, Material.GLASS, ComponentColor.gold("Invisible"), () -> {
                if (this.villager.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    this.villager.removePotionEffect(PotionEffectType.INVISIBILITY);
                    setLore(11, Component.text("Value: false"));
                } else {
                    this.villager.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                    setLore(11, Component.text("Value: true"));
                }
            }, Component.text("Value: " + this.villager.hasPotionEffect(PotionEffectType.INVISIBILITY)));
        } else {
            // Add the labels at the top
            for (int i = 0; i < 9; i += 3) {
                addLabel(i, Material.DIAMOND, ComponentColor.blue("Buy 1"));
                addLabel(i + 1, Material.DIAMOND, ComponentColor.blue("Buy 2"));
                addLabel(i + 2, Material.LEATHER_HELMET, ComponentColor.blue("Sell"), Component.text("Right-click the sold item for a trade"), Component.text("to remove that trade."));
            }
            // Add the trade items to the window
            populateTrades(this.screens.get(this.screen));
        }
    }

    private void populateTrades(List<ItemStack> stacks) {
        for (int i = 9; i < stacks.size() + 9; i++) {
            this.inventory.setItem(i, stacks.get(i - 9));
        }
    }

    @Override
    public void onItemClick(InventoryClickEvent event) {
        super.onItemClick(event);

        int slot = event.getRawSlot();
        if (event.isCancelled() || slot > 44 || this.screen == this.screens.size()) {
            return;
        }
        if (event.isRightClick() && slot % 3 == 2) { // Remove the current trade
            this.inventory.setItem(slot - 2, null);
            this.inventory.setItem(slot - 1, null);
            this.inventory.setItem(slot, null);
        }
    }

    private void saveScreen() { // Take the items from the inventory and save them in the item lists
        if (this.screen == this.screens.size()) {
            return;
        }
        this.screens.get(this.screen).clear();
        for (int i = 9; i < 45; i++) {
            this.screens.get(this.screen).add(this.inventory.getItem(i));
        }
    }

    @Override
    protected void onClose() {
        saveScreen();

        // Compound all the trades into one list
        List<ItemStack> screen = this.screens.get(0);
        for (int i = 1; i < this.screens.size(); ++i) {
            screen.addAll(this.screens.get(i));
        }

        // Convert the items into trades
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < screen.size(); i += 3) {
            ItemStack buy = screen.get(i), buyB = screen.get(i + 1), sell = screen.get(i + 2);
            if (buy == null || sell == null) {
                continue;
            }
            MerchantRecipe recipe = new MerchantRecipe(sell, 0, 9999999, false);
            recipe.addIngredient(buy);
            if (buyB != null) {
                recipe.addIngredient(buyB);
            }
            recipes.add(recipe);
        }
        this.villager.setRecipes(recipes);

        // Now, since spigot is dumb, it ignores the rewardExp flag in the MerchantRecipe object, so we have to set it manually
        this.villager.setSilent(true);
        this.villager.getRecipes().forEach(recipe -> recipe.setExperienceReward(false));
    }
}
