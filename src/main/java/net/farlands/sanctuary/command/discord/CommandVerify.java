package net.farlands.sanctuary.command.discord;

import com.kicas.rp.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandVerify extends DiscordCommand {
    private final Map<UUID, Pair<DiscordSender, Long>> verificationMap;
    private static final long VERIFICATION_EXPIRATION_TIME = 2L * 60L * 1000L; // 2min * 60sec * 1000ms

    public CommandVerify() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Links your discord account to your Minecraft account.", "/verify <inGameName>", "verify");
        this.verificationMap = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Usage case: /verify <username> <uuid> <discordId>
        if (args.length >= 3 && Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(UUID.fromString(args[1]), args[0]);
            flp.unverifyDiscord();
            flp.setDiscordID(Long.parseLong(args[2]));
            flp.updateDiscord();

            if (flp.isDiscordVerified())
                success(sender, "Successfully manually verified %s.", args[0]);
            else
                error(sender, "Manual verification failed. Are you sure you copied the discord ID correctly?");

            return true;
        }

        // From discord
        if (sender instanceof DiscordSender ds) {
            if (args.length == 0)
                return false;

            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(args[0]);
            Player player = flp.vanished ? null : flp.getOnlinePlayer();
            if (player == null) {
                // Player not online
                ds.getChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("Please log onto our server and run the command again.")
                        .setDescription("**Server IP:** `farlandsmc.net`")
                        .setColor(NamedTextColor.GOLD.value())
                        .build()
                ).queue();
                return true;
            }

            // Check if they have a verification pending
            if (
                verificationMap.containsKey(player.getUniqueId())
                && System.currentTimeMillis() - verificationMap.get(player.getUniqueId()).getSecond() < VERIFICATION_EXPIRATION_TIME
            ) {
                // Verification is already pending
                ds.getChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("This player already has a verification pending.")
                        .setDescription("Make sure to run `/verify` in-game to complete your verification.")
                        .setColor(NamedTextColor.RED.value())
                        .build()
                ).queue();
                return true;
            }

            // Mark that they have a verification pending; set the command cooldown
            verificationMap.put(
                    player.getUniqueId(),
                    new Pair<>((DiscordSender) sender, System.currentTimeMillis())
            );

            // Message the player on Discord
            ds.getChannel().sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("Verification pending.")
                    .setDescription("Please run `/verify` in-game to complete the verification.")
                    .setColor(NamedTextColor.GREEN.value())
                    .build()
            ).queue();

            // Message the player in-game
            player.sendMessage(
                ComponentColor.gold("Type ")
                    .append(ComponentUtils.suggestCommand("/verify"))
                    .append(ComponentColor.gold(" to verify your account."))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
        } else if (sender instanceof Player player) { // From in-game

            Pair<DiscordSender, Long> data = verificationMap.remove(player.getUniqueId());
            // Check if they have a verification pending
            if (data == null || System.currentTimeMillis() - data.getSecond() > VERIFICATION_EXPIRATION_TIME) {
                player.sendMessage(
                    ComponentColor.red("You have no verification pending. Have you run ")
                        .append(ComponentColor.gold("/verify %s", sender.getName()))
                        .append(ComponentColor.red(" in our "))
                        .append(ComponentUtils.command("/discord", ComponentColor.aqua("Discord")))
                        .append(ComponentColor.red(" server yet?"))
                );
                return true;
            }

            // Actually do the verification
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            flp.unverifyDiscord();
            flp.setDiscordID(data.getFirst().getUserID());
            flp.updateDiscord();

            // Tell them that they're verified
            success(player, "Account verified!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
            data.getFirst().getChannel().sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("Account verified!")
                    .setThumbnail(FLUtils.getHeadUrl(flp))
                    .setColor(NamedTextColor.GREEN.value())
                    .build()
            ).queue();
        } else {

            error(sender, "You must use this command from in-game or discord.");
        }

        return true;
    }

    @Override
    public boolean requiresVerifiedDiscordSenders() {
        return false;
    }
}
