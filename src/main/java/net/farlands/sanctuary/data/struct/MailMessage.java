package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * A message being mailed.
 */
public final class MailMessage implements ComponentLike {

    public static final       Component UNREAD_MAIL = ComponentColor.gold("You have mail. Read it with ").append(ComponentUtils.command("/mail read"));
    private final @Deprecated String    sender;
    private final             UUID      senderUUID;
    private final             Component message;

    /**
     *
     */
    public MailMessage(UUID sender, Component message) {
        this.senderUUID = sender;
        this.sender = FarLands.getDataHandler().getOfflineFLPlayer(sender) == null // Mainly just for backwards compatability with before `senderUUID` was added
            ? "Unknown"
            : FarLands.getDataHandler().getOfflineFLPlayer(sender).username;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MailMessage that = (MailMessage) o;

        if (!Objects.equals(sender, that.sender)) return false;
        if (!Objects.equals(senderUUID, that.senderUUID)) return false;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = this.sender != null ? this.sender.hashCode() : 0;
        result = 31 * result + (this.senderUUID != null ? this.senderUUID.hashCode() : 0);
        result = 31 * result + (this.message != null ? this.message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailMessage{" +
               "sender='" + sender + '\'' +
               ", senderUUID=" + senderUUID +
               ", message=" + message +
               '}';
    }

    @Override
    public @NotNull Component asComponent() {
        OfflineFLPlayer flp = senderUUID == null
            ? FarLands.getDataHandler().getOfflineFLPlayer(this.sender)
            : FarLands.getDataHandler().getOfflineFLPlayer(this.senderUUID);
        return Component.empty()
            .append(flp == null ? Component.text(this.sender == null ? "Unknown" : this.sender) : flp)
            .append(Component.text(": "))
            .append(this.message);
    }

    public UUID senderUUID() {
        return this.senderUUID;
    }
}
