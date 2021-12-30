package net.farlands.sanctuary.command.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.command.CommandSender;

public class CommandAddReactions extends DiscordCommand {

  public CommandAddReactions() {
    super(Rank.JR_BUILDER, "Add reactions to a message for voting.",
        "/addreactions <message-id> <reactions>", "addreactions", "addreaction");
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public boolean execute(CommandSender sender, String[] args) throws Exception {
    if (args.length < 3) {
      return false;
    }
    String[] parts = args[0].split(":");
    String channelID = parts[0];
    String commandMessageID = parts[1];
    // not like we need it :P
    // Is command handling that fucked lmao *sigh*
    // I wonder if the alias is fucking with it -- Yes

    // Command handling is a mess so yeah the way you're doing is probably safer
    // Just how this is? my line indents is diff too :P

    // Let's try this, would you build it and send to me -- yeah :vomit: windows meh
    TextChannel channel = FarLands.getDiscordHandler().getGuild().getTextChannelById(channelID);
    Message message = channel.getHistory().getMessageById(args[1]); // This is <message-id>

    // This is the best way to do it, I believe.
//    FarLands.getDiscordHandler()
//        .getGuild()
//        .getTextChannelById(channelID)
//        .retrieveMessageById(args[1])
//        .queue(msg -> {
//        if(msg == null) {
//          sender.sendMessage(ComponentColor.red("Unable to find message with id '%s'", args[1]));
//          return;
//        }
//
//        for (int i = 2; i < args.length; i++) {
//          msg.addReaction(args[i]).queue();
//        }
//    });
    if (message == null) {
      sender.sendMessage(ComponentColor.red("Unable to find message with id '%s'", args[1]));
      return true;
    }

    for (int i = 2; i < args.length; i++) {
      message.addReaction(args[i]).queue();
    }
    channel.retrieveMessageById().queue(m -> m.addReaction("\uD83D\uDC4D"));

    return true;
  }

  @Override
  public boolean requiresMessageID() {
    return true;
  }
}
