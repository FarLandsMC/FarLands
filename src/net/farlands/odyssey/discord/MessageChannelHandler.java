package net.farlands.odyssey.discord;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.util.FLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageChannelHandler {
    private final Map<DiscordChannel, MessageChannel> channels;
    private final Map<DiscordChannel, List<String>> buffers;

    public MessageChannelHandler() {
        this.channels = new HashMap<>();
        this.buffers = new HashMap<>();
    }

    public void startTicking() {
        FarLands.getScheduler().scheduleAsyncRepeatingTask(this::flush, 0L, 5L);
    }

    private synchronized void flush() {
        StringBuilder sb = new StringBuilder();
        channels.forEach((channel, messageChannel) -> {
            buffers.get(channel).forEach(messageBuffer -> {
                if(sb.length() + messageBuffer.length() > 1999) {
                    for(int i = 0;i < messageBuffer.length();i += 1999) {
                        if(sb.length() > 0) {
                            messageChannel.sendMessage(sb).queue();
                            sb.setLength(0);
                        }
                        sb.append(messageBuffer.substring(i, Math.min(i + 1999, messageBuffer.length())).trim()).append('\n');
                    }
                }else
                    sb.append(messageBuffer.trim()).append('\n');
            });

            if(sb.length() > 0 && !sb.toString().matches("\\s+")) {
                messageChannel.sendMessage(sb).queue();
                sb.setLength(0);
            }

            buffers.get(channel).clear();
        });
    }

    public void setChannel(DiscordChannel channel, MessageChannel messageChannel) {
        channels.put(channel, messageChannel);
        buffers.put(channel, new ArrayList<>());
    }

    public MessageChannel getChannel(DiscordChannel channel) {
        return channels.get(channel);
    }

    public synchronized void sendMessage(MessageChannel channel, String message) {
        DiscordChannel key = FLUtils.getKey(channels, channel);
        if(key == null)
            channel.sendMessage(message).queue();
        else
            sendMessage(key, message);
    }

    public synchronized void sendMessage(DiscordChannel channel, String message) {
        List<String> buffer = buffers.get(channel);
        if(buffer == null)
            return;

        buffer.add(message);
    }
}
