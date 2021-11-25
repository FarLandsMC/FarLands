package net.farlands.sanctuary.chat;

/**
 * Control the limits and functions of chat
 */
public class ChatControl {

    public static String limitFlood(String message) {
        int row = 0;
        char last = ' ';
        StringBuilder output = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.toLowerCase(c) == last) {
                if (++row < 4) {
                    output.append(c);
                }
            } else {
                last = Character.toLowerCase(c);
                output.append(c);
                row = 0;
            }
        }
        return output.toString();
    }

    public static String limitCaps(String message) {
        if (message.length() < 6) {
            return message;
        }

        float uppers = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c)) {
                ++uppers;
            }
        }

        if (uppers / message.length() >= 5f / 12) {
            return message.substring(0, 1).toUpperCase() + message.substring(1).toLowerCase();
        } else {
            return message;
        }
    }
}
