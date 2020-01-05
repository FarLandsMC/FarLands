package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandJS extends Command {
    private ScriptEngine engine;
    private final SuggestionGenerator suggestionGenerator;

    private static final List<String> SELF_ALIAS = Arrays.asList("self", "sender");
    // Add any jars you want suggestions from here.
    private static final List<String> CLASSPATH = Arrays.asList("./spigot.jar", "./plugins/FarLands.jar", System.getProperty("java.home") + "/lib/rt.jar");

    private void initJS() {
        // Setup class loading
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(FarLands.getInstance().getClass().getClassLoader());

        engine = new ScriptEngineManager().getEngineByName("nashorn");

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
        this.suggestionGenerator = new SuggestionGenerator(CLASSPATH);
        initJS();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) // Extra security
            return true;

        if ("js".equals(args[0])) {
            SELF_ALIAS.forEach(alias -> engine.put(alias, sender));

            try {
                Object result = engine.eval(joinArgsBeyond(0, " ", args));
                if (result != null)
                    sender.sendMessage(result.toString());
            } catch (ScriptException e) {
                sender.sendMessage(e.getMessage());
            }
        } else
            return false;

        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender)
            return true;
        else if (sender instanceof BlockCommandSender) // Prevent people circumventing permissions by using a command block
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null || !FarLands.getFLConfig().jsUsers.contains(flp.getUuid().toString())) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command.");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public boolean showErrorsOnDiscord() {
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        String lastToken = args.length > 0 ? args[args.length - 1] : "";
        return "js".equals(alias) ? suggestionGenerator.getPackageSuggestions(lastToken) : (args.length <= 1 ? Stream.of("update", "query")
                .filter(o -> o.startsWith(args.length == 0 ? "" : args[0])).collect(Collectors.toList()) : Collections.emptyList());
    }

    private static final class SuggestionGenerator {
        private final List<JarFile> jars;
        private final boolean operational;

        public SuggestionGenerator(List<String> classpath) {
            this.jars = classpath.stream().map(file -> { // Load the jar files
                try {
                    return new JarFile(file);
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                    return null;
                }
            }).collect(Collectors.toList());
            List<URL> urls = classpath.stream().map(entry -> { // Construct the class loader
                try {
                    return (new File(entry)).toURI().toURL();
                } catch (MalformedURLException ex) {
                    ex.printStackTrace(System.out);
                    return null;
                }
            }).collect(Collectors.toList());
            URLClassLoader classLoader = urls.contains(null) ? null : new URLClassLoader(urls.toArray(new URL[0]));
            this.operational = !jars.contains(null) && classLoader != null;
            if (!this.operational)
                Chat.error("Failed to load suggestions for /js.");
        }

        // "current" should be part of a fully qualified class name
        public List<String> getPackageSuggestions(String current) {
            if (!operational)
                return Collections.emptyList();
            Set<String> suggestions = new HashSet<>(); // Removes duplicates
            jars.forEach(jar ->
                    jar.stream().map(JarEntry::getName)
                            .filter(name -> !name.contains("$") && !name.contains("META-INF") && name.startsWith(current.replaceAll("\\.", "/")) &&
                                    (name.endsWith(".class") || !name.contains("."))) // Remove inner classes and metadata information
                            .forEach(name -> suggestions.add(formatPackageName(name, current)))
            );
            return new ArrayList<>(suggestions);
        }

        // Truncates a fully qualified class name to the same path length as the specified token
        // IE: name = java.lang.System, token = java.la will return "java.lang"
        private static String formatPackageName(String name, String token) {
            int index = name.length();
            char[] nameChars = name.toCharArray();
            for (int i = token.length(); i < nameChars.length; ++i) {
                if (nameChars[i] == '/') {
                    index = i;
                    break;
                }
            }
            name = name.substring(0, index);
            if (name.endsWith(".class"))
                name = name.substring(0, name.lastIndexOf('.'));
            return name.replaceAll("/", ".");
        }
    }
}
