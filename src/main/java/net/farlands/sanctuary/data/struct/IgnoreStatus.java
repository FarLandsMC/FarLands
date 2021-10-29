package net.farlands.sanctuary.data.struct;

import com.kicas.rp.util.Utils;

/**
 * Ignore options and status.
 */
public class IgnoreStatus {
    private int flags;

    public static final IgnoreStatus NONE = new IgnoreStatus();

    public IgnoreStatus() {
        this.flags = 0;
    }

    public boolean includesChat() {
        return isSet(IgnoreType.CHAT) || includesAll();
    }

    public boolean includesTeleports() {
        return isSet(IgnoreType.TELEPORTS) || includesAll();
    }

    public boolean includesPackages() {
        return isSet(IgnoreType.PACKAGES) || includesAll();
    }

    public boolean includesSharehomes() {
        return isSet(IgnoreType.SHAREHOMES) || includesAll();
    }

    public boolean includesAll() {
        return isSet(IgnoreType.ALL);
    }

    public boolean includesNone() {
        return (flags & IgnoreType.maskAll()) == 0;
    }

    public void set(IgnoreType type, boolean value) {
        // Toggle all on or all off
        if (type == IgnoreType.ALL)
            flags = value ? ~0 : 0;
            // Toggle one flag on or off
        else {
            // If all flags are on, then toggle off the "all" flag and the specified flag
            if (includesAll() && !value)
                flags = ~(type.flag() | IgnoreType.ALL.flag());
                // Toggle on or off the single flag
            else {
                if (value)
                    flags |= type.flag();
                else
                    flags &= ~type.flag();
            }
        }
    }

    public boolean isSet(IgnoreType type) {
        return (flags & type.flag()) != 0;
    }

    public enum IgnoreType {
        CHAT,
        TELEPORTS,
        PACKAGES,
        SHAREHOMES,
        ALL;

        private int flag() {
            return 1 << this.ordinal();
        }

        public String toFormattedString() {
            if (this == ALL)
                return "everything";
            else
                return Utils.formattedName(this);
        }

        public static int maskAll() {
            return (1 << values().length) - 1;
        }
    }
}
