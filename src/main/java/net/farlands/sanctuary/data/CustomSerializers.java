package net.farlands.sanctuary.data;

import com.google.gson.*;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.reflect.Type;

public class CustomSerializers {

    public static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(NamedTextColor.class, new NamedTextColorSerializer());
        builder.registerTypeAdapter(NamedTextColor.class, new NamedTextColorDeserializer());
    }

    private static class NamedTextColorSerializer implements JsonSerializer<NamedTextColor> {

        @Override
        public JsonElement serialize(NamedTextColor src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class NamedTextColorDeserializer implements JsonDeserializer<NamedTextColor> {

        @Override
        public NamedTextColor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.isJsonObject() ||
                json.getAsString() == null ||
                json.getAsString().equalsIgnoreCase("null") ?
                NamedTextColor.RED :
                NamedTextColor.NAMES.value(json.getAsString().toLowerCase());
        }
    }
}
