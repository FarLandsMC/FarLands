package net.farlands.sanctuary.data;

import com.squareup.moshi.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class CustomAdapters {

    public static void register(Moshi.Builder builder) { // Called in FarLands.java
        builder.add(new NamedTextColorAdapter());
        builder.add(new UUIDAdapter());
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
}
