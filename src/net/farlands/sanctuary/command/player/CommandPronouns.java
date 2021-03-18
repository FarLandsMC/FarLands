package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Pronouns;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CommandPronouns extends Command {
    public CommandPronouns() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Register your pronouns and configure their settings",
                "/pronouns [set|show-on-discord] [subject/object|true|false]", "pronouns", "pronoun");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length == 0) {
            if (flp.pronouns == null || flp.pronouns.subject == null) {
                TextUtils.sendFormatted(sender, "&(red)Your pronouns are not set. You can set them with {&(aqua)/pronouns set <subject/object>}");
                return true;
            }
            TextUtils.sendFormatted(sender, "&(green)Your pronouns are currently set to %0", flp.pronouns.toString());
            return true;
        }

        switch (args[0]) {
            case "set":
                if (args.length < 2) return false;
                return setPronouns(flp, args[1], sender);
            case "show-on-discord":
                if (args.length < 2) return false;
                return setShowOnDiscord(flp, args[1], sender);
            case "reset":
                flp.pronouns = null;
                flp.updateDiscord();
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return TabCompleterBase.filterStartingWith(args[0], Arrays.asList("set", "show-on-discord", "reset"));

            case 2:
                switch (args[0].toLowerCase()) {
                    case "set":
                        if (!args[1].contains("/")) {
                            return TabCompleterBase.filterStartingWith(args[1],
                                    Arrays.stream(Pronouns.SubjectPronoun.VALUES)
                                            .map(sp -> sp.getHumanName() + (sp.hasNoObject() ? "" : "/"))
                            );
                        }
                        if (StringUtils.countMatches(args[1], "/") == 1) {
                            String subject = args[1].split("/")[0];
                            return TabCompleterBase.filterStartingWith(args[1],
                                    Arrays.stream(Pronouns.ObjectPronoun.VALUES)
                                            .map(op ->
                                                    subject + "/" + op.getHumanName()
                                            )
                            );
                        }
                        return Collections.singletonList(args[1]);
                    case "show-on-discord":
                        return Arrays.asList("true", "false");
                    default:
                        return Collections.emptyList();
                }
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Set the pronouns of a player
     *
     * @param flp    player to change
     * @param arg    The value to use for determining the pronouns
     * @param sender Used for player feedback
     * @return If the command ran successfully
     */
    private boolean setPronouns(OfflineFLPlayer flp, String arg, CommandSender sender) {

        if (!arg.contains("/")) {
            Pronouns.SubjectPronoun sp = FLUtils.safeValueOf(Pronouns.SubjectPronoun::findByHumanName, arg);
            if (sp == null) {
                TextUtils.sendFormatted(sender, "&(red)Unknown pronoun. If this is one that you use, please contact a staff member.");
                return true;
            }
            if (sp.hasNoObject()) {
                flp.pronouns = new Pronouns(sp, null, flp.pronouns != null && flp.pronouns.showOnDiscord);
            }
        } else {

            String subjectString = arg.split("/")[0];
            Pronouns.SubjectPronoun sp = FLUtils.safeValueOf(Pronouns.SubjectPronoun::findByHumanName, subjectString);

            String objectString = arg.split("/")[1];
            Pronouns.ObjectPronoun op = FLUtils.safeValueOf(Pronouns.ObjectPronoun::findByHumanName, objectString);

            if (sp == null || op == null) {
                TextUtils.sendFormatted(sender, "&(red)Unknown pronoun. If this is one that you use, please contact a staff member.");
                return true;
            }

            flp.pronouns = new Pronouns(sp, op, flp.pronouns != null && flp.pronouns.showOnDiscord);
        }

        flp.updateDiscord();

        TextUtils.sendFormatted(sender,
                "&(green)Successfully updated your pronouns to {&(aqua)%0}!\n" +
                        (
                            flp.isDiscordVerified() ?
                            "You have show-on-discord set to %1, if you want it %2, run $(hovercmd,%3,&(aqua)Run Command,&(aqua)%3)." :
                            ""
                        ),
                flp.pronouns.toString(false),
                flp.pronouns.showOnDiscord,
                flp.pronouns.showOnDiscord ? "disabled" : "enabled",
                "/pronouns show-on-discord " + (flp.pronouns.showOnDiscord ? "false" : "true")
        );

        return true;
    }

    /**
     * Set the `showOnDiscord` status of a player
     *
     * @param flp    player to change
     * @param arg    The value to use for determining the pronouns
     * @param sender Used for player feedback
     * @return If the command ran successfully
     */
    private boolean setShowOnDiscord(OfflineFLPlayer flp, String arg, CommandSender sender) {

        boolean value;
        switch (arg.toLowerCase()) {
            case "true":
            case "on":
                value = true;
                break;
            case "false":
            case "off":
                value = false;
                break;
            default:
                TextUtils.sendFormatted(sender, "&(red)Usage: /pronouns show-on-discord <true|false>");
                return true;
        }

        if (flp.pronouns == null) {
            flp.pronouns = new Pronouns(null, null, value);
        } else {
            flp.pronouns.showOnDiscord = value;
        }

        flp.updateDiscord();

        TextUtils.sendFormatted(sender, "&(green)Set show-on-discord to %0!", flp.pronouns.showOnDiscord ? "enabled" : "disabled");

        return true;
    }
}
