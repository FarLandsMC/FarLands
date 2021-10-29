package net.farlands.sanctuary.data.struct;

/**
 * A message being mailed.
 */
public class MailMessage {
    private final String sender;
    private final String message;

    public MailMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    MailMessage() {
        this(null, null);
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return sender + ": " + message;
    }
}
