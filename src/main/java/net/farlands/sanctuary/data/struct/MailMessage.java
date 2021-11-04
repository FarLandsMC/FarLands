package net.farlands.sanctuary.data.struct;

/**
 * A message being mailed.
 */
public record MailMessage(String sender, String message) {
    public MailMessage() {
        this(null, null);
    }
}
