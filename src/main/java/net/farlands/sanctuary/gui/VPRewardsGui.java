package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.CustomHead;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VPRewardsGui extends Gui {

    private static final int       pageSize = 45;
    private static final ItemStack rightButton;
    private static final ItemStack leftButton;
    private static final ItemStack disabledLeftButton;
    private static final ItemStack disabledRightButton;

    static {
        rightButton = CustomHead.ARROW_RIGHT.asItemStack();
        ItemMeta meta = rightButton.getItemMeta();
        meta.displayName(ComponentColor.gold("Next").decorate(TextDecoration.BOLD));
        rightButton.setItemMeta(meta);

        leftButton = CustomHead.ARROW_LEFT.asItemStack();
        meta = leftButton.getItemMeta();
        meta.displayName(ComponentColor.gold("Previous").decorate(TextDecoration.BOLD));
        leftButton.setItemMeta(meta);

        disabledLeftButton = CustomHead.REDSTONE.asItemStack();
        meta = disabledLeftButton.getItemMeta();
        meta.displayName(ComponentColor.gold("No Previous Page").decorate(TextDecoration.BOLD));
        disabledLeftButton.setItemMeta(meta);

        disabledRightButton = CustomHead.REDSTONE.asItemStack();
        meta = disabledRightButton.getItemMeta();
        meta.displayName(ComponentColor.gold("No Next Page").decorate(TextDecoration.BOLD));
        disabledRightButton.setItemMeta(meta);
    }

    private int              page = 0;
    private List<ItemReward> rewards;

    public VPRewardsGui() {
        super(Component.text("Vote Party Rewards - " + FarLands.getDataHandler().getConfig().voteConfig.votePartyRewards().size() + " items"), 54);
        this.loadRewards();
    }

    private void loadRewards() {
        this.rewards = new ArrayList<>(FarLands.getDataHandler().getConfig().voteConfig.votePartyRewards());
        this.rewards.sort(Comparator.comparingInt(ItemReward::getRarity));
    }

    private void addPageButtons() {
        if (this.page < this.rewards.size() / pageSize) { // Next page button
            addActionItem(53, rightButton, () -> changeInventory(1));
        } else {
            addLabel(53, disabledRightButton);
        }

        if (this.page > 0) { // Previous page button
            addActionItem(45, leftButton, () -> changeInventory(-1));
        } else {
            addLabel(45, disabledLeftButton);
        }

        ItemStack stack = CustomHead.REFRESH.asItemStack();
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(ComponentColor.gold("Refresh List"));
        stack.setItemMeta(meta);

        addActionItem(49, stack, () -> {
            this.loadRewards();
            this.refreshInventory();
        });
    }

    private void changeInventory(int delta) {
        this.page += delta;
        this.refreshInventory();
    }

    @Override
    protected void populateInventory() {

        this.addPageButtons();

        if (this.page * pageSize > this.rewards.size() || this.page < 0) this.page = 0;
        this.page = (int) FLUtils.constrain(this.page, 0, this.rewards.size() / pageSize + 1);
        List<ItemReward> list = this.rewards.subList(this.page * pageSize, Math.min(this.rewards.size(), (this.page + 1) * pageSize));


        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = clone(list.get(i).getStack());
            ArrayList<Component> lore = stack.lore() == null ? new ArrayList<Component>() : new ArrayList<>(stack.lore());
            lore.add(0, ComponentColor.gray("Rarity: {}", list.get(i).getRarity()).decorate(TextDecoration.ITALIC));
            stack.lore(lore);

            this.inventory.setItem(i, stack);
        }


    }

    @Override
    public void onItemClick(InventoryClickEvent event) {
        if (event.getRawSlot() < pageSize) {
            int index = pageSize * this.page + event.getRawSlot();
            ItemReward reward = this.rewards.get(index);
            if (!event.isShiftClick()) {
                if (event.isRightClick()) reward.setRarity(reward.getRarity() - 1);
                if (event.isLeftClick()) reward.setRarity(reward.getRarity() + 1);
                this.refreshInventory();
                event.setCancelled(true);
            } else {
                if (event.isLeftClick()) {

                    FLUtils.giveItem(this.user, reward.getStack(), true);
                    event.setCancelled(true);

                } else if (event.isRightClick()) {

                    this.user.sendMessage(ComponentColor.red("Removed item {}.", ComponentUtils.item(this.rewards.get(index).getStack())));
                    FarLands.getDataHandler().getConfig().voteConfig.votePartyRewards().remove(this.rewards.get(index));
                    this.loadRewards();
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), this::refreshInventory, 1);

                }
            }
            return;
        } else if (!this.inventory.equals(event.getClickedInventory())) {
            if (event.getCurrentItem() != null) {
                FarLands.getDataHandler().getConfig().voteConfig.votePartyRewards().add(new ItemReward(0, clone(event.getCurrentItem())));
                this.loadRewards();
                this.refreshInventory();
                this.user.sendMessage(ComponentColor.green("Added item {}.", ComponentUtils.item(event.getCurrentItem())));
                event.setCancelled(true);
                return;
            }
        }


        super.onItemClick(event);


    }
}
