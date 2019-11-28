package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Punishment {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");

    private PunishmentType punishmentType;
    private long dateIssued;
    private String message;

    public static final int[] PUNISHMENT_DURATIONS = {24, 72, 168}; // In hours

    public Punishment(PunishmentType punishmentType, long dateIssued, String message) {
        this.punishmentType = punishmentType;
        this.dateIssued = dateIssued;
        this.message = message;
    }

    public Punishment(PunishmentType punishmentType, String message) {
        this(punishmentType, System.currentTimeMillis(), message);
    }

    public long getDateIssued() {
        return dateIssued;
    }

    public boolean hasExpired(int index) {
        int hours = hours(index);
        return hours >= 0 && System.currentTimeMillis() > (dateIssued + hours * 60L * 60L * 1000L);
    }

    public String getRawMessage() {
        return message;
    }

    public String generateBanMessage(int index, boolean totalTime) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.RED).append("You are banned from FarLands!\n");
        sb.append("Reason: ").append(ChatColor.GOLD).append(punishmentType.getFormattedName()).append('\n').append(ChatColor.RED);
        if(message != null && !message.isEmpty())
            sb.append("Staff Message: ").append(ChatColor.GOLD).append(message).append('\n').append(ChatColor.RED);
        long time = totalTime ? totalTime(index) : timeRemaining(index);
        String exp = time >= 0 ? TimeInterval.formatTime(time, true, TimeInterval.MINUTE) : "Never";
        sb.append("Expires: ").append(ChatColor.GOLD).append(exp).append('\n').append(ChatColor.RED);
        sb.append("Appeal on Discord: ").append(FarLands.getFLConfig().getAppealsLink()).append(' ');
        return sb.toString();
    }

    public PunishmentType getType() {
        return punishmentType;
    }

    public long timeRemaining(int index) {
        int hours = hours(index);
        return hours >= 0 ? (dateIssued + hours * 60L * 60L * 1000L) - System.currentTimeMillis() : -1L;
    }

    public long totalTime(int index) {
        int hours = hours(index);
        return hours >= 0 ? hours * 60L * 60L * 1000L : -1L;
    }

    private int hours(int index) {
        return punishmentType.isPermanent() ? -1 : (index < PUNISHMENT_DURATIONS.length ? PUNISHMENT_DURATIONS[index] : -1);
    }

    public String toUniqueString() { // Used by the evidence locker serialization system
        return punishmentType.getAlias() + ":" + dateIssued;
    }

    @Override
    public String toString() {
        return punishmentType.getFormattedName() + " (" + SDF.format(new Date(dateIssued)) + ")";
    }

    public enum PunishmentType {
        SPAM("Spamming"),
        HARASSMENT("Harassment"),
        ADVERTISING("Advertising"),
        AFK_BYPASS("Bypassing AFK", "afk-bypass"),
        TOXICITY("Toxicity"),
        SLURS("Slurs"),
        THREATS("Threats"),
        ADULT_CONTENT("Adult Content", "adult-content"),
        GENERAL_HACKS("General Hacks", "general-hacks"),
        FLYING("Flying"),
        XRAY("X-Ray", "x-ray"),
        GRIEF_MINOR("Griefing (Minor)", "minor-grief"),
        GRIEF_MAJOR("Griefing (Major)", "major-grief"),
        PVP_BYPASS("Bypassing PvP Toggle", "pvp-bypass"),
        BAN_EVASION("Ban Evasion", "ban-evasion", true),
        PERMANENT("The Ban Hammer has Spoken!", true),
        BOT_USE("Bot Use", "bot-use", true);

        private final String formattedName;
        private final String alias;
        private final boolean permanent;

        public static final PunishmentType[] VALUES = values();

        PunishmentType(String formattedName, String alias, boolean permanent) {
            this.formattedName = formattedName;
            this.alias = alias.toLowerCase();
            this.permanent = permanent;
        }

        PunishmentType(String formattedName, String alias) {
            this(formattedName, alias, false);
        }

        PunishmentType(String formattedName, boolean permanent) {
            this.formattedName = formattedName;
            this.alias = toString().toLowerCase();
            this.permanent = permanent;
        }

        PunishmentType(String formattedName) {
            this(formattedName, false);
        }

        public String getFormattedName() {
            return formattedName;
        }

        public String getAlias() {
            return alias;
        }

        public boolean isPermanent() {
            return permanent;
        }

        public static PunishmentType specialValueOf(String name) {
            for(PunishmentType pt : VALUES) {
                if(name.equalsIgnoreCase(pt.getAlias()))
                    return pt;
            }
            return null;
        }
    }
}
