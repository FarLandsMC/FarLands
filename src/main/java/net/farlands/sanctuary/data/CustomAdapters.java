package net.farlands.sanctuary.data;

import com.squareup.moshi.*;
import net.farlands.sanctuary.chat.MiniMessageWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class CustomAdapters {

    public static void register(Moshi.Builder builder) { // Called in FarLands.java
        builder.add(new NamedTextColorAdapter());
        builder.add(new UUIDAdapter());
        builder.add(new ComponentAdapter());
    }

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
                return MiniMessageWrapper.legacy().toBuilder()
                    .preventLuminanceBelow(16)
                    .removeColors(true, NamedTextColor.BLACK)
                    .removeTextDecorations(TextDecoration.OBFUSCATED)
                    .build().mmParse(nickname.replace("§", "&"));
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
}
