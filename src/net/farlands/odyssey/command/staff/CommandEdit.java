package net.farlands.odyssey.command.staff;

import com.google.common.collect.ImmutableMap;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.util.ReflectionHelper;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandEdit extends Command {
    // 0: no restriction, 1: read-only, 2: no reading or writing
    private static final Map<String, Integer> RESTRICTED_FIELDS = (new ImmutableMap.Builder<String, Integer>())
            .put("rank", 1)
            .put("uuid", 1)
            .put("username", 1)
            .put("jsUsers", 1)
            .put("dedicatedMemory", 1)
            .put("discordBotConfig.token", 2)
            .build();
    private static final Map<Class<?>, Function<String, ?>> DESERIALIZER = (new ImmutableMap.Builder<Class<?>, Function<String, ?>>())
            .put(Integer.class, Integer::parseInt)
            .put(Long.class, Long::parseLong)
            .put(Short.class, Short::parseShort)
            .put(Double.class, Double::parseDouble)
            .put(Float.class, Float::parseFloat)
            .put(Boolean.class, Boolean::parseBoolean)
            .put(Byte.class, Byte::parseByte)
            .put(Character.class, str -> str.charAt(0))
            .put(String.class, str -> str)
            .build();
    private static final Map<String, Supplier<?>> CONFIGS = (new ImmutableMap.Builder<String, Supplier<?>>())
            .put("main", FarLands::getFLConfig)
            .put("private", FarLands.getDataHandler()::getPluginData)
            .build();

    public CommandEdit() {
        super(Rank.BUILDER, "Edit player or config data.", "/edit <player|config> <name> <fieldname> [value,...]", "edit");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length < 3)
            return false;
        args[0] = args[0].toLowerCase(); // Not case sensitive
        Object target = getTarget(args[0], args[1]); // Get the object we're editing
        if(target == null) { // Send the error if no object was found
            if(!"player".equals(args[0]) && !"config".equals(args[0]))
                return false;
            sender.sendMessage(ChatColor.RED + Utils.capitalize(args[0]) + " not found.");
            return true;
        }
        // Apply restrictions
        int restriction = getRestriction(args[2]);
        if(restriction == 1 && args.length > 3) {
            sender.sendMessage(ChatColor.RED + "\"" + args[2] + "\" is read-only.");
            return true;
        }else if(restriction == 2) {
            sender.sendMessage(ChatColor.RED + "\"" + args[2] + "\" is restricted. Try searching for a specific sub-field.");
            return true;
        }
        try {
            if(args.length == 3) { // Value get requests
                sender.sendMessage(ChatColor.GREEN + args[2] + ChatColor.GOLD + " is currently set to: " + ChatColor.WHITE +
                        Objects.toString(getValue(args[2], target)));
            }else{ // Value set requests
                if(args[3].contains(",")) { // We're setting multiple fields in the target
                    final Object newTarget = getValue(args[2], target);
                    Map<String, String> tokens = new HashMap<>();
                    for(String token : args[3].split(","))
                        tokens.put(token.substring(0, token.indexOf('=')), token.substring(token.indexOf('=') + 1));
                    tokens.forEach((field, value) -> setValue(value, field, newTarget)); // Set the values
                }else{
                    if(setValue(args[3], args[2], target) == null) { // Try to update the value
                        sender.sendMessage(ChatColor.RED + "Could not decode the provided value, are you sure you spelt it correctly?");
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "Updated value" + (args[3].contains(",") ? "s." : "."));
            }
        }catch(InvalidFieldException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid field, field not found: " + ex.getField());
        }
        if(target instanceof OfflineFLPlayer)
            FarLands.getPDH().saveFLPlayerComplete((OfflineFLPlayer)target);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch(args.length) {
            // Give them the options of player or config
            case 0:
            case 1:
                return Stream.of("player", "config").filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
            case 2: // Send the list of available targets
                return "player".equals(args[0])
                        ? getOnlineVanishedPlayers(args[1])
                        : CONFIGS.keySet().stream().filter(config -> config.startsWith(args[1])).collect(Collectors.toList());
            case 3: // Send the list of available fields (with restricted ones redacted)
            {
                Object target = getTarget(args[0], args[1]);
                if(target == null)
                    return Collections.emptyList();
                int lidxof = args[2].lastIndexOf('.');
                List<String> suggestions;
                try {
                    suggestions = getFieldNames(args[2].substring(lidxof + 1),
                            args[2].contains(".") ? getValue(args[2].substring(0, lidxof), target) : target);
                }catch(InvalidFieldException ex) {
                    return Collections.emptyList();
                }
                return suggestions.stream().map(f -> args[2].substring(0, lidxof + 1) + f)
                        .filter(f -> RESTRICTED_FIELDS.getOrDefault(f, 0) < 2).collect(Collectors.toList());
            }
            case 4: // Much complicate, such wow. Give them suggestions for fileds in the target separated by commas
            {
                Object target = getTarget(args[0], args[1]);
                if(target == null)
                    return Collections.emptyList();
                Object value;
                try {
                    value = getValue(args[2], target);
                }catch(InvalidFieldException ex) {
                    return Collections.emptyList();
                }
                if(value == null)
                    return Collections.emptyList();
                if(value.getClass().isEnum()) {
                    return Arrays.stream(value.getClass().getEnumConstants()).map(Objects::toString)
                            .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }
                if(Boolean.class.equals(ReflectionHelper.asWrapper(value.getClass())))
                    return TRUE_OR_FALSE;
                final List<String> split = Arrays.asList(args[3].split(","));
                String last = args[3].endsWith(",") ? "" : split.get(split.size() - 1);
                return last.contains("=")
                        ? Collections.emptyList()
                        : getFieldNames(last, value).stream().filter(name -> split.stream().noneMatch(s -> s.substring(0,
                                Utils.indexOfDefault(s.indexOf('='), s.length())).equals(name)) &&
                                !RESTRICTED_FIELDS.containsKey(args[2] + '.' + name))
                            .map(name -> args[3].substring(0, args[3].lastIndexOf(',') + 1) + name + "=")
                            .collect(Collectors.toList());
            }
            default:
                return Collections.emptyList();
        }
    }

    private static Object getTarget(String type, String name) { // Gets the target, returns null if it couldn't be found
        if("player".equals(type))
            return getFLPlayer(name);
        else if("config".equals(type))
            return CONFIGS.containsKey(name) ? CONFIGS.get(name).get() : null;
        return null;
    }

    private static Object getValue(String field, Object target) { // Gets a field's value (recursively if needed)
        Field f = ReflectionHelper.getFieldObject(
            field.substring(0, Utils.indexOfDefault(field.indexOf('.'), field.length())),
            target.getClass()
        );
        if(f == null)
            throw new InvalidFieldException(field);
        Object value = ReflectionHelper.getFieldValue(f, target);
        return field.contains(".") ? getValue(field.substring(field.indexOf('.') + 1), value) : value;
    }

    private static Object parseValue(String value, Class<?> requiredClass) { // Parses a string according to a field's value type
        try {
            if("null".equals(value))
                return null;
            if(requiredClass.isEnum())
                return cast(requiredClass, Utils.safeValueOf(val -> ReflectionHelper.invoke("valueOf", requiredClass, null, val), value));
            Class<?> deserializerClass = ReflectionHelper.asWrapper(requiredClass);
            return DESERIALIZER.containsKey(deserializerClass)
                    ? cast(requiredClass, DESERIALIZER.get(deserializerClass).apply(value))
                    : null;
        }catch(RuntimeException ex) {
            return null;
        }
    }

    private static Object setValue(String value, String field, Object target) { // Sets a fields value (if possible)
        Field f = ReflectionHelper.getFieldObject(
            field.substring(0, Utils.indexOfDefault(field.indexOf('.'), field.length())),
            target.getClass()
        );
        if(f == null)
            throw new InvalidFieldException(field);
        if(field.contains("."))
            return setValue(value, field.substring(field.indexOf('.') + 1), ReflectionHelper.getFieldValue(f, target));
        else {
            Object val = parseValue(value, f.getType());
            if(val == null && !"null".equals(value))
                return null;
            ReflectionHelper.setFieldValue(f, target, val);
            return val == null ? new Object() : val; // Never returns null unless something went wrong
        }
    }

    private static List<String> getFieldNames(String partialName, Object target) {
        return target == null
                ? Collections.emptyList()
                : Arrays.stream(ReflectionHelper.getFields(target.getClass())).filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .map(Field::getName).filter(name -> name.startsWith(partialName)).collect(Collectors.toList());
    }

    private static Object cast(Class<?> newType, Object obj) {
        if(obj == null || newType == null)
            return null;
        Object casted = ReflectionHelper.safeCast(newType, obj);
        return casted == null ? obj : casted;
    }

    private static int getRestriction(String field) {
        if(RESTRICTED_FIELDS.containsKey(field))
            return RESTRICTED_FIELDS.get(field);
        for(Map.Entry<String, Integer> entry : RESTRICTED_FIELDS.entrySet()) {
            // Ensures that, for example, field=ab will not match entry.key=abc.d
            if(entry.getKey().startsWith(field) && entry.getKey().charAt(field.length()) == '.' || field.startsWith(entry.getKey()))
                return entry.getValue();
        }
        return 0;
    }

    private static final class InvalidFieldException extends RuntimeException {
        private final String field;

        InvalidFieldException(String field) {
            super("Field: " + field);
            this.field = field;
        }

        String getField() {
            return field;
        }
    }
}
