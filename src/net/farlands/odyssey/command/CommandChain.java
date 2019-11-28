package net.farlands.odyssey.command;

import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
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
                Pair<String, Integer> command = Utils.getEnclosed(index, input);
                if(command == null)
                    return false;
                commands.add(command.getFirst());
                index = command.getSecond();
                continue;
            }
            ++ index;
        }
        commands.forEach(cmd -> Bukkit.dispatchCommand(sender, cmd));
        return true;
    }
}
