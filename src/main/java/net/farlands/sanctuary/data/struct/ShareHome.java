package net.farlands.sanctuary.data.struct;

import java.util.Objects;

/**
 * Represents a shared home.
 */
public final class ShareHome {

    private final String sender;
    private final String message;
    private final Home home;

    /**
     */
    public ShareHome(String sender, String message, Home home) {
        this.sender = sender;
        this.message = message;
        this.home = home;
    }

    public String sender() {
        return sender;
    }

    public String message() {
        return message;
    }

    public Home home() {
        return home;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ShareHome) obj;
        return Objects.equals(this.sender, that.sender) &&
               Objects.equals(this.message, that.message) &&
               Objects.equals(this.home, that.home);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, message, home);
    }

    @Override
    public String toString() {
        return "ShareHome[" +
               "sender=" + sender + ", " +
               "message=" + message + ", " +
               "home=" + home + ']';
    }
}
