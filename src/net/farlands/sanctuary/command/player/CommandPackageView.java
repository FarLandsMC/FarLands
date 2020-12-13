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
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandPackageView extends PlayerCommand {
    public CommandPackageView() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "View packages sent to you by other players.", "/packageview [player]", "packageview", "packagesview");
    }

    @Override
    public boolean execute(Player sender, String[] args) {

        OfflineFLPlayer viewFlp = args.length >= 1 ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]) : null;
        List<Package> packages = FarLands.getDataHandler().getPackages(sender.getUniqueId());

        if(packages.isEmpty()){
            TextUtils.sendFormatted(sender, "&(red)You do not have any pending packages.");
            return true;
        }

        if(viewFlp == null && args.length >= 1){
            TextUtils.sendFormatted(sender, "&(red)Unknown player, \"" + args[0] + "\"");
            return true;
        }
        StringBuilder message = new StringBuilder("&(gold)- %0 ${inflect,noun,0,package} ");

        if(args.length == 0){
            message.append("-");
        }else{
            String accept = "${hovercmd,/paccept %2,&(dark_green)Accept %0 ${inflect,noun,0,package},&(dark_green,bold)[Accept]}";
            String decline = "${hovercmd,/pdecline %2,&(dark_red)Decline %0 ${inflect,noun,0,package},&(dark_red,bold)[Decline]}";
            message.append("from {&(green)%1} - " + accept + " " + decline);
        }


        List<Package> validPackages = new ArrayList<>();
        for (Package lPackage : packages) {

            String json = CraftItemStack.asNMSCopy(lPackage.item).save(new NBTTagCompound()).toString();

            String name;
            if (lPackage.item.getItemMeta().getDisplayName().equals("")) {
                name = lPackage.item.getType().name().replace("_", " ");
                name = WordUtils.capitalizeFully(name);
            } else {
                name = lPackage.item.getItemMeta().getDisplayName();
            }

            String itemDisplay = lPackage.item.getAmount() + " x ["  + "$(item," + json + "," + name + ")]";
            OfflineFLPlayer packageSender = FarLands.getDataHandler().getOfflineFLPlayer(lPackage.senderUuid);
            String formattedSenderName = Chat.removeColorCodes(lPackage.senderName.replaceAll("\\{+|}+", ""));

            if (args.length >= 1 &&
                    FarLands.getDataHandler().getOfflineFLPlayer(args[0]).getDisplayName()
                    .equalsIgnoreCase(formattedSenderName)) {

                validPackages.add(lPackage);
                message.append(String.format(
                        "\n&(aqua)%s &(gold)- &(green)%s",
                        itemDisplay,
                        lPackage.message
                ));

            }
            if(args.length == 0){
                validPackages.add(lPackage);
                String accept = "${hovercmd,/paccept " + formattedSenderName + ",&(dark_green)Accept package,&(dark_green,bold)[Accept]}";
                String decline = "${hovercmd,/pdecline " + formattedSenderName + ",&(dark_red)Decline package,&(dark_red,bold)[Decline]}";

                message.append(String.format(
                        "\n${hover,&(aqua)%s,&(green)%s}" +
                                "&(gold): " +
                                "&(aqua)%s %s",
                        lPackage.message,
                        packageSender.username,
                        itemDisplay,
                        accept + " " + decline
                ));
            }
        }
        if(validPackages.isEmpty()){
            TextUtils.sendFormatted(sender, "&(red)No packages found.");
            return true;
        }

        TextUtils.sendFormatted(
                sender,
                message.toString(),
                validPackages.size(),
                FarLands.getDataHandler().getOfflineFLPlayer(validPackages.get(0).senderUuid).username,
                Chat.removeColorCodes(validPackages.get(0).senderName.replaceAll("\\{+|}+", ""))
        );

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!(sender instanceof Player) || args.length != 1)
            return Collections.emptyList();
        Player player = (Player)sender;
        return TabCompleterBase.filterStartingWith(args.length == 1 ? args[0] : "",
                FarLands.getDataHandler().getPackages(player.getUniqueId()).stream()
                        .map(p -> FarLands.getDataHandler().getOfflineFLPlayer(p.senderUuid).username));
    }
}
