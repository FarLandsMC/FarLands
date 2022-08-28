package net.farlands.sanctuary.command.discord;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.FileSystem;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CommandGetLog extends DiscordCommand {
    public CommandGetLog() {
        super(Rank.ADMIN, "Get the server logs over a range of time.", "/getlog [startDate=current-date] [endDate=startDate]", "getlog");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof DiscordSender)) {
            return error(sender, "This command must be used from Discord.");
        }

        // Get a numeric representation of the start and end dates
        int start, end;
        if (args.length == 0 || args[0].equalsIgnoreCase("latest"))
            start = end = parseDate(FLUtils.dateToString(System.currentTimeMillis() - 21600000L, "yyyy-MM-dd"));
        else {
            try {
                start = parseDate(args[0]);
                end = args.length == 1 ? start : parseDate(args[1]);
            } catch (IllegalArgumentException ex) {
                return error(sender, "Invalid date \"%s\".  Expected format: yyyy-mm-dd", ex.getMessage());
            }
        }

        (new LogBuilder((DiscordSender) sender, start, end)).start();
        return true;
    }

    // Convert a date (year, month, day) into an integer that reflects ordering
    private static int parseDate(String date) throws IllegalArgumentException {
        String[] data = date.substring(0, FLUtils.indexOfDefault(date.indexOf('.'), date.length())).split("-");
        if (data.length < 3)
            throw new IllegalArgumentException(date);
        try {
            return Integer.parseInt(data[0]) * 10000 + Integer.parseInt(data[1]) * 100 + Integer.parseInt(data[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(date);
        }
    }

    // Combine a log's date and number into an integer that reflects ordering
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
                File file = FarLands.getDataHandler().getTempFile("log-" + (start == end ? start : start + "-" + end) + ".log.gz");

                OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[4096];

                Arrays.stream(FileSystem.listFiles(new File(System.getProperty("user.dir") + File.separator + "logs"))).filter(f -> {
                    if (!f.isFile() || "latest.log".equals(f.getName()))
                        return false;

                    // Ensure the logs are in the correct date range
                    int date = parseDate(f.getName());
                    return start <= date && date <= end;
                }).sorted(Comparator.comparingInt(f -> getLogNumber(f.getName()))).forEach(f -> {
                    copy(f, true, out, buffer);
                });

                // Append latest.log if needed
                if (end == parseDate(FLUtils.dateToString(System.currentTimeMillis() - 21600000L, "yyyy-MM-dd"))) {
                    File latest = new File(String.join(File.separator, System.getProperty("user.dir"), "logs", "latest.log"));
                    if (latest.exists())
                        copy(latest, false, out, buffer);
                }

                out.close();

                sender.getChannel().sendFile(file).complete();
                file.delete();
            } catch (Throwable ex) {
                sender.sendMessage("Failed to retrieve log.");
                ex.printStackTrace();
            }
        }

        // Performs a stream copy using the given buffer
        private static void copy(File input, boolean compressed, OutputStream out, byte[] buffer) {
            try {
                InputStream in = compressed
                        ? new GZIPInputStream(new FileInputStream(input))
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
