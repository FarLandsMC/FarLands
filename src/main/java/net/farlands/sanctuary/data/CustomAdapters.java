package net.farlands.sanctuary.data;

import com.squareup.moshi.*;
import net.farlands.sanctuary.chat.MiniMessageWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        builder.add(new UUIDAdapter());
        builder.add(new ComponentAdapter());
        builder.add(new LocationAdapter());
    }

    /**
     * Serializes {@link NamedTextColor} -- NamedTextColor.GRAY to "gray"
     */
    public static class NamedTextColorAdapter extends JsonAdapter<NamedTextColor> {

        @Nullable
        @FromJson
        @Override
        public NamedTextColor fromJson(JsonReader in) throws IOException {
            if (in.peek() == JsonReader.Token.NULL) {
                in.nextNull();
                return null;
            }
            return NamedTextColor.NAMES.value(
                in.nextString()
                    .toLowerCase() // Needed for backwards compatibility with ChatColor
            );
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable NamedTextColor value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
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
            if (in.peek() == JsonReader.Token.NULL) {
                in.nextNull();
                return null;
            }
            return UUID.fromString(in.nextString());
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable UUID value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
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
            if (in.peek() == JsonReader.Token.NULL) {
                in.nextNull();
                return null;
            }

            final String nickname = in.nextString();
            try {
                return GsonComponentSerializer.gson().deserialize(nickname);
            } catch (Exception ignored) {
                return MiniMessageWrapper.legacy()
                    .toBuilder()
                    .preventLuminanceBelow(16)
                    .removeColors(true, NamedTextColor.BLACK)
                    .removeTextDecorations(TextDecoration.OBFUSCATED)
                    .build()
                    .mmParse(nickname.replace("§", "&"));
            }
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable Component value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
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
            if (in.peek() == JsonReader.Token.NULL) {
                in.nextNull();
                return null;
            }

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
            return new Location(Bukkit.getWorld(worldUid), x, y, z, pitch, yaw);
        }

        @ToJson
        @Override
        public void toJson(@NotNull JsonWriter out, @Nullable Location value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
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
}
