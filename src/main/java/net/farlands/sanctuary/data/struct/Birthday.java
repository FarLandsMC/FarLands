package net.farlands.sanctuary.data.struct;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Represents a player birthday!
 */
public class Birthday {
    private final int month;
    private final int day;

    public Birthday(int month, int day) {
        this.month = month - 1;
        this.day = day;
    }

    public long timeFromToday() {
        Calendar cal = Calendar.getInstance();
        try {
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DATE, day);
        } catch (ArrayIndexOutOfBoundsException ex) {
            return 0;
        }
        return cal.getTimeInMillis() - System.currentTimeMillis();
    }

    public boolean isToday() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.MONTH) == month && cal.get(Calendar.DATE) == day;
    }

    /**
     * Formats the Birthday as month + Day
     *
     * @param shortMonth Should the month be shortened to 3 chars?
     * @return Formatted String
     */
    public String toFormattedString(boolean shortMonth) {
        String[] months = !shortMonth ? new DateFormatSymbols().getMonths() : new DateFormatSymbols().getShortMonths();
        return months[month] + " " + day;
    }

    /**
     * Formats the Birthday as month + Day
     *
     * @return Formatted String
     */
    public String toFormattedString() {
        return toFormattedString(false);
    }
}
