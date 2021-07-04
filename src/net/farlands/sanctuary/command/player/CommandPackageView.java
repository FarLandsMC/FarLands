package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.mechanic.Chat;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPackageView extends Command {
    public CommandPackageView() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "View packages sent to you by other players.", "/packageview [player|all]", "packageview", "packagesview");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        OfflineFLPlayer viewFlp = args.length >= 1 ? FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]) : null;

        OfflineFLPlayer searchPlayer = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        boolean externalPlayer = false;

        if(args.length >= 2 && searchPlayer.rank.isStaff()){
            searchPlayer = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
            externalPlayer = true;
        }

        if(searchPlayer == null){
            TextUtils.sendFormatted(sender, "&(red)Unknown player " + args[1] + ".");
            return true;
        }

        List<Package> packages = FarLands.getDataHandler().getPackages(searchPlayer.uuid);

        if(packages.isEmpty()){
            TextUtils.sendFormatted(sender, "&(red)" + (externalPlayer ? searchPlayer.username + " does" : "You do") + " not have any pending packages.");
            return true;
        }

        if(viewFlp == null && args.length >= 1 && !args[0].equalsIgnoreCase("all")){
            TextUtils.sendFormatted(sender, "&(red)Unknown player \"" + args[0] + "\"");
            return true;
        }
        StringBuilder message = new StringBuilder("&(gold)- " + (externalPlayer ? searchPlayer + " has" : "") + " %0 ${inflect,noun,0,package} ");

        if(args.length == 0 || args[0].equalsIgnoreCase("all")){
            message.append("-");
        }else{
            String accept = "${hovercmd,/paccept %2,&(dark_green)Accept %0 ${inflect,noun,0,package},&(dark_green,bold)[Accept]}";
            String decline = "${hovercmd,/pdecline %2,&(dark_red)Decline %0 ${inflect,noun,0,package},&(dark_red,bold)[Decline]}";
            message.append("from {&(green)%1} - " + (externalPlayer ? "" : accept + " " + decline));
        }


        List<Package> validPackages = new ArrayList<>();
        for (Package lPackage : packages) {

            ItemStack item = lPackage.item.clone();
            ItemMeta im = item.getItemMeta();

            // Replace {( with [ and }) with ] to prevent bracket mismatch - escaping with '\}' doesn't work.
            im.setDisplayName(im.getDisplayName().replaceAll("[{(]", "[").replaceAll("[})]", "]"));
            item.setItemMeta(im);

            String json = CraftItemStack.asNMSCopy(item).save(new NBTTagCompound()).toString();
            String name;
            if (im.getDisplayName().equals("")) {
                name = item.getType().name().replace("_", " ");
                name = WordUtils.capitalizeFully(name);
            } else {
                name = im.getDisplayName();
            }
            name = name.replaceAll("[{(]", "[").replaceAll("[})]", "]");

            String itemDisplay = item.getAmount() + " x ["  + "$(item," + json + "," + name + ")]";
            OfflineFLPlayer packageSender = FarLands.getDataHandler().getOfflineFLPlayer(lPackage.senderUuid);
            String formattedSenderName = Chat.removeColorCodes(lPackage.senderName.replaceAll("\\{+|}+", ""));

            if (args.length >= 1 && !args[0].equalsIgnoreCase("all") && viewFlp.getDisplayName().equalsIgnoreCase(formattedSenderName)) {

                validPackages.add(lPackage);
                message.append(String.format(
                        "\n&(aqua)%s &(gold)- &(green)%s",
                        itemDisplay,
                        lPackage.message
                ));

            }
            if(args.length == 0 || args[0].equalsIgnoreCase("all")){
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
                        externalPlayer ? "" : accept + " " + decline
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
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if(flp == null){
            return Collections.emptyList();
        }
        switch(args.length){
            case 1:
                List<String> values = FarLands.getDataHandler().getPackages(flp.uuid).stream().map(p -> FarLands.getDataHandler().getOfflineFLPlayer(p.senderUuid).username).collect(Collectors.toList());
                values.add("all");
                return TabCompleterBase.filterStartingWith(args[0], values);
            case 2:
                return TabCompleterBase.getOnlinePlayers(args[1]);
            default:
                return Collections.emptyList();
        }
    }
}
