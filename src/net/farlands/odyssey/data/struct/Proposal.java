package net.farlands.odyssey.data.struct;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Proposal {
    private long messageID;
    private long dateEnds;
    private String message;
    private transient boolean resolved;

    public static final String VOTE_YES = "\u2705";
    public static final String VOTE_NO = "\u274C";

    private void init(String issuer) {
        String msg0 = "@everyone A new proposal was issued by **" + issuer + "**:```" + message +
                "```This vote will expire 48 hours after it was issued. Votes required to pass: " +
                ((staffCount() + 1) / 2);
        Message messageObj = FarLands.getDiscordHandler().getChannel("output").sendMessage(msg0).complete();
        messageID = messageObj.getIdLong();
        messageObj.addReaction(VOTE_YES).queue(unused -> messageObj.addReaction(VOTE_NO).queue());
    }

    public Proposal(String issuer, String message) {
        this.dateEnds = System.currentTimeMillis() + 48L * 60L * 60L * 1000L;
        this.message = message;
        this.resolved = false;
        init(issuer);
    }

    Proposal() {
        this.resolved = false;
    }

    public void update() {
        Message messageObj = FarLands.getDiscordHandler().getChannel("output").getMessageById(messageID).complete();
        if(messageObj == null || System.currentTimeMillis() > dateEnds) {
            resolve(2);
            return;
        }
        int votesRequired = (staffCount() + 1) / 2;
        MessageReaction yes = messageObj.getReactions().stream().filter(r -> VOTE_YES.equalsIgnoreCase(r.getReactionEmote().getName()))
                .findAny().orElse(null);
        MessageReaction no = messageObj.getReactions().stream().filter(r -> VOTE_NO.equalsIgnoreCase(r.getReactionEmote().getName()))
                .findAny().orElse(null);
        int yesVotes, noVotes;
        if(yes == null || yes.getCount() == 0) {
            messageObj.addReaction(VOTE_YES).queue();
            yesVotes = 0;
        }else{
            yesVotes = yes.getCount();
            if(yes.getUsers().complete().contains(FarLands.getDiscordHandler().getNativeBot().getSelfUser())) {
                -- yesVotes;
                if(yesVotes > 0)
                    yes.removeReaction().queue();
            }
        }
        if(no == null || no.getCount() == 0) {
            messageObj.addReaction(VOTE_NO).queue();
            noVotes = 0;
        }else{
            noVotes = no.getCount();
            if(no.getUsers().complete().contains(FarLands.getDiscordHandler().getNativeBot().getSelfUser())) {
                -- noVotes;
                if(noVotes > 0)
                    no.removeReaction().queue();
            }
        }
        String contentRaw = messageObj.getContentRaw();
        messageObj.editMessage(contentRaw.substring(0, contentRaw.lastIndexOf(':') + 2) + (votesRequired - yesVotes)).queue();
        if(yesVotes >= votesRequired)
            resolve(0);
        else if(noVotes >= votesRequired)
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
        switch(status) {
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
        FarLands.getDiscordHandler().sendMessageRaw("output", result);
        resolved = true;
    }

    private static int staffCount() {
        try {
            ResultSet rs = FarLands.getPDH().query("SELECT Count(*) FROM playerdata WHERE rank>=" + Rank.JR_BUILDER.ordinal());
            int count = rs.getInt(1);
            rs.close();
            return count;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return 4;
        }
    }
}
