package net.farlands.odyssey.util;

public enum TimeInterval {
    SECOND(1, "second", "s"),
    MINUTE(60, "minute", "m"),
    HOUR(3600, "hour", "h"),
    DAY(86400, "day", "d"),
    WEEK(604800, "week", "wk"),
    MONTH(2592000, "month", "mt"),
    YEAR(31104000, "year", "yr");

    private final int toSecondsFactor;
    private final String name;
    private final String abbreviation;

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

    public static String formatTime(long millis, boolean abbreviate, TimeInterval minInterval) {
        long seconds = millis / 1000L;
        if(Math.round(seconds) < 1.0)
            return "Now";
        StringBuilder sb = new StringBuilder(0);
        TimeInterval currentInterval;
        for(int i = VALUES.length - 1;i >= minInterval.ordinal();-- i) {
            currentInterval = VALUES[i];
            int time = (int)(seconds / currentInterval.getToSecondsFactor());
            seconds %= currentInterval.getToSecondsFactor();
            if(time == 0 && minInterval.equals(currentInterval) && !minInterval.equals(VALUES[0]) && sb.length() == 0)
                time = 1;
            if(time > 0) {
                if(sb.length() > 0) {
                    if(!abbreviate && i == minInterval.ordinal())
                        sb.append(" and ");
                    else
                        sb.append(' ');
                }
                sb.append(time);
                if(abbreviate)
                    sb.append(currentInterval.getAbbreviation());
                else{
                    sb.append(' ').append(currentInterval.getName());
                    if(time > 1)
                        sb.append('s');
                }
            }
        }
        return sb.toString();
    }

    public static String formatTime(long millis, boolean abbreviate) {
        return formatTime(millis, abbreviate, VALUES[0]);
    }

    public static long parseSeconds(String time) {
        if(time == null || time.isEmpty())
            return -1L;
        if("now".equalsIgnoreCase(time))
            return 0L;
        long totalSeconds = 0L;
        String[] timeElements = time.split(" ");
        for(String te : timeElements) {
            TimeInterval ti = null;
            for(TimeInterval ti0 : VALUES) {
                if(te.endsWith(ti0.getAbbreviation()))
                    ti = ti0;
            }
            if(ti == null)
                return -1L;
            int t;
            try {
                t = Integer.parseInt(te.substring(0, te.length() - ti.getAbbreviation().length()));
            }catch(NumberFormatException ex) {
                return -1L;
            }
            totalSeconds += t * ti.getToSecondsFactor();
        }
        return totalSeconds;
    }
}
