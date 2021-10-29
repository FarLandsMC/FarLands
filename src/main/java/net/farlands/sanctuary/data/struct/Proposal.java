package net.farlands.sanctuary.data.struct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.md_5.bungee.api.ChatColor;

/**
 * Handles a proposal.
 */
public class Proposal {
    private long messageID;
    private long dateEnds;
    private String message;
    private String issuer;
    private transient boolean resolved;

    public static final String VOTE_YES = "\u2705";
    public static final String VOTE_NO = "\u274C";

    private void init(String issuer) {

        Message messageObj = FarLands.getDiscordHandler().getChannel(DiscordChannel.NOTEBOOK).sendMessage(
                messageEmbed(message, issuer, ((staffCount() + 1) / 2))
        ).complete();
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, "@everyone");
        messageID = messageObj.getIdLong();
        messageObj.addReaction(VOTE_YES).queue(unused -> messageObj.addReaction(VOTE_NO).queue());
    }

    public Proposal(String issuer, String message) {
        this.dateEnds = System.currentTimeMillis() + 48L * 60L * 60L * 1000L;
        this.message = message;
        this.issuer = issuer;
        this.resolved = false;
        init(issuer);
    }

    Proposal() {
        this.resolved = false;
    }

    public void update() {
        Message messageObj = FarLands.getDiscordHandler().getChannel(DiscordChannel.NOTEBOOK).retrieveMessageById(messageID).complete();
        if (messageObj == null || System.currentTimeMillis() > dateEnds) {
            resolve(2);
            return;
        }
        int votesRequired = (staffCount() + 1) / 2;
        MessageReaction yes = messageObj.getReactions().stream().filter(r -> VOTE_YES.equalsIgnoreCase(r.getReactionEmote().getName()))
                .findAny().orElse(null);
        MessageReaction no = messageObj.getReactions().stream().filter(r -> VOTE_NO.equalsIgnoreCase(r.getReactionEmote().getName()))
                .findAny().orElse(null);
        int yesVotes, noVotes;
        if (yes == null || yes.getCount() == 0) {
            messageObj.addReaction(VOTE_YES).queue();
            yesVotes = 0;
        } else {
            yesVotes = yes.getCount();
            if (yes.retrieveUsers().complete().contains(FarLands.getDiscordHandler().getNativeBot().getSelfUser())) {
                --yesVotes;
                if (yesVotes > 0)
                    yes.removeReaction().queue();
            }
        }
        if (no == null || no.getCount() == 0) {
            messageObj.addReaction(VOTE_NO).queue();
            noVotes = 0;
        } else {
            noVotes = no.getCount();
            if (no.retrieveUsers().complete().contains(FarLands.getDiscordHandler().getNativeBot().getSelfUser())) {
                --noVotes;
                if (noVotes > 0)
                    no.removeReaction().queue();
            }
        }

        messageObj.editMessage(messageEmbed(message, issuer, (votesRequired - yesVotes))).queue();

        if (yesVotes >= votesRequired)
            resolve(0);
        else if (noVotes >= votesRequired)
            resolve(1);
    }

    public boolean isResolved() {
        return resolved;
    }

    public long getMessageID() {
        return messageID;
    }

    // status 0: passed, 1: declined, 2: expired
    private void resolve(int status) {
        String result = "@everyone Vote `" + message.substring(0, Math.min(message.length(), 50)).trim() + "...` has ";
        switch (status) {
            case 0:
                result += "passed. " + VOTE_YES;
                break;
            case 1:
                result += "failed. " + VOTE_NO;
                break;
            default:
                result += "expired. " + VOTE_NO;
                break;
        }
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, result);
        resolved = true;
    }

    private static int staffCount() {
        return (int) FarLands.getDataHandler().getOfflineFLPlayers().stream().filter(flp -> flp.rank.isStaff()).count();
    }

    private static MessageEmbed messageEmbed(String message, String issuer, int amountToPass) {
        return new EmbedBuilder()
                .setColor(ChatColor.YELLOW.getColor())
                .setTitle("New Proposal")
                .setDescription("```" + message + "```")
                .addField("Issued By", issuer, false)
                .addField("Votes to Pass", "" + amountToPass, false)
                .build();
    }
}
