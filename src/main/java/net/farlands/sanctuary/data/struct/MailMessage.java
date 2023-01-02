package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A message being mailed.
 */
public final class MailMessage implements ComponentLike {

    public static final Component UNREAD_MAIL = ComponentColor.gold("You have mail. Read it with ").append(ComponentUtils.command("/mail read"));
    private final       String    sender;
    private final       Component message;

    /**
     *
     */
    public MailMessage(String sender, Component message) {
        this.sender = sender;
        this.message = message;
    }

    public MailMessage() {
        this(null, null);
    }

    public String sender() {
        return sender;
    }

    public Component message() {
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

    @Override
    public @NotNull Component asComponent() {
        return Component.empty()
            .append(FarLands.getDataHandler().getOfflineFLPlayer(this.sender))
            .append(Component.text(": "))
            .append(this.message);
    }
}
