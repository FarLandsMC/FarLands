package net.farlands.sanctuary.util;

import net.kyori.adventure.text.Component;

/**
 * Convert time units for readability
 */
public enum TimeInterval {
    SECOND(1, "second", "s"),
    MINUTE(60, "minute", "m"),
    HOUR(3600, "hour", "h"),
    DAY(86400, "day", "d"),
    WEEK(604800, "week", "wk"),
    MONTH(2592000, "month", "mt"),
    YEAR(31104000, "year", "yr");

    private final int    toSecondsFactor; // Seconds that the time unit represents
    private final String name; // Long name of the time unit
    private final String abbreviation; // Short name of the time unit

    public static final TimeInterval[] VALUES = values();

    TimeInterval(int toSecondsFactor, String name, String abbreviation) {
        this.toSecondsFactor = toSecondsFactor;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public int getToSecondsFactor() {
        return toSecondsFactor;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Format a time
     *
     * @param millis      Milliseconds
     * @param abbreviate  If the time units should be abbreviated, 'h' instead of 'hour'
     * @param minInterval The minimum interval to display
     */
    public static String formatTime(long millis, boolean abbreviate, TimeInterval minInterval) {
        long seconds = millis / 1000L;
        if (Math.round(seconds) < 1.0) {
            return "Now";
        }
        StringBuilder sb = new StringBuilder(0);
        TimeInterval currentInterval;
        for (int i = VALUES.length - 1; i >= minInterval.ordinal(); --i) {
            currentInterval = VALUES[i];
            int time = (int) (seconds / currentInterval.getToSecondsFactor());
            seconds %= currentInterval.getToSecondsFactor();
            if (time == 0 && minInterval.equals(currentInterval) && !minInterval.equals(VALUES[0]) && sb.length() == 0) {
                time = 1;
            }
            if (time > 0) {
                if (sb.length() > 0) {
                    if (!abbreviate && i == minInterval.ordinal()) {
                        sb.append(" and ");
                    } else {
                        sb.append(' ');
                    }
                }
                sb.append(time);
                if (abbreviate) {
                    sb.append(currentInterval.getAbbreviation());
                } else {
                    sb.append(' ').append(currentInterval.getName());
                    if (time > 1) {
                        sb.append('s');
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Format a time, with the minimum interval as {@link TimeInterval#SECOND}
     *
     * @param millis     Milliseconds
     * @param abbreviate If the time units should be abbreviated, 'h' instead of 'hour'
     */
    public static String formatTime(long millis, boolean abbreviate) {
        return formatTime(millis, abbreviate, VALUES[0]);
    }

    /**
     * Format time into a {@link Component}
     *
     * @param millis      Milliseconds
     * @param abbreviate  If the time units should be abbreviated, 'h' instead of 'hour'
     * @param minInterval The minimum interval to display
     */
    public static Component formatTimeComponent(long millis, boolean abbreviate, TimeInterval minInterval) {
        return Component.text(formatTime(millis, abbreviate, minInterval));
    }

    /**
     * Format time into a {@link Component} with the minimum interval as {@link TimeInterval#SECOND}
     *
     * @param millis     Milliseconds
     * @param abbreviate If the time units should be abbreviated, 'h' instead of 'hour'
     */
    public static Component formatTimeComponent(long millis, boolean abbreviate) {
        return Component.text(formatTime(millis, abbreviate));
    }

    /**
     * Parse seconds from a formatted time interval string
     *
     * @param time The string representing the time interval
     * @return
     */
    public static long parseSeconds(String time) {
        if (time == null || time.isEmpty()) {
            return -1L;
        }
        if ("now".equalsIgnoreCase(time)) {
            return 0L;
        }
        long totalSeconds = 0L;
        String[] timeElements = time.split(" ");
        for (String te : timeElements) {
            TimeInterval ti = null;
            for (TimeInterval ti0 : VALUES) {
                if (te.endsWith(ti0.getAbbreviation())) {
                    ti = ti0;
                }
            }
            if (ti == null) {
                return -1L;
            }
            int t;
            try {
                t = Integer.parseInt(te.substring(0, te.length() - ti.getAbbreviation().length()));
            } catch (NumberFormatException ex) {
                return -1L;
            }
            totalSeconds += (long) t * ti.getToSecondsFactor();
        }
        return totalSeconds;
    }
}
