package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommandJS extends Command {
    private ScriptEngine engine;

    private static final List<String> SELF_ALIAS = Arrays.asList("self", "sender");

    private void initJS() {
        // Setup class loading
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(FarLands.getInstance().getClass().getClassLoader());

        ScriptEngineManager manager = new ScriptEngineManager();
        System.out.println(manager.getEngineFactories());
        engine = manager.getEngineByName("nashorn");

        try {
            engine.eval(new String(FarLands.getDataHandler().getResource("boot.js"), StandardCharsets.UTF_8)); // Load bootstrap script
        } catch (ScriptException | IOException e) {
            e.printStackTrace();
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }

    }

    public CommandJS() {
        super(Rank.ADMIN, "Evaluate a JavaScript expression", "/js <expression>", "js");
        initJS();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) // Extra security
            return true;

        SELF_ALIAS.forEach(alias -> engine.put(alias, sender));

        try {
            Object result = engine.eval(String.join(" ", args));
            if (result != null)
                sender.sendMessage(result.toString());
        } catch (ScriptException e) {
            sender.sendMessage(e.getMessage());
        }

        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender)
            return true;
        else if (sender instanceof BlockCommandSender) // Prevent people circumventing permissions by using a command block
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null || !FarLands.getFLConfig().jsUsers.contains(flp.uuid.toString())) {
            sendFormatted(sender, "&(red)You cannot use this command.");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public boolean showErrorsOnDiscord() {
        return false;
    }
}
