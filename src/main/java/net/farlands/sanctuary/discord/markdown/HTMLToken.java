package net.farlands.sanctuary.discord.markdown;

public class HTMLToken {
    private final boolean isTag;
    private final String text;

    private HTMLToken(boolean tag, String value) {
        isTag = tag;
        text = value;
    }

    public static HTMLToken tag(String text) {
        return new HTMLToken(true, text);
    }

    public static HTMLToken text(String text) {
        return new HTMLToken(false, text);
    }

    /**
     * @return true if this is a tag, false if it's text.
     */
    public boolean isTag() {
        return isTag;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        String type;
        if (isTag()) {
            type = "tag";
        } else {
            type = "text";
        }
        return type + ": " + getText();
    }
}
