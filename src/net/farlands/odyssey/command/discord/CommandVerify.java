package net.farlands.odyssey.command.discord;

import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandVerify extends DiscordCommand {
    private final Map<UUID, Pair<DiscordSender, Long>> verificationMap;
    private static final long VERIFICATION_EXPIRATION_TIME = 2L * 60L * 1000L;

    public CommandVerify() {
        super(Rank.INITIATE, "Verify and link your discord account to your Minecraft account.", "/verify <inGameName>", "verify");
        this.verificationMap = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Usage case: /verify <username> <uuid> <discordId>
        if (args.length >= 3 && Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(UUID.fromString(args[1]), args[0]);
            flp.setDiscordID(Long.parseLong(args[2]));
            flp.updateDiscord();

            if (flp.isDiscordVerified())
                TextUtils.sendFormatted(sender, "&(green)Successfully manually verified %0.", args[0]);
            else
                TextUtils.sendFormatted(sender, "&(red)Manual verification failed. Are you sure you copied the discord ID correctly?");

            return true;
        }

        // From discord
        if (sender instanceof DiscordSender) {
            if (args.length == 0)
                return false;

            Player player = getPlayer(args[0], sender);
            if (player == null) {
                sender.sendMessage("Please log on to our server then run the command again.");
                return true;
            }

            // Check if they have a verification pending
            if (verificationMap.containsKey(player.getUniqueId()) && System.currentTimeMillis() -
                    verificationMap.get(player.getUniqueId()).getSecond() < VERIFICATION_EXPIRATION_TIME) {
                sender.sendMessage("This player already has a verification pending. Make sure to run /verify in-game to complete your verification.");
                return true;
            }

            // Mark that they have a verification pending; set the command cooldown
            verificationMap.put(
                    player.getUniqueId(),
                    new Pair<>((DiscordSender) sender, System.currentTimeMillis())
            );

            TextUtils.sendFormatted(player, "&(gold)Type {&(aqua)/verify} in-game to verify your account.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
        } else if (sender instanceof Player) { // From in-game
            Player player = (Player) sender;

            Pair<DiscordSender, Long> data = verificationMap.remove(player.getUniqueId());
            // Check if they have a verification pending
            if (data == null || System.currentTimeMillis() - data.getSecond() > VERIFICATION_EXPIRATION_TIME) {
                TextUtils.sendFormatted(sender, "&(red)You have no verification pending. Did you type " +
                        "{&(gold)/verify %0} in our discord server yet?", sender.getName());
                return true;
            }

            // Actually do the verification
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            flp.setDiscordID(data.getFirst().getUserID());
            flp.updateDiscord();

            // Tell them that they're verified
            TextUtils.sendFormatted(player, "&(green)Account verified!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
            data.getFirst().sendMessage("Account verified!");
        } else
            sender.sendMessage(ChatColor.RED + "You must use this command from in-game or discord.");

        return true;
    }

    @Override
    public boolean requiresVerifiedDiscordSenders() {
        return false;
    }
}
