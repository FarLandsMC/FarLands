package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.Range;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.kicas.rp.command.TabCompleterBase.filterStartingWith;

public class CommandConfig extends Command {

    public CommandConfig() {
        super(
            CommandData.simple(
                "config",
                "Control the player's configuration",
                "/config <field> [...args]"
            ).category(Category.PLAYER_SETTINGS_AND_INFO)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Queue<String> cmd = new LinkedList<>(List.of(args));

        // The first arg is required, return false if it's not provided
        String arg = cmd.poll();
        if (arg == null) return false;

        // Try to parse the ConfigField
        ConfigFields cf = ConfigFields.from(arg);

        // If it's not valid, error to the user
        if (cf == null) {
            return error(sender, "Invalid field. Valid fields are: {}", ConfigFields.fields(sender));
        }

        // Check that the sender is valid for the field that they desire
        Optional<String> a = cf.inner().testSender(sender);
        if (a.isPresent()) {
            return error(sender, a.get());
        }

        // Get the next arg
        arg = cmd.poll();

        // If the arg is not present, then just read the value
        if (arg == null) {
            String current = cf.inner().get(sender);
            return info(sender, "{} is currently set to {}", cf, current);
        }

        // Otherwise, try to set it
        try {
            cf.inner().set(arg, sender);
            String current = cf.inner().get(sender);
            return info(sender, "Updated {} to be {}.", cf, current);
        } catch (IllegalArgumentException ex) {
            return error(sender, "Invalid value: {}", ex.getMessage());
        }

    }

    @Override
    public @NotNull List<String> tabComplete(
        @NotNull CommandSender sender,
        @NotNull String alias,
        @NotNull String[] args,
        Location location
    ) throws IllegalArgumentException {
        try {
            return ConfigFields.tabComplete(sender, args);
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Create a range predicate from a string
     * @param range The range using a "rust-style" format, see {@link Range#from}
     * @return A predicate to be used in {@link ConfigFields.ConfigField.Builder#senderPredicate}
     */
    private static Predicate<Integer> range(@NotNull String range) {
        return Range.from(range)::contains;
    }

    public enum ConfigFields {
        FLY(
            ConfigField
                .builder(Rank.JR_BUILDER, Boolean.class)
                .setter((v, cs) -> {
                    FarLands.getDataHandler().getOfflineFLPlayer(cs).flightPreference = v;
                })
                .getter((cs) -> FarLands.getDataHandler().getOfflineFLPlayer(cs).flightPreference ? "on" : "off")
                .build()
        ),
        FLY_SPEED(
            ConfigField
                .builder(Rank.JR_BUILDER, Integer.class)
                .validator(range("0..=10"), "Value must be within 0..=10") // Technically could be -10..=10, but that's just weird
                .senderPredicate(cs -> cs instanceof Player, "You must be in-game to change this field.")
                .setter((n, cs) -> {
                    ((Player) cs).setFlySpeed(n * 0.1f);
                })
                .getter((cs) -> String.valueOf((int) (((Player) cs).getFlySpeed() * 10)))
                .build()
        ),
        GOD(
            ConfigField
                .builder(Rank.JR_BUILDER, Boolean.class)
                .setter((v, cs) -> {
                    FarLands.getDataHandler().getOfflineFLPlayer(cs).god = v;
                })
                .getter((cs) -> FarLands.getDataHandler().getOfflineFLPlayer(cs).god ? "on" : "off")
                .build()
        ),
        FIREWORK_LAUNCH(
            ConfigField
                .builder(Rank.INITIATE, Boolean.class)
                .setter((v, cs) -> {
                    FarLands.getDataHandler().getOfflineFLPlayer(cs).fireworkLaunch = v;
                })
                .getter((cs) -> FarLands.getDataHandler().getOfflineFLPlayer(cs).fireworkLaunch ? "on" : "off")
                .build()
        ),
        ;

        private final ConfigField<?> inner;

        ConfigFields(ConfigField<?> inner) {
            this.inner = inner;
        }

        /**
         * @return the inner value of this field
         */
        public ConfigField<?> inner() {
            return this.inner;
        }

        /**
         * Get a {@link ConfigField} from a humanised name
         * @param fieldName The humanised name
         * @return The Config filed
         */
        public static ConfigFields from(String fieldName) {
            return Utils.valueOfFormattedName(fieldName, ConfigFields.class);
        }

        /**
         * Get all fields that the sender can modify/get in this context that match a certain filter
         * @param sender The sender to use for context checking
         * @param filter The filter to use (checks contains)
         * @return the fields that can be get/set
         */
        public static List<String> fields(CommandSender sender, String filter) {
            return ConfigFields.fields(sender)
                .stream()
                .filter(s -> s.contains(filter))
                .toList();
        }

        /**
         * Get all fields that the sender can modify/get in this context
         * @param sender The sender to use for context checking
         * @return the fields that can be get/set
         */
        public static List<String> fields(CommandSender sender) {
            return Arrays.stream(ConfigFields.values())
                .filter(cf -> cf.inner().testSender(sender).isEmpty())
                .map(Utils::formattedName)
                .toList();
        }

        /**
         * Handle tab-completion for the ConfigFields
         * @throws IllegalArgumentException If the arguments are invalid
         */
        public static List<String> tabComplete(
            CommandSender sender,
            String[] args
        ) throws IllegalArgumentException {
            return switch (args.length) {
                case 1 -> fields(sender, args[0]);
                case 2 -> {
                    ConfigFields cf = ConfigFields.from(args[0]);
                    if (cf == null || cf.inner().testSender(sender).isPresent()) {
                        throw new IllegalArgumentException();
                    }

                    List<String> out;
                    if (cf.inner().argumentType == Boolean.class) {
                        out = List.of("on", "off");
                    } else {
                        yield Collections.emptyList();
                    }

                    yield filterStartingWith(args[1], out);
                }
                default -> throw new IllegalArgumentException();
            };
        }

        /**
         * A function that handles the setting of a field
         * <p>
         * This is passed the parsed value and the sender
         */
        @FunctionalInterface
        private interface Setter<T> {
            void apply(T value, CommandSender sender);
        }

        /**
         * Represents a field that can be modified by the `/config` command.
         * <p>
         * Valid {@link T}s are listed here:
         * <ul>
         *     <li>{@link Boolean}
         *         <ul>
         *            <li>This accepts anything in {@link ConfigField#TRUE_VALUES} as {@code true}
         *                and anything in {@link ConfigField#FALSE_VALUES} as {@code false}</li>
         *         </ul>
         *     </li>
         *     <li>{@link String}</li>
         *     <li>{@link Integer}
         *         <ul>
         *            <li>This parses integers using {@link Integer#decode}</li>
         *         </ul>
         *     </li>
         *     <li>{@link Double}
         *         <ul>
         *            <li>This parses doubles using {@link Double#parseDouble}</li>
         *            <li>Double was chosen over float because it can be downcast into a float.</li>
         *         </ul>
         *     </li>
         * </ul>
         * <p>
         *
         * @param <T> The type of the field -- used for argument validation
         */
        private static class ConfigField<T> {

            private static final Set<String> TRUE_VALUES  = Set.of("on", "yes", "true");
            private static final Set<String> FALSE_VALUES = Set.of("off", "no", "false");

            private final Rank                            rank;
            private final Class<T>                        argumentType;
            private final Predicate<T>                    validator;
            private final String                          validatorIncorrect;
            private final Predicate<CommandSender>        senderPredicate;
            private final String                          senderIncorrect;
            private final Setter<T>                       setter;
            private final Function<CommandSender, String> getter;

            private ConfigField(Builder<T> builder) {
                rank = builder.rank;
                argumentType = builder.argumentType;
                validator = builder.validator;
                validatorIncorrect = builder.validatorIncorrect;
                senderPredicate = builder.senderPredicate;
                senderIncorrect = builder.senderIncorrect;
                setter = builder.setter;
                getter = builder.getter;
            }

            /**
             * Initialise a builder for the {@link ConfigField}
             * @param rank The minimum rank than a user must be to get/set the field
             * @param argType The type that the argument expects
             * @return The builder
             * @param <T> The type that the argument expects
             */
            public static <T> Builder<T> builder(Rank rank, Class<T> argType) {
                return new Builder<>(rank, argType);
            }

            /**
             * Test to see if a sender is allowed to modify this value
             *
             * @param sender The sender to check
             * @return An error message or empty if it's allowed to be modified
             */
            public Optional<String> testSender(CommandSender sender) {
                if (this.senderPredicate != null && !this.senderPredicate.test(sender)) {
                    return Optional.of(this.senderIncorrect);
                }
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
                if (flp == null) {
                    return Optional.of("Sender must be a player");
                }

                if (flp.rank.specialCompareTo(this.rank) >= 0) {
                    return Optional.empty();
                } else {
                    return Optional.of("You don't have permission to modify this field.");
                }
            }

            /**
             * Parses a string into {@link T}.
             * @param str The string to parse
             * @return The parsed string
             * @throws IllegalArgumentException If the string is not valid (i.e. "hello" when checking for boolean) or if the string doesn't pass {@link ConfigField#validate}
             */
            @SuppressWarnings("unchecked")
            public T parse(String str) throws IllegalArgumentException {
                if (this.argumentType == Boolean.class) { // Bools
                    boolean out;
                    if (TRUE_VALUES.contains(str)) {
                        out = true;
                    } else if (FALSE_VALUES.contains(str.toLowerCase())) {
                        out = false;
                    } else {
                        throw new IllegalArgumentException("Expected boolean, found \"" + str + "\"");
                    }

                    Optional<String> a = this.validate((T) (Boolean) out); // Idk why one would want a validator for a bool...
                    a.ifPresent(s -> {
                        throw new IllegalArgumentException(s);
                    });

                    return (T) (Boolean) out;
                } else if (this.argumentType == String.class) { // Strings
                    Optional<String> a = this.validate((T) str);
                    a.ifPresent(s -> {
                        throw new IllegalArgumentException(s);
                    });
                    return (T) str;
                } else if (this.argumentType == Integer.class) { // Ints
                    int i;

                    try {
                        i = Integer.decode(str);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Expected integer, found \"" + str + "\"");
                    }

                    Optional<String> a = this.validate((T) (Integer) i);
                    a.ifPresent(s -> {
                        throw new IllegalArgumentException(s);
                    });

                    return (T) (Integer) i;
                } else if (this.argumentType == Double.class) { // Doubles  (rather than floats because we can always downcast)
                    double f;

                    try {
                        f = Double.parseDouble(str);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Expected float, found \"" + str + "\"");
                    }

                    Optional<String> a = this.validate((T) (Double) f);
                    a.ifPresent(s -> {
                        throw new IllegalArgumentException(s);
                    });

                    return (T) (Double) f;
                } else {
                    throw new RuntimeException("Can't parse unknown class: " + this.argumentType);
                }
            }

            /**
             * Validate an input based on type
             *
             * @param input the input to check
             * @return A string if it _failed_ and empty if it succeeded
             */
            public Optional<String> validate(T input) {
                if (this.validator == null || this.validator.test(input)) {
                    return Optional.empty();
                } else {
                    return Optional.of(this.validatorIncorrect);
                }
            }

            /**
             * Get the current value of a field for a sender
             * @param sender The sender to use
             * @return The value that is currently stored in this field
             */
            public String get(CommandSender sender) {
                return this.getter.apply(sender);
            }

            /**
             * Update the value of a field for a sender
             * @param value The value to set
             * @param sender The sender to update
             */
            public void set(String value, CommandSender sender) {
                this.setter.apply(this.parse(value), sender);
            }

            public static class Builder<T> {

                private final Rank                            rank;
                private final Class<T>                        argumentType;
                private       Predicate<T>                    validator;
                private       String                          validatorIncorrect;
                private       Predicate<CommandSender>        senderPredicate;
                private       String                          senderIncorrect;
                private       Setter<T>                       setter;
                private       Function<CommandSender, String> getter;

                private Builder(Rank rank, Class<T> argType) {
                    this.rank = rank;
                    this.argumentType = argType;
                }

                /**
                 * Set the validator for this field
                 *
                 * @param validator The validator which receives the parsed value
                 * @param validatorIncorrect The message to give to the user when the validation fails
                 */
                public Builder<T> validator(Predicate<T> validator, String validatorIncorrect) {
                    this.validator = validator;
                    this.validatorIncorrect = validatorIncorrect;
                    return this;
                }

                /**
                 * Set the sender predicate for this field
                 *
                 * @param senderPredicate The predicate used to check the sender
                 * @param senderIncorrect The message to give to the user when the check fails
                 */
                public Builder<T> senderPredicate(Predicate<CommandSender> senderPredicate, String senderIncorrect) {
                    this.senderPredicate = senderPredicate;
                    this.senderIncorrect = senderIncorrect;
                    return this;
                }

                /**
                 * Set the setter for this field
                 *
                 * @param setter The setter to use, which gets passed the parsed value and the sender
                 */
                public Builder<T> setter(Setter<T> setter) {
                    this.setter = setter;
                    return this;
                }

                /**
                 * Set the getter for this field
                 *
                 * @param getter The getter to use, which gets passed the sender and returns the value to be presented to the user
                 */
                public Builder<T> getter(Function<CommandSender, String> getter) {
                    this.getter = getter;
                    return this;
                }

                /**
                 * Build this into a {@link ConfigField}
                 * @return The built {@link ConfigField}
                 */
                public ConfigField<T> build() {
                    return new ConfigField<>(this);
                }
            }
        }
    }

}