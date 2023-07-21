package net.farlands.sanctuary.command.player;

import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.VoteRewards;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kicas.rp.command.TabCompleterBase.filterStartingWith;

public class CommandVoteRewards extends PlayerCommand {

    public CommandVoteRewards() {
        super(
            CommandData.simple(
                    "voterewards",
                    "/voterewards [all|none|vp-only]",
                    "Enable or disable receiving vote rewards."
                )
                .category(Category.PLAYER_SETTINGS_AND_INFO)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length == 0) {
            return info(sender, "Vote rewards set to {:aqua}", flp.voteRewardsToggle);
        } else if (args.length > 1) {
            return false;
        }

        VoteRewards toggle = Utils.valueOfFormattedName(args[0], VoteRewards.class);

        if (toggle == null) {
            return error(sender, "Invalid value, expected one of: {}", (Object) VoteRewards.values());
        }

        flp.voteRewardsToggle = toggle;

        return success(sender, "Updated vote rewards to {:aqua}.", toggle);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location loc) throws IllegalArgumentException {
        return args.length == 1
            ? filterStartingWith(args[0], Arrays.stream(VoteRewards.values()).map(Utils::formattedName))
            : Collections.emptyList();
    }
}
