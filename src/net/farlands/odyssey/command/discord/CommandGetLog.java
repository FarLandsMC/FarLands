package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.FileSystem;
import net.farlands.odyssey.util.Utils;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CommandGetLog extends DiscordCommand {
    public CommandGetLog() {
        super(Rank.ADMIN, "Get the server logs over a range of time.", "/getlog <startDate> [endDate]", "getlog");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int start, end;
        if (args.length == 0 || args[0].equalsIgnoreCase("latest"))
            start = end = parseDate(Utils.dateToString(System.currentTimeMillis() - 21600000L, "yyyy-MM-dd"));
        else {
            try {
                start = parseDate(args[0]);
                end = args.length == 1 ? start : parseDate(args[1]);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage("Invalid date \"" + ex.getMessage() + "\". Expected format: yyyy-mm-dd.");
                return true;
            }
        }

        (new LogBuilder((DiscordSender) sender, start, end)).start();
        return true;
    }

    private static int parseDate(String date) throws IllegalArgumentException {
        String[] data = date.substring(0, Utils.indexOfDefault(date.indexOf('.'), date.length())).split("-");
        if (data.length < 3)
            throw new IllegalArgumentException(date);
        try {
            return Integer.parseInt(data[0]) * 10000 + Integer.parseInt(data[1]) * 100 + Integer.parseInt(data[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(date);
        }
    }

    private static int getLogNumber(String name) {
        return parseDate(name) * 100 + Integer.parseInt(name.substring(name.lastIndexOf('-') + 1, name.indexOf('.')));
    }

    private static class LogBuilder extends Thread {
        final DiscordSender sender;
        final int start, end;

        LogBuilder(DiscordSender sender, int start, int end) {
            this.sender = sender;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                File file = FarLands.getDataHandler().getTempFile("log-" + (start == end ? start : start + "-" + end) +
                        ".log.gz");

                OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[65536];

                Arrays.stream(FileSystem.listFiles(new File(System.getProperty("user.dir") +
                        File.separator + "logs"))).filter(f -> {
                    if (!f.isFile() || "latest.log".equals(f.getName()))
                        return false;
                    int date = parseDate(f.getName());
                    return date >= start && date <= end;
                }).sorted(Comparator.comparingInt(f -> getLogNumber(f.getName()))).forEach(f -> {
                    copy(f, true, out, buffer);
                });

                if (end == parseDate(Utils.dateToString(System.currentTimeMillis() - 21600000L, "yyyy-MM-dd"))) {
                    File latest = new File(String.join(File.separator, System.getProperty("user.dir"),
                            "logs", "latest.log"));
                    if (latest.exists())
                        copy(latest, false, out, buffer);
                }

                out.close();

                sender.getChannel().sendFile(file).complete();
                file.delete();
            } catch (Throwable ex) {
                sender.sendMessage("Failed to retrieve log.");
                ex.printStackTrace(System.out);
            }
        }

        private static void copy(File input, boolean compressed, OutputStream out, byte[] buffer) {
            try {
                InputStream in = compressed ? new GZIPInputStream(new FileInputStream(input))
                        : new FileInputStream(input);
                int len;
                while ((len = in.read(buffer)) > 0)
                    out.write(buffer, 0, len);
                in.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
