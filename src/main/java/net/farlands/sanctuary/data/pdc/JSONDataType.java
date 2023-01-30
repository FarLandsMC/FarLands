package net.farlands.sanctuary.data.pdc;

import com.squareup.moshi.JsonAdapter;
import net.farlands.sanctuary.FarLands;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A {@link PersistentDataType} that saves the provided value as JSON
 * @param <T> The type of data to save
 */
public class JSONDataType<T> implements PersistentDataType<String, T> {

    private final Class<T> clazz;
    private final Type customType;

    /**
     * Default constructor for the class
     * @param clazz The base class to use (needed for {@link PersistentDataType})
     * @param customType A custom type to use for the moshi adapter.  This is mostly used for generic classes like {@link List}
     */
    public JSONDataType(Class<T> clazz, Type customType) {
        this.clazz = clazz;
        this.customType = customType;
    }

    /**
     * Constructor that attempts to get the type from the provided {@link Class<T>}
     * @param clazz The base class to use (needed for {@link PersistentDataType})
     */
    public JSONDataType(Class<T> clazz) {
        this(clazz, clazz);
    }

    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<T> getComplexType() {
        return this.clazz;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull T complex, @NotNull PersistentDataAdapterContext context) {
        JsonAdapter<T> adapter = FarLands.getMoshi().adapter(this.customType);
        return adapter.toJson(complex);
    }

    @Override
    public @NotNull T fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        try {
            @SuppressWarnings("unchecked")
            T parsed = (T) FarLands.getMoshi()
                .adapter(this.customType)
                .fromJson(primitive);
            if (parsed == null) throw new IOException("Invalid JSON (null)");
            return parsed;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
