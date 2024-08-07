package net.farlands.sanctuary.command.staff;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommandJS extends Command {

    private       ScriptEngine      engine;
    private final Map<UUID, Object> lastResult;

    private static final List<String> SELF_ALIAS  = Arrays.asList("self", "sender");
    private static final String       FILE_PREFIX = "$$file$$";

    private void initJS() {
        // Setup class loading
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(FarLands.getInstance().getClass().getClassLoader());

        this.engine = GraalJSScriptEngine.create(
            null,
            Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> true)
                .allowAllAccess(true)
        );

        try {
            this.engine.eval(new String(FarLands.getDataHandler().getResource("boot.js"), StandardCharsets.UTF_8)); // Load bootstrap script
        } catch (ScriptException | IOException e) {
            e.printStackTrace();
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }

    }

    public CommandJS() {
        super(Rank.ADMIN, "Evaluate a JavaScript expression", "/js <expression>", "js");
        initJS();
        this.lastResult = new HashMap<>();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) { // Paranoia :)
            return true;
        }

        SELF_ALIAS.forEach(alias -> this.engine.put(alias, sender));

        try {
            var flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            this.engine.put("_", this.lastResult.get(flp == null ? null : flp.uuid));
            Object result = this.engine.eval(String.join(" ", args));
            if (result instanceof Component comp) {
                sender.sendMessage(comp);
                return true;
            }
            String str = result + "";
            if (str.startsWith(FILE_PREFIX)) {
                str = str
                    .substring(FILE_PREFIX.length())
                    .replaceAll("\\n", "\n");
                boolean notebook = sendFile(sender, str);
                if (notebook) {
                    success(sender, "Sent out as file to #scribes-notebook");
                }
                return true;
            } else {
                this.lastResult.put(flp == null ? null : flp.uuid, result);
            }
            Component component = Component.text(str);
            if (str.length() > 400) { // Limit to 400 characters
                component = ComponentColor.white(str.substring(0, 400))
                    .append(ComponentColor.gray("..."));
                FarLands.getDebugger().echo(str.substring(0, Math.min(str.length(), 2000)));
            }
            sender.sendMessage(component);
        } catch (ScriptException e) {
            String message = e.getMessage().replaceFirst("org\\.graalvm\\.polyglot\\.PolyglotException: ", "");
            sender.sendMessage(ComponentColor.red(message));
            e.printStackTrace();
        } catch (Exception e) {
            sender.sendMessage(ComponentColor.red(e.getMessage()));
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        } else if (sender instanceof BlockCommandSender) { // Prevent people circumventing permissions by using a command block
            return false;
        }
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null || !FarLands.getFLConfig().jsUsers.contains(flp.uuid.toString())) {
            error(sender, "You cannot use this command.");
            return false;
        }
        return super.canUse(sender);
    }

    private boolean sendFile(CommandSender sender, String s) {
        TextChannel channel = DiscordChannel.NOTEBOOK.getChannel();
        if (sender instanceof DiscordSender ds) {
            channel = ds.getChannel();
        }
        channel.sendFiles(
            FileUpload.fromData(
                s.getBytes(),
                "js-output_" + sender.getName() + "_" + FLUtils.dateToString(System.currentTimeMillis(), "yyyy-MM-dd-ss") + "_.txt"
            )
        ).queue();
        return !(sender instanceof DiscordSender);
    }

    @Override
    public boolean showErrorsOnDiscord() {
        return false;
    }
}
