package net.farlands.sanctuary.data;

import com.squareup.moshi.*;
import net.farlands.sanctuary.data.struct.VoteRewards;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

/**
 * Custom adapters for use with Moshi
 */
public class CustomAdapters {

    public static void register(Moshi.Builder builder) { // Called in FarLands.java
        builder.add(new NamedTextColorAdapter());
        builder.add(new TextColorAdapter());
        builder.add(new UUIDAdapter());
        builder.add(new ComponentAdapter());
        builder.add(new LocationAdapter());
        builder.add(new VoteRewardsAdapter());
    }

    /**
     * If the next value on `in` is null, it will accept it and return true
     */
    private static boolean handleNull(@NotNull JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return true;
        }
        return false;
    }

    /**
     * If `o` is null, it will write a null value to `out` and return true
     */
    @Contract("_, !null -> false; _, null -> true")
    private static boolean handleNull(@NotNull JsonWriter out, @Nullable Object o) throws IOException {
        if (o == null) {
            out.nullValue();
            return true;
        }
        return false;
    }

    /**
     * Serializes {@link TextColor} -- NamedTextColor.GRAY to "gray"
     */
    public static class NamedTextColorAdapter extends JsonAdapter<NamedTextColor> {

        @Nullable
        @FromJson
        @Override
        public NamedTextColor fromJson(@NotNull JsonReader in) throws IOException {
            if (handleNull(in)) return null;

            return NamedTextColor.NAMES.value(in.nextString().toLowerCase().replace('-', '_'));
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable NamedTextColor col) throws IOException {
            if (handleNull(out, col)) return;

            out.value(col.toString());
        }
    }

    /**
     * Serializes {@link TextColor} -- the name of the colour if it's a NamedTextColor, otherwise a hex string.
     */
    public static class TextColorAdapter extends JsonAdapter<TextColor> {

        @Nullable
        @FromJson
        @Override
        public TextColor fromJson(@NotNull JsonReader in) throws IOException {
            if (handleNull(in)) return null;

            return FLUtils.parseColor(in.nextString());
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable TextColor col) throws IOException {
            if (handleNull(out, col)) return;

            out.value(FLUtils.colorToString(col));
        }
    }

    /**
     * Serializes {@link UUID} to a string with {@link UUID#toString()} and {@link UUID#fromString(String)}
     */
    public static class UUIDAdapter extends JsonAdapter<UUID> {

        @Nullable
        @FromJson
        @Override
        public UUID fromJson(JsonReader in) throws IOException {
            if (handleNull(in)) return null;

            return UUID.fromString(in.nextString());
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable UUID value) throws IOException {
            if (handleNull(out, value)) return;

            out.value(value.toString());
        }
    }

    /**
     * Serialize components with Gson or Legacy deserialization
     */
    public static class ComponentAdapter extends JsonAdapter<Component> {

        @FromJson
        @Override
        public @Nullable Component fromJson(JsonReader in) throws IOException {
            if (handleNull(in)) return null;

            String s = in.nextString();
            try {
                return GsonComponentSerializer.gson().deserialize(s);
            } catch (Exception ignored) {
                return ComponentUtils.parse(s.replace('§', '&'));
            }
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable Component value) throws IOException {
            if (handleNull(out, value)) return;

            out.value(GsonComponentSerializer.gson().serialize(value));
        }
    }

    /**
     * Serialize {@link Location}
     */
    public static class LocationAdapter extends JsonAdapter<Location> {

        @FromJson
        @Override
        public @Nullable Location fromJson(JsonReader in) throws IOException {
            if (handleNull(in)) return null;

            UUID worldUid = null;
            double x = 0, y = 0, z = 0;
            float pitch = 0, yaw = 0;

            in.beginObject();
            while(in.peek() != JsonReader.Token.END_OBJECT) {
                switch(in.nextName()) {
                    case "world" -> worldUid = UUID.fromString(in.nextString());
                    case "x" -> x = in.nextDouble();
                    case "y" -> y = in.nextDouble();
                    case "z" -> z = in.nextDouble();
                    case "pitch" -> pitch = (float) in.nextDouble();
                    case "yaw" -> yaw = (float) in.nextDouble();
                }
            }
            in.endObject();
            return new Location(Bukkit.getWorld(worldUid), x, y, z, pitch, yaw);
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable Location value) throws IOException {
            if (handleNull(out, value)) return;

            out.beginObject();
            out.name("world").value(value.getWorld().getUID().toString());
            out.name("x").value(value.getX());
            out.name("y").value(value.getY());
            out.name("z").value(value.getZ());
            out.name("pitch").value(value.getPitch());
            out.name("yaw").value(value.getYaw());
            out.endObject();

        }
    }

    /**
     * Serialize {@link VoteRewards}
     * <p>
     * This is necessary to convert from the old boolean that was used
     */
    public static class VoteRewardsAdapter extends JsonAdapter<VoteRewards> {

        @FromJson
        @Override
        public @Nullable VoteRewards fromJson(JsonReader in) throws IOException {
            return switch (in.peek()) {
                case NULL -> in.nextNull();
                case BOOLEAN -> VoteRewards.from(in.nextBoolean());
                default -> VoteRewards.valueOf(in.nextString());
            };
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable VoteRewards value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.name());
        }
    }
}
