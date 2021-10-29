package net.farlands.sanctuary.data.struct;

/**
 * Represents a shared home.
 */
public class ShareHome {

    public final String sender;
    public final String message;
    public final Home home;

    public ShareHome(String sender, String message, Home home) {
        this.sender = sender;
        this.message = message;
        this.home = home;
    }

    @Override
    public String toString() {
        return "HomeShare[" +
                "sender:'" + sender + "'" +
                ", message:'" + message + "'" +
                ", home:" + home + ']';
    }
}
