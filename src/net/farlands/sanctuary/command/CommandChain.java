package net.farlands.sanctuary.command;

import com.kicas.rp.util.Pair;

import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;
import java.util.List;

public class CommandChain extends Command {
    public CommandChain() {
        super(Rank.INITIATE, "Chain multiple commands together.", "/chain <{command0}> [{command1} {command2}...]", "chain");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0)
            return false;
        String input = String.join(" ", args);
        List<String> commands = new LinkedList<>();
        int index = 0;
        while(index < input.length()) {
            if('{' == input.charAt(index)) {
                Pair<String, Integer> command = FLUtils.getEnclosed(index, input);
                if(command == null || command.getSecond() < 0)
                    return false;
                commands.add(command.getFirst());
                index = command.getSecond();
                continue;
            }
            ++ index;
        }

        List<String> badCommands = new LinkedList<>();
        for(String cmd : commands){
            String cmdName = cmd.split(" ")[0];
            Command command = FarLands.getCommandHandler().getCommand(cmdName);
            if(command == null || command.canUse(sender, false)){
                Bukkit.dispatchCommand(sender, cmd);
            }else{
                badCommands.add(cmdName);
            }
        }
        if(!badCommands.isEmpty()){
            TextUtils.sendFormatted(
                    sender,
                    "&(red)You do not have permission to run the following $(inflect,noun,0,command): %1",
                    badCommands.size(),
                    String.join(", ", badCommands)
            );
        }
        return true;
    }
}
