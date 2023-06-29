package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

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
            sender.sendMessage(ComponentColor.red("Unknown Player %s.", args[1]));
            return true;
        }

        List<Package> packages = FarLands.getDataHandler().getPackages(searchPlayer.uuid);

        if(packages.isEmpty()){
            sender.sendMessage(ComponentColor.red("%s not have any pending packages.", externalPlayer ? searchPlayer.username + " does" : "You do"));
            return true;
        }

        if(viewFlp == null && args.length >= 1 && !args[0].equalsIgnoreCase("all")){
            sender.sendMessage(ComponentColor.red("Unknown Player \"%s\"", args[0]));
            return true;
        }

        List<Package> validPackages = packages.stream()
            .filter(p -> {
                String formattedSenderName = FLUtils.removeColorCodes(p.senderName().replaceAll("\\{+|}+", ""));;
                return (args.length >= 1 && !args[0].equalsIgnoreCase("all") &&
                    viewFlp.username.equalsIgnoreCase(formattedSenderName)) ||
                    args.length == 0 || args[0].equalsIgnoreCase("all");

            })
            .collect(Collectors.toList());

        if(validPackages.isEmpty()){
            sender.sendMessage(ComponentColor.red("No packages found."));
            return true;
        }

        int pkgs = validPackages.size();

        TextComponent.Builder component = Component.text()
            .color(NamedTextColor.GOLD)
            .content("- ")
            .append(Component.text((externalPlayer ? searchPlayer + " has " : "")))
            .append(Component.text(pkgs))
            .append(Component.text(" " + (pkgs == 1 ? "package" : "packages")))
            ;

        if(args.length == 0 || args[0].equalsIgnoreCase("all")){
            component.append(Component.text("-"));
        }else{
            String username = FarLands
                .getDataHandler()
                .getOfflineFLPlayer(
                    validPackages.get(0).senderUuid()
                ).username;
            Component accept = ComponentUtils.command(
                "/paccept " + username,
                ComponentColor.darkGreen("Accept " + pkgs + (pkgs == 1 ? "package" : "packages")),
                ComponentColor.darkGreen("[Accept]").style(Style.style(TextDecoration.BOLD))
            );
            Component decline = ComponentUtils.command(
                "/pdecline " + username,
                ComponentColor.darkRed("Decline " + pkgs + (pkgs == 1 ? "package" : "packages")),
                ComponentColor.darkRed("[Decline]").style(Style.style(TextDecoration.BOLD))
            );

            component.append(Component.text("from "))
                .append(ComponentColor.green(username))
                .append(Component.text(" - "));

            if (!externalPlayer) {
                component.append(accept)
                    .append(Component.text(" "))
                    .append(decline);
            }
        }


        for (Package lPackage : packages) {

            TextComponent.Builder line = Component.text()
                .color(NamedTextColor.GOLD);

            Component itemDisplay = ComponentColor.gold(lPackage.item().getAmount() + " x [")
                .append(ComponentUtils.item(lPackage.item()))
                .append(ComponentColor.gold("]"));

            OfflineFLPlayer packageSender = FarLands.getDataHandler().getOfflineFLPlayer(lPackage.senderUuid());
            String username = FLUtils.removeColorCodes(lPackage.senderName().replaceAll("\\{+|}+", ""));

            if (args.length >= 1 && !args[0].equalsIgnoreCase("all") && viewFlp.username.equalsIgnoreCase(username)) {
                line.append(itemDisplay)
                    .append(ComponentColor.gold(" - "))
                    .append(ComponentColor.green("").append(lPackage.message()));

            }
            if (args.length == 0 || args[0].equalsIgnoreCase("all")) {
                line.append(
                        lPackage.message() == null
                        ? ComponentColor.aqua(packageSender.username)
                        : ComponentUtils.hover(
                            ComponentColor.aqua(packageSender.username),
                            ComponentColor.aqua("").append(lPackage.message())
                        )
                    )
                    .append(ComponentColor.gold(": "))
                    .append(itemDisplay);

                if (!externalPlayer) {
                    Component accept = ComponentUtils.command( // [Accept]
                        "/paccept " + username,
                        ComponentColor.darkGreen("Accept package"),
                        ComponentColor.darkGreen("[Accept]").style(Style.style(TextDecoration.BOLD))
                    );
                    Component decline = ComponentUtils.command( // [Decline]
                        "/pdecline " + username,
                        ComponentColor.darkRed("Decline package"),
                        ComponentColor.darkRed("[Decline]").style(Style.style(TextDecoration.BOLD))
                    );

                    line.append(accept).append(Component.text(" ")).append(decline);
                }
            }
            component.append(
                ComponentColor.gold("\n")
                    .append(line)
            );

        }

        if(validPackages.isEmpty()){
            sender.sendMessage(ComponentColor.red("No packages found."));
            return true;
        }

//        TextUtils.sendFormatted(
//                sender,
//                message.toString(),
//                validPackages.size(),
//                FarLands.getDataHandler().getOfflineFLPlayer(validPackages.get(0).senderUuid).username,
//                Chat.removeColorCodes(validPackages.get(0).senderName.replaceAll("\\{+|}+", ""))
//        );
        sender.sendMessage(component);

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
                List<String> values = FarLands.getDataHandler().getPackages(flp.uuid).stream().map(p -> FarLands.getDataHandler().getOfflineFLPlayer(p.senderUuid()).username).collect(Collectors.toList());
                values.add("all");
                return TabCompleterBase.filterStartingWith(args[0], values);
            case 2:
                return TabCompleterBase.getOnlinePlayers(args[1]);
            default:
                return Collections.emptyList();
        }
    }
}
