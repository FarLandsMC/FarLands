package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.TimeInterval;
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

    public boolean isActive(int index) {
        int hours = hours(index);
        return hours < 0 || System.currentTimeMillis() < (dateIssued + hours * 60L * 60L * 1000L);
    }

    public String getRawMessage() {
        return message;
    }

    public String generateBanMessage(int index, boolean totalTime) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.RED).append("You are banned from FarLands!\n");
        sb.append("Reason: ").append(ChatColor.GOLD).append(punishmentType.getHumanName()).append('\n').append(ChatColor.RED);
        if(message != null && !message.isEmpty())
            sb.append("Staff Message: ").append(ChatColor.GOLD).append(message).append('\n').append(ChatColor.RED);
        long time = totalTime ? totalTime(index) : timeRemaining(index);
        String exp = time >= 0 ? TimeInterval.formatTime(time, true, TimeInterval.MINUTE) : "Never";
        sb.append("Expires: ").append(ChatColor.GOLD).append(exp).append('\n').append(ChatColor.RED);
        sb.append("Appeal on Discord: ").append(FarLands.getFLConfig().appealsLink).append(' ');
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
        return Utils.formattedName(punishmentType) + ":" + dateIssued;
    }

    @Override
    public String toString() {
        return punishmentType.getHumanName() + " (" + SDF.format(new Date(dateIssued)) + ")";
    }

    public enum PunishmentType {
        SPAM("Spamming"),
        HARASSMENT("Harassment"),
        ADVERTISING("Advertising"),
        AFK_BYPASS("Bypassing AFK"),
        TOXICITY("Toxicity"),
        SLURS("Slurs"),
        THREATS("Threats"),
        ADULT_CONTENT("Adult Content"),
        GENERAL_HACKS("General Hacks"),
        FLYING("Flying"),
        X_RAY("X-Ray"),
        MINOR_GRIEF("Griefing (Minor)"),
        MAJOR_GRIEF("Griefing (Major)"),
        PVP_BYPASS("Bypassing PvP Toggle"),
        BAN_EVASION("Ban Evasion", true),
        PERMANENT("The Ban Hammer has Spoken!", true),
        BOT_USE("Bot Use", true);

        private final String humanName;
        private final boolean permanent;

        public static final PunishmentType[] VALUES = values();

        PunishmentType(String humanName, boolean permanent) {
            this.humanName = humanName;
            this.permanent = permanent;
        }

        PunishmentType(String humanName) {
            this(humanName, false);
        }

        public String getHumanName() {
            return humanName;
        }

        public boolean isPermanent() {
            return permanent;
        }
    }
}
