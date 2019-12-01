package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GameRewardSet {
    private final List<ItemReward> rewards;
    private final Map<UUID, Integer> recipients;
    private double bias;
    private ItemStack finalReward;

    public GameRewardSet(ItemStack finalReward) {
        this.rewards = new ArrayList<>();
        this.recipients = new HashMap<>();
        this.bias = 0.37;
        this.finalReward = finalReward;
    }

    public GameRewardSet(NBTTagCompound nbt) {
        this.rewards = new ArrayList<>();
        nbt.getList("rewards", 10).stream().map(base -> new ItemReward((NBTTagCompound)base)).forEach(rewards::add);
        this.recipients = new HashMap<>();
        this.bias = nbt.getDouble("bias");
        if(nbt.hasKey("finalReward")) {
            NBTTagCompound serRecipients = nbt.getCompound("recipients");
            serRecipients.getKeys().forEach(key -> recipients.put(UUID.fromString(key), serRecipients.getInt(key)));
            this.finalReward = Utils.itemStackFromNBT(nbt.getCompound("finalReward"));
        }else
            this.finalReward = null;
    }

    public void addReward(ItemReward reward) {
        rewards.add(reward);
    }

    public void setFinalReward(ItemStack stack) {
        finalReward = stack;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public ItemStack getReward(Player player) {
        if(rewards.size() < 5)
            return rewards.get(Utils.RNG.nextInt(rewards.size())).getStack();
        int selection = Utils.biasedRandom(rewards.size(), bias);
        if(finalReward != null) {
            int received = Utils.getAndPutIfAbsent(recipients, player.getUniqueId(), 0) | (1 << selection);
            if(received + 1 == (1 << rewards.size())) {
                received |= 1 << rewards.size();
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    player.sendMessage(ChatColor.GOLD +
                            "You have received all the prizes for this game! You are now being given the final prize...");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 5.0F);
                }, 5L);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    Utils.giveItem(player, finalReward, true);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 5.0F);
                }, 60L);
            }
            recipients.put(player.getUniqueId(), received);
        }
        return rewards.get(selection).getStack();
    }

    public NBTTagCompound asTagCompound() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        rewards.stream().map(ItemReward::asTagCompound).forEach(list::add);
        nbt.set("rewards", list);
        nbt.setDouble("bias", bias);
        if(finalReward != null) {
            NBTTagCompound serRecipients = new NBTTagCompound();
            recipients.forEach((key, value) -> serRecipients.setInt(key.toString(), value));
            nbt.set("recipients", serRecipients);
            nbt.set("finalReward", Utils.itemStackToNBT(finalReward));
        }
        return nbt;
    }
}
