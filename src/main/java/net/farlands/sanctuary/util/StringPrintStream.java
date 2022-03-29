package net.farlands.sanctuary.util;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Simple PrintStream that can be converted to a String
 */
public class StringPrintStream extends PrintStream {

    private final ByteArrayOutputStream byteArrayOutputStream;

    public StringPrintStream() {
        this(new ByteArrayOutputStream());
    }

    private StringPrintStream(@NotNull ByteArrayOutputStream byteArrayOutputStream) {
        super(byteArrayOutputStream, true, StandardCharsets.UTF_8);
        this.byteArrayOutputStream = byteArrayOutputStream;
    }

    @Override
    public String toString() {
        return this.byteArrayOutputStream.toString();
    }
}