package net.farlands.sanctuary.data.struct;

public class MailMessage {
    private String sender;
    private String message;

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
