package net.farlands.sanctuary.data;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class SerializationExclusionStrategy implements ExclusionStrategy {
    public boolean shouldSkipField(FieldAttributes attrs) {
        return attrs.getAnnotation(SkipSerializing.class) != null;
    }

    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
