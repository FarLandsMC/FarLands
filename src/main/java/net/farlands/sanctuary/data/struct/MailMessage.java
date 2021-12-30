package net.farlands.sanctuary.data.struct;

import java.util.Objects;

/**
 * A message being mailed.
 */
public final class MailMessage {

    private final String sender;
    private final String message;

    /**
     */
    public MailMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public MailMessage() {
        this(null, null);
    }

    public String sender() {
        return sender;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MailMessage) obj;
        return Objects.equals(this.sender, that.sender) &&
               Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, message);
    }

    @Override
    public String toString() {
        return "MailMessage[" +
               "sender=" + sender + ", " +
               "message=" + message + ']';
    }

}
