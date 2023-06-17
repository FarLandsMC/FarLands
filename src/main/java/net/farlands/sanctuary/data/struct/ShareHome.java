package net.farlands.sanctuary.data.struct;

import java.util.Objects;

/**
 * Represents a shared home.
 */
public record ShareHome(String sender, String message, Home home) {

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
    public String toString() {
        return "ShareHome[" +
               "sender=" + sender + ", " +
               "message=" + message + ", " +
               "home=" + home + ']';
    }
}
