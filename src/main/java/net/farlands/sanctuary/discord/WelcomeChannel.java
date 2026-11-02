package net.farlands.sanctuary.discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.ImageInfo;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.internal.requests.restaction.MessageEditActionImpl;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandDonate;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.format.NamedTextColor;

public class WelcomeChannel {
    private static MessageEmbed staffEmbed() {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(NamedTextColor.GOLD.value())
            .setTitle("Staff Members")
            .setFooter(
                "New Jr. staff are nominated by current staff with a vote when the need arises. "
                 + "There is no way to apply for staff other than to be a good player, follow the rules, and help others when needed. "
                 + "Courtesy at all times is what we look for first!"
             );

        HashMap<Rank, List<String>> players = new HashMap<>();
        List<OfflineFLPlayer> allPlayers = FarLands.getDataHandler().getOfflineFLPlayers();

        for (OfflineFLPlayer p : allPlayers) {
            if (!p.rank.isStaff()) continue;

            String str = MarkdownProcessor.escapeMarkdown(p.username);
            if (p.timezone != null && !p.timezone.isBlank()) {
                str += " (" + p.timezone + ")";
            }
            players.computeIfAbsent(p.rank, _k -> new ArrayList<>()).add(str);
        }


        for (int i = Rank.VALUES.length; i-- > 0;) {
            Rank rank = Rank.VALUES[i];
            if (!rank.isStaff()) break;

            var rankPlayers = players.get(rank);

            if (rankPlayers != null && !rankPlayers.isEmpty()) {
                builder.addField(rank.getName(), String.join(", ", rankPlayers), false);
            }
        }

        return builder.build();
    }

    private static String statusText() {
        var message = new StringBuilder();

        message.append("# Server Information\n");
        message.append("**IP**: `farlandsmc.net`\n");
        message.append("**Version**: `" + Bukkit.getMinecraftVersion() + "`\n");

        return message.toString();
    }

    static final String WELCOME_HEADER = "https://farlands.s3.us-east-005.backblazeb2.com/Welcome-Header.png";
    static final String INFO_HEADER    = "https://farlands.s3.us-east-005.backblazeb2.com/Info-header.png";
    static final String RANK_HEADER    = """
        # Playtime Ranks
        This table shows the list player ranks and some of their core features/requirements.  For more information, see <https://farlandsmc.net/ranks.html>.
        """;
    static final String RANK_TABLE = """
        ```
                   Play Time/         Teleport         /wild      Additional
        Rank       Donation    Homes  Delay     Shops  Cooldown   Features
        ─────────┬──────────────────────────────────────────────┬────────────
        Initiate │ None         1      7s        0      21m     │
                 │                                              │
        Bard     │  3h          3      6s        2      18m     │
                 │                                              │
        Esquire  │ 12h          5      6s        5      15m     │ /stack, /condense
                 │                                              │
        Knight   │  1d          8      5s        10     12m     │ /ptime
                 │                                              │
        Sage     │  3d         10      5s        15      9m     │ /pweather, /skull
                 │                                              │
        Adept    │  6d         12      4s        20      8m     │ /nick, /colors
                 │                                              │
        Scholar  │ 10d         16      3s        30      7m     │
                 │                                              │
        Donor    │ 10 USD      24      2s        40      6m     │ /hat, /particles, /echest, /hdb
                 │                                              │ 15k claim blocks, collectable
                 │                                              │
        Patron   │ 30 USD      32      None      50      3m     │ /craft, /ext
                 │                                              │ 60k claim blocks, 30m AFK, collectable
                 │                                              │
        Sponsor  │ 60 USD      40      None      50      1m     │ /mend, /eat, /editarmorstand, /petblock, /kittycannon
                 │                                              │ 100k claim blocks, collectable
        ```
        """;
    static final String MEDIA_RANK = """
        ## Media Rank
        The Media rank is given to those who stream or create videos on YouTube, Twitch, or other platforms. 

        The base requirements for this rank are as follows:
        - You must be monetized by YouTube/Twitch
        - You must be at least Knight rank
        - You must put out at least one MC video a month or 1 MC stream a week

        If you feel that you are eligible for the Media rank and have not already been reached out to by staff, please reach out to us and we will consider it!

        If you stream regularly, but are having difficulty getting monetized, staff may still grant you the media rank after internal discussion.

        Please do not over advertise your streams/videos. Links to your stream or videos are welcome in <#601607069107224586>. Stating every once and a while that you are streaming is fine, but do not solicit people for subscriptions/views/etc on the server or in DMs.

        -# Commands granted for the Media rank are for use only when streaming or recording.  If the commands are used for other purposes or getting an unfair advantage, then you may bre moved from the rank, potentially being ineligible in the future.
        """;

    /**
     * Attempt to update the #welcome messages, logging if an error occurs
     *
     * @return Whether the update happend successfully
     */
    public static boolean update() {
        try {
            updateFallible();
            return true;
        } catch (Exception e) {
            Logging.error("Error updating #welcome:", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void updateFallible() {
        TextChannel channel = DiscordChannel.WELCOME.getChannel();

        List<Consumer<MessageEditAction>> messages = List.of(
            edit -> edit.setContent(WELCOME_HEADER),
            edit -> edit.setContent("**Welcome to the official FarLands Discord!**").setEmbeds(staffEmbed()),
            edit -> edit.setContent(INFO_HEADER),
            edit -> edit.setContent(statusText()),
            edit -> edit.setContent(RANK_HEADER),
            edit -> edit.setContent(RANK_TABLE),
            edit -> edit.setContent("# Donor Ranks")
                .setEmbeds(CommandDonate.makeEmbed())
                .setActionRow(
                    Button.link("donate_link", "Donate")
                        .withUrl(FarLands.getFLConfig().donationLink)
                ),
            edit -> edit.setContent(MEDIA_RANK)
        );

        channel.getManager().setTopic("Java %s | Server IP: farlandsmc.net".formatted(Bukkit.getMinecraftVersion())).queue();

        applyMessages(channel, messages);
    }

    private static void applyMessages(MessageChannel channel, List<Consumer<MessageEditAction>> messages) {
        Logging.log("Updating #welcome messages");
        PluginData pluginData = FarLands.getDataHandler().getPluginData();

        var welcomeMessages = pluginData.welcomeMessages;

        // This is not the fastest way to do this, but it's done so rarely that it hardly matters

        // remove any welcome messages that have been deleted in the channel
        for (int i = welcomeMessages.size(); i-- > 0;) {
            var id = welcomeMessages.get(i);
            try {
                channel.retrieveMessageById(id).complete();
            } catch (ErrorResponseException ex) {
                if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                    welcomeMessages.remove(i);
                } else {
                    throw ex;
                }
            }
        }

        // We don't have enough messages in the channel
        while (welcomeMessages.size() < messages.size()) {
            var msg = channel.sendMessage("_Pending Message_").complete();
            welcomeMessages.add(msg.getIdLong());
        }

        assert welcomeMessages.size() == messages.size();

        for (int i = 0; i < welcomeMessages.size(); ++i) {
            Message msg = channel.retrieveMessageById(welcomeMessages.get(i)).complete();
            editMessage(msg, messages.get(i));
        }
    }

    private static void editMessage(Message message, Consumer<MessageEditAction> editor) {
        var edit = new MessageEditActionImpl(message.getChannel(), message.getId());

        edit.setEmbeds();
        edit.setComponents();

        editor.accept(edit);
        edit.queue();
    }
}
