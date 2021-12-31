package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandJS extends Command {
    private ScriptEngine engine;

    private static final List<String> SELF_ALIAS = Arrays.asList("self", "sender");

    private void initJS() {
        // Setup class loading
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(FarLands.getInstance().getClass().getClassLoader());

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine("--language=es6");

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
            String str = result + "";
            Component component = Component.text(str);
            if(str.length() > 400) { // Limit to 400 characters
                component = ComponentColor.white(str.substring(0, 400))
                    .append(ComponentColor.gray("..."));
                FarLands.getDebugger().echo(str.substring(0, Math.min(str.length(), 2000)));
            }
            sender.sendMessage(component);
        } catch (ScriptException e) {
            sender.sendMessage(ComponentColor.red(e.getMessage()));
            e.printStackTrace();
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
