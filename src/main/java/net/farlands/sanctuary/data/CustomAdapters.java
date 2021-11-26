package net.farlands.sanctuary.data;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;

public class CustomAdapters {

    public static void register(GsonBuilder builder) { // Called in FarLands.java
        builder.registerTypeAdapter(NamedTextColor.class, new NamedTextColorAdapter());
    }

    public static class NamedTextColorAdapter extends TypeAdapter<NamedTextColor> {

        @Override
        public void write(JsonWriter out, NamedTextColor value) throws IOException {
            if(value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public NamedTextColor read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return NamedTextColor.NAMES.value(
                in.nextString()
                    .toLowerCase() // Needed for backwards compatibility with ChatColor
            );
        }
    }
}
