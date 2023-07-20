package net.farlands.sanctuary.gui;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.CustomHead;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
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
    private static final ItemStack refreshButton;

    static {
        rightButton = CustomHead.ARROW_RIGHT.asItemStack();
        ItemMeta meta = rightButton.getItemMeta();
        meta.displayName(ComponentUtils.formatStyled("gold bold !italic", "Next"));
        rightButton.setItemMeta(meta);

        leftButton = CustomHead.ARROW_LEFT.asItemStack();
        meta = leftButton.getItemMeta();
        meta.displayName(ComponentUtils.formatStyled("gold bold !italic", "Previous"));
        leftButton.setItemMeta(meta);

        disabledLeftButton = CustomHead.REDSTONE.asItemStack();
        meta = disabledLeftButton.getItemMeta();
        meta.displayName(ComponentUtils.formatStyled("red bold !italic", "No Previous Page"));
        disabledLeftButton.setItemMeta(meta);

        disabledRightButton = CustomHead.REDSTONE.asItemStack();
        meta = disabledRightButton.getItemMeta();
        meta.displayName(ComponentUtils.formatStyled("red bold !italic", "No Next Page"));
        disabledRightButton.setItemMeta(meta);

        refreshButton = CustomHead.REFRESH.asItemStack();
        meta = refreshButton.getItemMeta();
        meta.displayName(ComponentUtils.formatStyled("gold !italic", "Refresh List"));
        refreshButton.setItemMeta(meta);
    }

    private int              page = 0;
    private List<ItemReward> rewards;

    public VPRewardsGui() {
        super(ComponentUtils.format("Vote Party Rewards - {} items", FarLands.getDataHandler().getConfig().voteConfig.votePartyRewards().size()), 54);
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

        addActionItem(49, refreshButton, () -> {
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
            lore.add(0, ComponentUtils.formatStyled("gray italic", "Rarity: {}", list.get(i).getRarity()));
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

                    this.user.sendMessage(ComponentColor.red("Removed item {}.", this.rewards.get(index).getStack()));
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
                this.user.sendMessage(ComponentColor.green("Added item {}.", event.getCurrentItem()));
                event.setCancelled(true);
                return;
            }
        }


        super.onItemClick(event);


    }
}
