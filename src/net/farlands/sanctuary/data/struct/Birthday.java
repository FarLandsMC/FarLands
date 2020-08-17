package net.farlands.sanctuary.data.struct;

import java.util.Calendar;

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

    @Override
    public String toString() {
        return (month + 1) + "/" + day;
    }
}
