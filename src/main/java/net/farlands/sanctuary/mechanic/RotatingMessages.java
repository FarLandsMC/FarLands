package net.farlands.sanctuary.mechanic;

import net.farlands.sanctuary.FarLands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle rotating chat messages
 */
public class RotatingMessages extends Mechanic {

    @Override
    public void onStartup() {
        FarLands.getFLConfig().rotatingMessages.stream().map(MiniMessage.miniMessage()::parse).forEach(rotatingMessages::add);

        // Wait for any dynamically added messages to be registered
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), this::scheduleRotatingMessages, 15L * 20L);
    }

    private final List<Component> rotatingMessages = new ArrayList<>();

    private void scheduleRotatingMessages() {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            int messageCount = rotatingMessages.size();
            int rotatingMessageGap = FarLands.getFLConfig().rotatingMessageGap * 60 * 20;
            for (int i = 0; i < messageCount; ++i) {
                Component message = rotatingMessages.get(i);
                Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    FarLands.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message)),
                    (long) i * rotatingMessageGap + 600, (long) messageCount * rotatingMessageGap);
            }
        }, 5L);
    }

    public void addRotatingMessage(String s) {
        rotatingMessages.add(MiniMessage.miniMessage().parse(s));}
}
