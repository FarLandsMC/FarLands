package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.TextUtils;


import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandPackageAccept extends PlayerCommand {

    public CommandPackageAccept() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Accept or decline packages from other players.",
                "/paccept|pdecline <player>", true, "paccept", "pdecline");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        List<Package> packages = FarLands.getDataHandler().getPackages(sender.getUniqueId());
        if (packages.isEmpty()) {
            TextUtils.sendFormatted(sender, "&(red)You do not have any pending packages.");
            return true;
        }

        String packageID = "";
        if (packages.size() <= 1) {
            packageID = packages.get(0).senderName;
        }

        if (!packageID.isEmpty()) {
            if (args.length > 1 && !args[1].isEmpty())
                packageID = args[1];
            else {
                if(args.length == 1 && packages.size() == 1){
                    FLUtils.giveItem(sender, packages.get(0).item, true);
                    TextUtils.sendFormatted(
                            sender, "&(gold)Receiving package from {&(aqua){%0}}.",
                            packages.get(0).senderName
                    );
                    packages.remove(0);
                    return true;
                }else {
                    TextUtils.sendFormatted(sender, "&(red)Please specify the package sender.");
                    return true;
                }
            }
        }

        for (Package lPackage : packages) {
            if (Chat.removeColorCodes(lPackage.senderName.replaceAll("\\{+|}+", "")).equalsIgnoreCase(packageID)) {
                if ("paccept".equalsIgnoreCase(args[0])) {
                    TextUtils.sendFormatted(
                            sender, "&(gold)Receiving package from {&(aqua){%0}}.",
                            lPackage.senderName
                    );
                    final String message = lPackage.message;
                    if (message != null && !message.isEmpty())
                        TextUtils.sendFormatted(sender, "&(gold)Item {&(aqua)%0} was sent with the following message {&(aqua)%1}",
                                FLUtils.itemName(lPackage.item), message);
                    FLUtils.giveItem(sender, lPackage.item, true);
                } else {
                    TextUtils.sendFormatted(
                            sender, "&(gold)Returning package from {&(aqua){%0}}.",
                            lPackage.senderName
                    );
                    OfflineFLPlayer packageSenderFlp = FarLands.getDataHandler().getOfflineFLPlayer(lPackage.senderUuid);
                    FarLands.getDataHandler().addPackage(packageSenderFlp.uuid,
                            new Package(null, "FarLands Packaging Service",
                            lPackage.item, "Return To Sender", true)
                    );
                    if (packageSenderFlp.isOnline())
                        packageSenderFlp.getSession().givePackages();
                }
                packages.remove(lPackage);
                return true;
            }
        }

        TextUtils.sendFormatted(sender, "&(red)The package sender specified was not correct.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!(sender instanceof Player))
            return Collections.emptyList();
        Player player = (Player)sender;
        return TabCompleterBase.filterStartingWith(args.length > 1 ? args[1] : "",
                FarLands.getDataHandler().getPackages(player.getUniqueId()).stream()
                        .map(p -> Chat.removeColorCodes(p.senderName.replaceAll("\\{+|}+", ""))));
    }
}
