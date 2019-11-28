package net.farlands.odyssey.mechanic;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.player.CommandMessage;
import net.farlands.odyssey.command.staff.CommandStaffChat;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.mechanic.anticheat.AntiCheat;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Chat extends Mechanic {
    public static final List<ChatColor> ILLEGAL_COLORS = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    private static final List<String> DISCORD_CHARS = Arrays.asList("_", "~", "*", ":", "`", "|", ">");
    private static final MessageFilter MESSAGE_FILTER = new MessageFilter();
    private final List<BaseComponent[]> rotatingMessages;

    Chat() {
        this.rotatingMessages = new ArrayList<>();
    }

    public void addRotatingMessage(String message) {
        rotatingMessages.add(TextUtils.format(message));
    }

    private void scheduleRotatingMessages() {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            int messageCount = rotatingMessages.size(), rotatingMessageGap = FarLands.getFLConfig().getRotatingMessageGap() * 60 * 20;
            for(int i = 0; i < messageCount; ++i) {
                BaseComponent[] message = rotatingMessages.get(i);
                Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(player ->
                        player.spigot().sendMessage(message)), i * rotatingMessageGap + 600, messageCount * rotatingMessageGap);
            }
        }, 5L);
    }

    @Override
    public void onStartup() {
        FarLands.getFLConfig().getRotatingMessages().stream().map(TextUtils::format).forEach(rotatingMessages::add);
        // Wait for any dynamically added messages to be registered
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), this::scheduleRotatingMessages, 15L * 20L);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        FLPlayer flp = FarLands.getPDH().getFLPlayer(event.getPlayer());
        if(flp.isVanished()) {
            event.setJoinMessage(null);
            FarLands.broadcastStaff(ChatColor.GRAY + event.getPlayer().getName() + " joined silently.");
        }else{
            event.setJoinMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                    ChatColor.RESET + flp.getRank().getNameColor() + flp.getUsername() + ChatColor.YELLOW + " has joined.");
            FarLands.getDiscordHandler().sendMessage("ingame", event.getJoinMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FLPlayer flp = FarLands.getPDH().getFLPlayer(event.getPlayer());
        FarLands.getDataHandler().getRADH().delete("replytoggle", event.getPlayer().getName());
        if(flp.isVanished())
            event.setQuitMessage(null);
        else{
            event.setQuitMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + " > " +
                    ChatColor.RESET + flp.getRank().getNameColor() + flp.getUsername() + ChatColor.YELLOW + " has left.");
            FarLands.getDiscordHandler().sendMessage("ingame", event.getQuitMessage());
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FarLands.getDiscordHandler().sendMessage("ingame", event.getDeathMessage());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) // Process this more superficial, non-critical stuff last
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        FLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        
        spamUpdate(player, event.getMessage());
        if(flp.isMuted()) {
            flp.getCurrentMute().sendMuteMessage(player);
            FarLands.broadcastStaff(TextUtils.format("&(red)[MUTED] %0: &(gray)%1", event.getPlayer().getName(),
                    event.getMessage()));
            return;
        }
        chat(flp, player, event.getMessage());
    }

    public static void chat(FLPlayer senderFlp, Player sender, String message) {
        Rank rank = senderFlp.getRank(),
                displayedRank = senderFlp.isTopVoter() && !rank.isStaff() ? Rank.VOTER : rank;
        chat(senderFlp, sender, message.trim(), displayedRank.getColor() + "" + (displayedRank.isStaff() ? ChatColor.BOLD : "") +
                displayedRank.getSymbol() + displayedRank.getNameColor() + " " + senderFlp.getDisplayName() + ": " + ChatColor.WHITE);
    }

    public static void chat(FLPlayer senderFlp, Player sender, String message, String displayPrefix) {
        if (!senderFlp.getRank().isStaff() && MESSAGE_FILTER.autoCensor(message)) {
            // Make it seem like the message went through for the sender
            sender.sendMessage(String.format(displayPrefix + " %s: " + ChatColor.WHITE + "%s",
                    sender.getDisplayName(), applyColorCodes(message)));
            FarLands.broadcastStaff(String.format(ChatColor.RED + "[AUTO-CENSOR] %s: " + ChatColor.GRAY + "%s",
                    sender.getDisplayName(), applyColorCodes(message)), "alerts");
            return;
        }
        
        message = applyColorCodes(senderFlp.getRank(), message);
        if (message.substring(0, 1).equals("!")) {
            if (message.length() <= 1)
                return;
            message = message.substring(1);
        } else {
            if (FarLands.getDataHandler().getRADH().retrieveBoolean("staffchat", sender.getUniqueId().toString())) {
                FarLands.getCommandHandler().getCommand(CommandStaffChat.class).execute(sender, new String[] {message});
                return;
            }
            Player recipient = (Player)FarLands.getDataHandler().getRADH().retrieve("replytoggle", sender.getName());
            if (recipient != null) {
                if(recipient.isOnline()) {
                    CommandMessage.sendMessage(recipient, sender, message);
                    return;
                }else{
                    sender.sendMessage(ChatColor.RED + recipient.getName() + " is no longer online, your reply toggle has been turned off.");
                    FarLands.getDataHandler().getRADH().delete("replytoggle", sender.getName());
                }
            }
        }
        if (flooding(message)) {
            sender.sendMessage(ChatColor.RED + "Please do not flood the chat");
            FarLands.broadcastStaff(TextUtils.format("&(red)[FLOOD] %0: &(gray)%1", sender.getName(), message), "alerts");
            return;
        }
        
        final String fmessage = displayPrefix + limitCaps(message),
                censorMessage = displayPrefix + Chat.getMessageFilter().censor(limitCaps(message));
        Bukkit.getOnlinePlayers().stream().map(FarLands.getPDH()::getFLPlayer).filter(flp -> !flp.isIgnoring(senderFlp.getUuid()))
                .forEach(flp -> {
                    Player player = flp.getOnlinePlayer();
                    if (flp.isCensoring())
                        player.sendMessage(censorMessage);
                    else
                        player.sendMessage(fmessage);
                });
        FarLands.getDiscordHandler().sendMessage("ingame", fmessage);
        Bukkit.getConsoleSender().sendMessage(fmessage);
    }
    
    public void spamUpdate(Player player, String message) {
        if(Rank.getRank(player).isStaff())
            return;
        UUID uuid = player.getUniqueId();
        RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
        double strikes = (double)radh.retrieveAndStoreIfAbsent(0.0, "spam", uuid.toString());
        if(Utils.deltaEquals(strikes, 0.0, 1e-8))
            radh.setCooldown(160L, "spamCooldown", uuid.toString(), () -> radh.delete("spam", uuid.toString()));
        strikes += 1 + message.length() / 80.0;
        if(strikes >= 7.0) {
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                player.kickPlayer("Kicked for spam. Repeating this offense could result in a ban.");
                AntiCheat.broadcast(player.getName() + " was kicked for spam.", true);
            });
            radh.delete("spam", uuid.toString());
            return;
        }
        radh.store(strikes, "spam", uuid.toString());
    }
    
    private static boolean flooding(String message) {
        if (message.length() <= 16)
            return false;
        float spaces = 1, row = 0, maxRow = 0;
        char last = ' ';
        for (char c : message.toLowerCase().toCharArray()) {
            if (c == ' ')
                ++spaces;
            else if (c == last)
                row += c == '.' ? .5f : 1;
            else {
                last = c;
                if (row > 16)
                    return true;
                if (row > maxRow)
                    maxRow = row;
                row = c == '.' ? .5f : 1;
            }
        }
        if (row > 16)
            return true;
        if (row > maxRow)
            maxRow = row;
        return maxRow / message.length() > 1f / 3; // || spaces / message.length() < 1f / 16;
    }

    private static String limitCaps(String message) {
        if (message.length() < 4)
            return message;
        float uppers = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c))
                ++uppers;
        }
        return uppers / message.length() >= 1f / 3 ? message.toLowerCase() : message;
    }
    
    public static MessageFilter getMessageFilter() {
        return MESSAGE_FILTER;
    }

    public static String applyDiscordFilters(String message) {
        for(String c : DISCORD_CHARS)
            message = message.replaceAll('\\' + c, "\\\\" + c);
        message = message.replaceAll("@", "\\\\@ ");
        return removeColorCodes(message);
    }

    public static String removeColorCodes(String message) {
        StringBuilder sb = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for(int i = 0;i < chars.length;++ i) {
            if(chars[i] == '&' || chars[i] == ChatColor.COLOR_CHAR) {
                ++ i;
                continue;
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static String applyColorCodes(Rank rank, String message) {
        if(rank == null || rank.specialCompareTo(Rank.ADEPT) < 0)
            return removeColorCodes(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        if(!rank.isStaff()) {
            for(ChatColor color : ILLEGAL_COLORS)
                message = message.replaceAll(ChatColor.COLOR_CHAR + Character.toString(color.getChar()), "");
        }
        return message;
    }

    public static String applyColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static class MessageFilter {
        private final Map<String, Boolean> words;
        private final List<String> replacements;
        private final Random rng;

        private void init() {
            try {
                Stream.of(FarLands.getDataHandler().getDataTextFile("censor-dict.txt").split("\n")).forEach(line -> {
                    boolean ac = line.startsWith("AC:");
                    words.put(ac ? line.substring(3) : line, ac);
                });
                replacements.addAll(Arrays.asList(FarLands.getDataHandler().getDataTextFile("censor-replacements.txt").split("\n")));
            }catch(IOException ex) {
                FarLands.error("Failed to load words and replacements for message filter words.");
                throw new RuntimeException(ex);
            }
        }

        MessageFilter() {
            this.words = new HashMap<>();
            this.replacements = new ArrayList<>();
            this.rng = new Random();
            init();
        }

        public String censor(String s) {
            String censored = s.toLowerCase();
            for(String word : words.keySet())
                censored = censored.replaceAll("(^|\\W)\\Q" + word + "\\E($|\\W)", getRandomReplacement());
            return Utils.matchCase(s, censored);
        }

        public boolean isProfane(String s) {
            return !s.equals(censor(s));
        }

        public boolean autoCensor(String s) {
            String censored = s.toLowerCase();
            for(String word : words.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList()))
                censored = censored.replaceAll("(^|\\W)\\Q" + word + "\\E($|\\W)", " ");
            return !s.equalsIgnoreCase(censored);
        }

        String getRandomReplacement() {
            return ' ' + replacements.get(rng.nextInt(replacements.size())) + ' ';
        }
    }
}
