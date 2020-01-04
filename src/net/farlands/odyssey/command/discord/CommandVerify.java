package net.farlands.odyssey.command.discord;

import com.kicas.rp.util.Pair;
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
    private static final long VERIFICATION_EXPIRATION_TIME = 60L * 1000L;

    public CommandVerify() {
        super(Rank.INITIATE, "Verify and link your discord account to your Minecraft account.", "/verify <inGameName>",
                "verify");
        this.verificationMap = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 3 && Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(UUID.fromString(args[1]), args[0]);
            flp.setDiscordID(Long.parseLong(args[2]));
            flp.updateDiscord();
            if (flp.isDiscordVerified())
                sender.sendMessage(ChatColor.GREEN + "Successfully manually verified " + args[0] + ".");
            else
                sender.sendMessage(ChatColor.RED + "Manual verification failed. Are you sure you copied the discord ID correctly?");
            return true;
        }

        if (sender instanceof DiscordSender) { // From discord
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
                sender.sendMessage("This player already has a verification pending.");
                return true;
            }
            // Mark that they have a verification pending; set the command cooldown; tell them what to do
            verificationMap.put(
                    player.getUniqueId(),
                    new Pair<>((DiscordSender) sender, System.currentTimeMillis())
            );
            player.sendMessage(ChatColor.GOLD + "Type " + ChatColor.GREEN + "/verify" + ChatColor.GOLD + " in-game to verify your account.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
        } else if (sender instanceof Player) { // From in-game
            Player player = (Player) sender;

            Pair<DiscordSender, Long> data = verificationMap.remove(player.getUniqueId());
            // Check if they have a verification pending
            if (data == null || System.currentTimeMillis() - data.getSecond() > VERIFICATION_EXPIRATION_TIME) {
                sender.sendMessage(ChatColor.RED + "You have no verification pending. Did you type " + ChatColor.GOLD +
                        "/verify " + sender.getName() + ChatColor.RED + " in discord yet?");
                return true;
            }

            // Actually do the verification
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            flp.setDiscordID(data.getFirst().getUserID());
            flp.updateDiscord();

            // Tell them that they're verified
            player.sendMessage(ChatColor.GREEN + "Account verified!");
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
