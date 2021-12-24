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

/**
 * Handles writing and reading FL files.
 */
public final class FileSystem {
    private FileSystem() {
    }

    public static File getFile(File basedir, String... pathElements) {
        return new File(basedir.getAbsolutePath() + File.separator + String.join(File.separator, pathElements));
    }

    public static File getFileAndCreate(File basedir, String... pathElements) throws IOException {
        File file = getFile(basedir, pathElements);
        if (!file.exists())
            file.createNewFile();
        return file;
    }

    public static File[] listFiles(File file) { // Provide some null safety in this dangerous, nullable world
        File[] files = file.listFiles();
        return files == null ? new File[0] : files;
    }

    public static boolean createFile(File file) throws IOException {
        if (file.exists())
            return false;
        file = file.getAbsoluteFile();
        file.getParentFile().mkdirs();
        return file.createNewFile();
    }

    public static void delete(File file) throws IOException {
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            Files.walkFileTree(file.toPath(), new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    File f = p.toFile();
                    if (f.isFile())
                        f.delete();
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
        } else
            file.delete();
    }

    public static void writeUTF8(String data, File file) throws IOException {
        if (!file.exists())
            createFile(file);
        FileOutputStream ofstream = new FileOutputStream(file);
        ofstream.write(data.getBytes(StandardCharsets.UTF_8));
        ofstream.flush();
        ofstream.close();
    }

    public static void writeUTF8Safe(String data, File file) {
        try {
            writeUTF8(data, file);
        } catch (IOException e) {
            Logging.error("Failed to save " + file.getName() + ".");
            e.printStackTrace();
        }
    }

    public static String readUTF8(File file) throws IOException {
        if (!file.exists())
            return "";
        return Files.readString(file.toPath());
    }

    public static <T> T loadJson(Class<T> clazz, File file) {
        try {
            if (!file.exists()) {
                FileSystem.createFile(file);
                return ReflectionHelper.instantiate(clazz);
            }
            T obj = FarLands.getMoshi().adapter(clazz).fromJson(readUTF8(file));
            return obj == null ? ReflectionHelper.instantiate(clazz) : obj;
        } catch (IOException ex) {
            Logging.error("Failed to load " + file.getName() + ".");
            ex.printStackTrace();
            return ReflectionHelper.instantiate(clazz);
        }
    }

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

    public static <T> void saveJson(T object, File file) {
        saveJson(FarLands.getMoshi(), object, file);
    }

    public static <T> void saveJson(Moshi moshi, T object, File file) {
        JsonAdapter<T> adapter = moshi.adapter((Type) object.getClass());
        writeUTF8Safe(adapter.toJson(object), file);
    }

    public static <T> void saveJson(JsonAdapter<T> adapter, T object, File file) {
        writeUTF8Safe(adapter.toJson(object), file);
    }
}
