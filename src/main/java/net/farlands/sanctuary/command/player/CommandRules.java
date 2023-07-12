package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Pair;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CommandRules extends Command {

    private static final List<Pair<String, String>> RULES = Arrays.asList(
        new Pair<>("No hacking, cheating, or exploits of any kind.",
                   "This includes, but is not limited to: " +
                   "x-ray clients (including texture packs), fly hacks, PvP hacks, aim-bots, and item duplication. " +
                   "If a game mechanic is exploited to gain an unfair advantage, then it is up to staff discretion " +
                   "on whether or not to act on such an offense.  Note that the exploitation of known bugs, such as " +
                   "exceeding data limits, that pose a risk of crashing the server or other players, or that could " +
                   "potentially corrupt chunk data is strictly prohibited and may result in a ban. Also note that " +
                   "if you report a game-breaking bug, you will not be punished or warned as long as you stop " +
                   "exploiting the bug after the initial report, and as long as your exploration of the bug did not " +
                   "cause damage to the server or other players. Continuing to probe such bugs after reporting them " +
                   "will result in a warning and then a ban."),
        new Pair<>("Slurs, hate speech, sexual and/or NSFW (not safe for work) content are prohibited.", """
            - Speech that derides particular ethnic, religious, or sexual minorities will result in a punishment and potentially a permanent ban from both in-game and discord.
            - Links to pornography, or otherwise sexually-charged content, is strictly prohibited since a large portion of our userbase are minors. Gore and other revolting content is also prohibited.
            - Swearing is allowed on the server within reason. If it is directed toward a particular player, you may be asked to stop by the player or a staff member. If you continue, then you may be muted or banned.
            - The discussion of politics, religion, race, and other hot-button topics is not strictly prohibited, however we tend to frown upon such discussion, as it often causes conflict and arguments. If a staff member asks you to take the discussion to PMs or off the server, you must do so or risk getting muted or banned."""),
        new Pair<>("Respect other players at all times.",
                   "Brigading servers and/or harassing players will not " +
                   "be tolerated. If someone asks you to stop, you should stop. Also, a player violating a rule in " +
                   "a way that negatively affects you does not give you the right to violate the rules in " +
                   "retaliation; in other words: two wrongs do not make right. You must ask permission before " +
                   "destroying or modifying large amounts of land near another player's claim. Similarly, you must " +
                   "ask permission before voting for the server on another player's behalf, sending them items via " +
                   "/package, or transferring pets to them."),
        new Pair<>("Bypassing restrictions to harass or grief* another player is prohibited.", """
            This includes, but is not limited to:
            - Bypassing /ignore to forcibly send messages to another player
            - Scamming or bypassing claim protections to grief builds or steal items
            - Player trapping, killing, or otherwise bypassing the PvP toggle
            *Staff are not responsible for who you trust on your claim(s). If a player griefs a build or steals items on a claim in which they are trusted, staff will not punish that player, restore the build, or return lost items."""),
        new Pair<>("Spam and/or advertisement of other servers or commercial sites is not allowed.", """
            Types of spam include, but are not limited to:
            - Repeating a particular word, phrase, or character cluster
            - Flooding chat with walls of text
            - Advertising media unrelated to our server
            - Hopping between voice channels rapidly and/or spamming loud, annoying, or obnoxious noises (discord only)"""),
        new Pair<>("Mechanisms that induce excessive server lag are not allowed.",
                   "This includes large, " +
                   "complicated redstone mechanisms that run on a clock, overpopulated animal farms, and chunk " +
                   "loaders. The use of an alternate account to load a farm while the main account performs other " +
                   "tasks on the server is considered chunk loading and may result in a punishment for both " +
                   "accounts. Some more extraneous violations of this rule include bypassing the AFK checker and " +
                   "building auto-farms in public warps so that other players unknowingly load the farm while you " +
                   "are not in the area."),
        new Pair<>("You are responsible for managing your own account.",
                   "We will not pardon a punishment if a " +
                   "sibling or co-user of an account breaks the rules and causes the account to get banned. It is " +
                   "your responsibility to ensure that all users of your account comply with the rules."),
        new Pair<>("Using an alternate account to evade a ban will result in a permanent ban of both accounts.",
                   "In other words: ban evasion results in an IP ban. This is only applicable to new accounts: if " +
                   "you and a sibling have both joined the server before, and the sibling gets banned, then " +
                   "you will still be able to play on the server."),
        new Pair<>("Respect all staff and staff decisions.",
                   "All players are allowed to appeal a ban, however, " +
                   "openly harassing or ridiculing staff will not be tolerated."),
        new Pair<>("Rules are subject to common sense.",
                   "Any attempt to use loopholes or exploits to violate " +
                   "the spirit of these rules is prohibited and subject to enforcement.")
    );

    public CommandRules() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Look at the server rules.", "/rules [ruleNumber]", "rules");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof DiscordSender ds) {
                ds.sendMessage(
                    "View the rules in <#%s>".formatted(
                        FarLands.getDiscordHandler().getNativeBot()
                            .getTextChannelsByName("rules", true)
                            .get(0)
                            .getId()
                    ),
                    false
                );
                return true;
            }
            TextComponent.Builder rules = Component.text().content("Server rules (click to view full rule):");
            for (int i = 0; i < RULES.size(); i++) {
                Pair<String, String> rule = RULES.get(i);
                rules.append(
                    ComponentUtils.command(
                        "/rules " + (i + 1),
                        ComponentColor.gray("\n{}. {:white}", i + 1, rule.getFirst()),
                        ComponentColor.gray("View rule " + (i + 1))
                    )
                );
            }
            sender.sendMessage(rules.build());
        } else {
            int rule;
            try {
                rule = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid rule number: " + args[0]);
                return true;
            }

            if (rule < 1 || rule > RULES.size()) {
                sender.sendMessage(ChatColor.RED + "Rule numbers must be between 1 and " + RULES.size());
                return true;
            }

            Pair<String, String> ruleText = RULES.get(rule - 1);

            sender.sendMessage(
                ComponentColor.gold(rule + ". " + ruleText.getFirst() + " ")
                    .append(ComponentColor.white(ruleText.getSecond()))
            );
        }

        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false)
            .addOption(OptionType.INTEGER, "rule", "Rule to get more information on", false, true);
    }
}
