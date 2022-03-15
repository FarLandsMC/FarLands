package net.farlands.sanctuary.util;

import com.kicas.rp.util.ReflectionHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import net.farlands.sanctuary.FarLands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Util class for reading and writing files
 */
public final class FileSystem {

    /**
     * Get a file object from a path
     *
     * @param basedir      The base directory to get the file from
     * @param pathElements The path down the directory tree to the file
     */
    public static File getFile(File basedir, String... pathElements) {
        return new File(basedir.getAbsolutePath() + File.separator + String.join(File.separator, pathElements));
    }

    /**
     * v
     * Get the file and create it if it doesn't exist
     *
     * @param basedir      The base directory to get the file from
     * @param pathElements The path down the directory tree to the file
     */
    public static File getFileAndCreate(File basedir, String... pathElements) throws IOException {
        File file = getFile(basedir, pathElements);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    /**
     * Return all files in a directory -- null safe
     *
     * @param file Directory in which to search
     */
    public static File[] listFiles(File file) {
        File[] files = file.listFiles();
        return files == null ? new File[0] : files;
    }

    /**
     * Create a file if it doesn't exist
     *
     * @param file The file to create
     */
    public static boolean createFile(File file) throws IOException {
        if (file.exists()) {
            return false;
        }
        file = file.getAbsoluteFile();
        file.getParentFile().mkdirs();
        return file.createNewFile();
    }

    /**
     * Delete a file if it exists
     *
     * @param file The file to delete
     */
    public static void delete(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            Files.walkFileTree(file.toPath(), new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    File f = p.toFile();
                    if (f.isFile()) {
                        f.delete();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    dir.toFile().delete();
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            file.delete();
        }
    }

    /**
     * Write utf-8 content to a file
     *
     * @param data The utf-8 data to write
     * @param file The target file
     */
    public static void writeUTF8(String data, File file) throws IOException {
        if (!file.exists()) {
            createFile(file);
        }
        FileOutputStream ofstream = new FileOutputStream(file);
        ofstream.write(data.getBytes(StandardCharsets.UTF_8));
        ofstream.flush();
        ofstream.close();
    }

    /**
     * Write utf-8 content to a file -- handles {@link IOException}s
     *
     * @param data The utf-8 data to write
     * @param file The target file
     */
    public static void writeUTF8Safe(String data, File file) {
        try {
            writeUTF8(data, file);
        } catch (IOException e) {
            Logging.error("Failed to save " + file.getName() + ".");
            e.printStackTrace();
        }
    }

    /**
     * Read utf-8 content from a file
     *
     * @param file The target file to read
     */
    public static String readUTF8(File file) throws IOException {
        if (!file.exists()) {
            return "";
        }
        return Files.readString(file.toPath());
    }

    /**
     * Load JSON data for a class from a file
     *
     * @param clazz The class to attempt to load into
     * @param file  The target file to read
     */
    public static <T> T loadJson(Class<T> clazz, File file) {
        try {
            if (!file.exists()) {
                FileSystem.createFile(file);
                return ReflectionHelper.instantiate(clazz);
            }
            T obj = FarLands.getMoshi().adapter(clazz).fromJson(readUTF8(file));
            return obj == null ? ReflectionHelper.instantiate(clazz) : obj;
        } catch (IOException ex) {
            Logging.error("Failed to load " + file.getName() + ".\n" + ex.getMessage() + "\n" +
                          Arrays.stream(ex.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n")));
            ex.printStackTrace();
            return ReflectionHelper.instantiate(clazz);
        }
    }

    /**
     * Attempt to load JSON data for a type from a file
     *
     * @param type         The target type
     * @param defaultValue The default value if unable to read or the file doesn't exist
     * @param file         The target file to read
     */
    public static <T> T loadJson(Type type, T defaultValue, File file) {
        try {
            if (!file.exists()) {
                FileSystem.createFile(file);
                return defaultValue;
            }
            String emptyString = defaultValue instanceof Iterable ? "[]" : "{}";

            JsonAdapter<T> adapter = FarLands.getMoshi().adapter(type);
            String jsonData = readUTF8(file);
            T obj = adapter.fromJson(jsonData.isBlank() ? emptyString : jsonData);
            return obj == null ? defaultValue : obj;
        } catch (IOException ex) {
            Logging.error("Failed to load " + file.getName() + ".");
            ex.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * Attempt to save JSON data to a file
     *
     * @param object The data to save
     * @param file   The target file
     */
    public static <T> void saveJson(T object, File file) {
        saveJson(FarLands.getMoshi(), object, file);
    }

    /**
     * Attempt to save JSON data to a file
     *
     * @param object The data to save
     * @param file   the target file
     * @param indent If the file to should be indented and formatted nicely.
     */
    public static <T> void saveJson(T object, File file, boolean indent) {
        if (!indent) {
            saveJson(FarLands.getMoshi(), object, file);
            return;
        }
        JsonAdapter<T> adapter = FarLands.getMoshi().adapter((Type) object.getClass());
        adapter = adapter.indent("\t");
        writeUTF8Safe(adapter.toJson(object), file);
    }

    /**
     * Attempt to save JSON using a specific {@link Moshi} instance
     *
     * @param moshi  The {@link Moshi} instance to use
     * @param object The data to save
     * @param file   The target file
     */
    public static <T> void saveJson(Moshi moshi, T object, File file) {
        JsonAdapter<T> adapter = moshi.adapter((Type) object.getClass());
        writeUTF8Safe(adapter.toJson(object), file);
    }

    /**
     * Attempt to save JSON using a specific {@link JsonAdapter}
     *
     * @param adapter The {@link JsonAdapter} to use
     * @param object  The data to save
     * @param file    The target file
     */
    public static <T> void saveJson(JsonAdapter<T> adapter, T object, File file) {
        writeUTF8Safe(adapter.toJson(object), file);
    }
}
