package net.farlands.sanctuary.command.discord;

import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

public class CommandAddReactions extends DiscordCommand {

    private static final Map<Character, String> UNICODE_EMOJIS = new ImmutableMap.Builder<Character, String>()
        .put('1', "1️⃣")
        .put('2', "2️⃣")
        .put('3', "3️⃣")
        .put('4', "4️⃣")
        .put('5', "5️⃣")
        .put('6', "6️⃣")
        .put('7', "7️⃣")
        .put('8', "8️⃣")
        .put('9', "9️⃣")
        .put('0', "0️⃣")
        .put('a', "🇦")
        .put('b', "🇧")
        .put('c', "🇨")
        .put('d', "🇩")
        .put('e', "🇪")
        .put('f', "🇫")
        .put('g', "🇬")
        .put('h', "🇭")
        .put('i', "🇮")
        .put('j', "🇯")
        .put('k', "🇰")
        .put('l', "🇱")
        .put('m', "🇲")
        .put('n', "🇳")
        .put('o', "🇴")
        .put('p', "🇵")
        .put('q', "🇶")
        .put('r', "🇷")
        .put('s', "🇸")
        .put('t', "🇹")
        .put('u', "🇺")
        .put('v', "🇻")
        .put('w', "🇼")
        .put('x', "🇽")
        .put('y', "🇾")
        .put('z', "🇿")
        .build();

    public CommandAddReactions() {
        super(
            Rank.JR_BUILDER,
            "Add reactions to a message for voting.",
            "/addreactions [channel-mention] <message-id> <reactions...>",
            "addreactions",
            "addreaction", "addreact"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] argsArr) {
        if (!(sender instanceof DiscordSender)) {
            sender.sendMessage(ComponentColor.red("This command must be used from Discord."));
            return false;
        }
        if(argsArr.length < 1) return false;
        try {
            List<String> args = new ArrayList<>(List.of(argsArr));

            String[] parts = args.remove(0).split(":");
            String commandChannelID = parts[0];
            String commandMessageID = parts[1];

            TextChannel commandChannel = FarLands.getDiscordHandler().getGuild().getTextChannelById(commandChannelID);
            if (commandChannel == null) {
                sender.sendMessage(ComponentColor.red("Channel not found."));
                return true;
            }
            Message commandMessage = commandChannel.retrieveMessageById(commandMessageID).complete();

            args = new ArrayList<>(List.of(commandMessage.getContentRaw().split(" ")));
            args.remove(0); // Remove the command itself


            TextChannel channel = commandChannel;
            Message message = getReplyMessage(commandMessage);
            if (message == null) {
                if (args.get(0).matches("^<#\\d+>$")) { // channel id
                    String channelID = args.get(0).substring(2, args.get(0).length() - 1);
                    channel = FarLands.getDiscordHandler().getGuild().getTextChannelById(channelID);
                    args.remove(0);
                }

                if (channel == null) return false;

                String messageID = args.remove(0);
                message = channel.retrieveMessageById(messageID).complete();
                if (message == null) {
                    error(sender, "Unable to find message with id '%s'", messageID);
                    return true;
                }
            }

            if (args.isEmpty()) return false; // Not enough args

            List<Emoji> emotes = new ArrayList<>();
            List<String> unicodeEmojis = new ArrayList<>();
            Set<String> badEmotes = new HashSet<>();

            for (String arg : args) {
                arg = arg.toLowerCase();
                if (arg.matches("^<:.+:(\\d+)>$")) { // Custom Emote
                    int start = arg.indexOf(":", 2) + 1;
                    String emoteID = arg.substring(start, arg.length() - 1);
                    Emoji emote = FarLands.getDiscordHandler().getEmote(Long.parseLong(emoteID));

                    if (emote == null) {
                        badEmotes.add(arg);
                    } else {
                        emotes.add(emote);
                    }

                } else if (UNICODE_EMOJIS.containsKey(arg.charAt(0)) && arg.length() == 1) { // 1 -> 1️⃣ and so on
                    unicodeEmojis.add(UNICODE_EMOJIS.get(arg.charAt(0)));

                } else if (arg.matches("[\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee]")) { // Match single unicode emoji
                    unicodeEmojis.add(arg);

                } else { // invalid
                    badEmotes.add(arg);

                }
            }

            if (emotes.size() + unicodeEmojis.size() > 0) {
                for (Emoji emote : emotes) {
                    message.addReaction(emote).queue();
                }
                for (String unicodeEmoji : unicodeEmojis) {
                    message.addReaction(Emoji.fromUnicode(unicodeEmoji)).queue();
                }

                List<String> print = emotes.stream().map(Emoji::getAsReactionCode).collect(Collectors.toList());
                print.addAll(unicodeEmojis);
                String msg = "Added reactions: %s".formatted(String.join(", ", print));
                commandMessage.reply(msg).queue();
            }
            if (!badEmotes.isEmpty()) {
                String msg = "Invalid Emotes: %s.\nMake sure to only use default Discord emotes and custom ones from this server.".formatted(String.join(", ", badEmotes));
                commandMessage.reply(msg).queue();
            }
        } catch (IllegalArgumentException e) { // Gotta catch 'em all (Lots of things throw this, so just catch them all lol)
            return false;
        }

        return true;
    }

    public static Message getReplyMessage(Message source) {
        if (source.getReferencedMessage() == null) return null;
        return source.getReferencedMessage();
    }

    @Override
    public boolean requiresMessageID() {
        return true;
    }

}
