package net.farlands.odyssey.discord;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.FLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageChannelHandler {
    private final Map<String, MessageChannel> channels;
    private final Map<String, List<String>> buffers;

    public MessageChannelHandler() {
        this.channels = new HashMap<>();
        this.buffers = new HashMap<>();
    }

    public void startTicking() {
        FarLands.getScheduler().scheduleAsyncRepeatingTask(this::flush, 0L, 5L);
    }

    private synchronized void flush() {
        StringBuilder sb = new StringBuilder();
        channels.forEach((key, channel) -> {
            buffers.get(key).forEach(msg -> {
                if(sb.length() + msg.length() > 1999) {
                    for(int i = 0;i < msg.length();i += 1999) {
                        if(sb.length() > 0) {
                            channel.sendMessage(sb).queue();
                            sb.setLength(0);
                        }
                        sb.append(msg.substring(i, Math.min(i + 1999, msg.length())).trim()).append('\n');
                    }
                }else
                    sb.append(msg.trim()).append('\n');
            });
            if(sb.length() > 0 && !sb.toString().matches("\\s+")) {
                channel.sendMessage(sb).queue();
                sb.setLength(0);
            }
            buffers.get(key).clear();
        });
    }

    public void setChannel(String name, MessageChannel channel) {
        channels.put(name, channel);
        buffers.put(name, new ArrayList<>());
    }

    public MessageChannel getChannel(String name) {
        return channels.get(name);
    }

    public synchronized void sendMessage(MessageChannel channel, String message) {
        String key = FLUtils.getKey(channels, channel);
        if(key == null)
            channel.sendMessage(message).queue();
        else
            sendMessage(key, message);
    }

    public synchronized void sendMessage(final String channel, String message) {
        List<String> buffer = buffers.get(channel);
        if(buffer == null)
            return;
        buffer.add(message);
    }
}
