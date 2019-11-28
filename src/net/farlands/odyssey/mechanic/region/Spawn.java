package net.farlands.odyssey.mechanic.region;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.FLCommandEvent;
import net.farlands.odyssey.command.player.CommandSetHome;
import net.farlands.odyssey.mechanic.Mechanic;
import net.farlands.odyssey.util.FireworkBuilder;
import net.farlands.odyssey.util.FireworkExplosionType;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import static net.farlands.odyssey.util.Utils.region;

import java.util.*;

public class Spawn extends Mechanic {
    private final ElytraCourse elytraCourse;

    private static final List<Pair<Location, Location>> END_PORTAL = Arrays.asList(
        region(-116, 130, -76, -109, 137, -67)
    );
    private static final Location ELYTRA_COURSE_RESPAWN = new Location(Bukkit.getWorld("world"), 96.5, 204.1, 69.5, 270, 0);
    private static final List<Pair<Location, Location>> ELYTRA_COURSE_REGION = Collections.singletonList(region(92, 0, -28, 412, 255, 325));
    private static final List<Pair<Location, Location>> ELYTRA_COURSE_START = Collections.singletonList(region(105, 199, 66, 106, 205, 72));
    private static final Location ELYTRA_COURSE_FIREWORK_SPAWN_CENTER = new Location(Bukkit.getWorld("world"), 335.5, 195.5, 305.5);
    private static final List<List<Pair<Location, Location>>> ELYTRA_CHECK_POINTS = Arrays.asList(
        Collections.singletonList(region(129, 195, 78, 130, 201, 84)),
        Arrays.asList(region(148, 199, 73, 149, 202, 74), region(149, 198, 74, 150, 203, 75),
                region(150, 197, 74, 151, 204, 75), region(151, 197, 75, 152, 204, 76),
                region(152, 198, 75, 153, 203, 76), region(153, 199, 76, 154, 202, 77),
                region(149, 199, 73, 150, 202, 74), region(150, 198, 74, 151, 203, 75),
                region(151, 197, 74, 152, 204, 75), region(152, 197, 75, 153, 204, 76),
                region(153, 198, 75, 154, 203, 76), region(154, 199, 76, 155, 202, 77)),
        Collections.singletonList(region(154, 200, 48, 160, 206, 49)),
        Arrays.asList(region(165, 204, 19, 166, 207, 20), region(165, 203, 20, 166, 208, 21),
                region(166, 202, 22, 167, 209, 23), region(167, 202, 22, 168, 209, 23),
                region(168, 203, 23, 169, 208, 24), region(168, 204, 24, 169, 207, 25),
                region(166, 202, 21, 167, 209, 22), region(166, 204, 19, 167, 207, 20),
                region(166, 203, 20, 167, 208, 21), region(167, 202, 22, 168, 209, 23),
                region(168, 202, 22, 169, 209, 23), region(169, 203, 23, 170, 208, 24),
                region(169, 204, 24, 170, 207, 25), region(167, 202, 21, 168, 209, 22)),
        Collections.singletonList(region(198, 210, 14, 199, 216, 20)),
        Collections.singletonList(region(221, 209, 14, 222, 214, 18)),
        Collections.singletonList(region(240, 206, 8, 241, 212, 15)),
        Collections.singletonList(region(255, 202, 11, 256, 207, 16)),
        Collections.singletonList(region(294, 192, 30, 296, 199, 38)),
        Collections.singletonList(region(312, 194, 52, 317, 199, 56)),
        Collections.singletonList(region(321, 200, 71, 326, 204, 73)),
        Collections.singletonList(region(326, 200, 99, 333, 207, 100)),
        Collections.singletonList(region(324, 203, 125, 330, 209, 126)),
        Collections.singletonList(region(319, 207, 154, 325, 213, 155)),
        Arrays.asList(region(310, 208, 168, 311, 211, 169), region(311, 207, 169, 312, 213, 170),
                region(312, 206, 169, 313, 213, 170), region(313, 206, 170, 314, 213, 171),
                region(314, 206, 170, 315, 212, 171), region(315, 207, 171, 316, 211, 172),
                region(311, 208, 168, 312, 211, 169), region(312, 207, 169, 313, 213, 170),
                region(313, 206, 169, 314, 213, 170), region(314, 206, 170, 315, 213, 171),
                region(315, 206, 170, 316, 212, 171), region(316, 207, 171, 317, 211, 172)),
        Collections.singletonList(region(290, 205, 174, 291, 211, 180)),
        Collections.singletonList(region(271, 203, 177, 272, 209, 183)),
        Collections.singletonList(region(249, 204, 178, 250, 210, 184)),
        Arrays.asList(region(226, 209, 169, 227, 212, 170), region(227, 208, 168, 228, 213, 169),
                region(228, 207, 167, 229, 214, 168), region(228, 207, 166, 229, 214, 167),
                region(228, 208, 165, 229, 213, 166), region(229, 209, 164, 230, 212, 165),
                region(227, 209, 169, 228, 212, 170), region(228, 208, 168, 229, 213, 169),
                region(229, 207, 167, 230, 214, 168), region(229, 207, 166, 230, 214, 167),
                region(229, 208, 165, 230, 213, 166), region(230, 209, 164, 231, 212, 165)),
        Collections.singletonList(region(221, 211, 148, 227, 217, 149)),
        Arrays.asList(region(229, 218, 131, 230, 221, 132), region(230, 217, 132, 231, 222, 133),
                region(231, 216, 132, 232, 223, 133), region(232, 216, 133, 233, 223, 134),
                region(233, 217, 134, 234, 222, 135), region(234, 218, 134, 235, 221, 135),
                region(230, 218, 131, 231, 221, 132), region(231, 217, 132, 232, 222, 133),
                region(232, 216, 132, 233, 223, 133), region(233, 216, 133, 234, 223, 134),
                region(234, 217, 134, 235, 222, 135), region(235, 218, 134, 236, 221, 135)),
        Collections.singletonList(region(250, 208, 123, 251, 214, 129)),
        Arrays.asList(region(266, 203, 130, 267, 206, 131), region(265, 201, 131, 266, 208, 132),
                region(264, 201, 131, 265, 208, 132), region(263, 201, 132, 264, 208, 133),
                region(262, 201, 132, 263, 208, 133), region(261, 203, 133, 262, 206, 134),
                region(267, 203, 130, 268, 206, 131), region(266, 201, 131, 267, 208, 132),
                region(265, 201, 131, 266, 208, 132), region(264, 201, 132, 265, 208, 133),
                region(263, 201, 132, 264, 208, 133), region(262, 203, 133, 263, 206, 134)),
        Collections.singletonList(region(263, 196, 147, 270, 202, 148)),
        Collections.singletonList(region(251, 193, 158, 252, 199, 165)),
        Collections.singletonList(region(214, 186, 158, 215, 192, 164)),
        Collections.singletonList(region(175, 182, 160, 176, 188, 166)),
        Arrays.asList(region(150, 179, 174, 151, 182, 175), region(149, 178, 173, 150, 183, 174),
                region(148, 177, 173, 149, 184, 174), region(147, 177, 172, 148, 184, 173),
                region(146, 178, 172, 147, 183, 173), region(145, 179, 171, 146, 182, 172),
                region(151, 179, 174, 152, 182, 175), region(150, 178, 173, 151, 183, 174),
                region(149, 177, 173, 150, 184, 174), region(148, 177, 172, 149, 184, 173),
                region(147, 178, 172, 148, 183, 173), region(146, 179, 171, 147, 182, 172)),
        Arrays.asList(region(148, 179, 187, 149, 182, 188), region(147, 177, 188, 148, 184, 189),
                region(146, 177, 188, 147, 184, 189), region(145, 177, 189, 146, 184, 190),
                region(144, 177, 189, 145, 184, 190), region(143, 179, 190, 144, 182, 191),
                region(149, 179, 187, 150, 182, 188), region(148, 177, 188, 149, 184, 189),
                region(147, 177, 188, 148, 184, 189), region(146, 177, 189, 147, 184, 190),
                region(145, 177, 189, 146, 184, 190), region(144, 179, 190, 145, 182, 191)),
        Collections.singletonList(region(178, 183, 194, 179, 189, 200)),
        Collections.singletonList(region(207, 191, 202, 208, 197, 208)),
        Arrays.asList(region(222, 190, 204, 223, 193, 205), region(222, 189, 205, 223, 194, 206),
                region(222, 188, 206, 223, 195, 207), region(222, 188, 207, 223, 195, 208),
                region(221, 188, 208, 222, 195, 209), region(221, 188, 209, 222, 195, 210),
                region(221, 189, 210, 222, 194, 211), region(221, 190, 211, 222, 193, 212),
                region(223, 190, 204, 224, 193, 205), region(223, 189, 205, 224, 194, 206),
                region(223, 188, 206, 224, 195, 207), region(223, 188, 207, 224, 195, 208),
                region(222, 188, 208, 223, 195, 209), region(222, 188, 209, 223, 195, 210),
                region(222, 189, 210, 223, 194, 211), region(222, 190, 211, 223, 193, 212)),
        Arrays.asList(region(236, 195, 216, 237, 198, 217), region(235, 193, 217, 236, 200, 218),
                region(234, 193, 217, 235, 200, 218), region(235, 193, 218, 236, 200, 219),
                region(232, 193, 218, 233, 200, 219), region(231, 195, 219, 232, 198, 220),
                region(237, 195, 216, 238, 198, 217), region(236, 193, 217, 237, 200, 218),
                region(235, 193, 217, 236, 200, 218), region(236, 193, 218, 237, 200, 219),
                region(233, 193, 218, 234, 200, 219), region(232, 195, 219, 233, 198, 220),
                region(234, 193, 218, 235, 200, 219)),
        Arrays.asList(region(249, 197, 226, 250, 200, 227), region(248, 196, 227, 249, 202, 228),
                region(248, 195, 228, 249, 202, 229), region(247, 195, 229, 248, 202, 230),
                region(247, 195, 230, 248, 201, 231), region(246, 196, 231, 247, 200, 232),
                region(250, 197, 226, 251, 200, 227), region(249, 196, 227, 250, 202, 228),
                region(249, 195, 228, 250, 202, 229), region(248, 195, 229, 249, 202, 230),
                region(248, 195, 230, 249, 201, 231), region(247, 196, 231, 248, 200, 232)),
        Arrays.asList(region(262, 200, 242, 263, 203, 243), region(261, 198, 243, 262, 205, 244),
                region(260, 198, 243, 261, 205, 244), region(259, 198, 244, 260, 205, 245),
                region(258, 198, 244, 259, 205, 245), region(257, 200, 245, 258, 203, 246),
                region(263, 200, 242, 264, 203, 243), region(262, 198, 243, 263, 205, 244),
                region(261, 198, 243, 262, 205, 244), region(260, 198, 244, 261, 205, 245),
                region(259, 198, 244, 260, 205, 245), region(258, 200, 245, 259, 203, 246)),
        Collections.singletonList(region(291, 197, 249, 292, 204, 256)),
        Collections.singletonList(region(324, 193, 256, 325, 199, 262)),
        Arrays.asList(region(259, 197, 252, 260, 200, 253), region(359, 196, 253, 360, 201, 254),
                region(359, 195, 254, 360, 202, 255), region(359, 195, 255, 360, 202, 256),
                region(360, 195, 256, 361, 202, 257), region(360, 195, 257, 361, 202, 258),
                region(360, 196, 258, 361, 201, 259), region(360, 197, 259, 361, 200, 260),
                region(260, 197, 252, 261, 200, 253), region(360, 196, 253, 361, 201, 254),
                region(360, 195, 254, 361, 202, 255), region(360, 195, 255, 361, 202, 256),
                region(361, 195, 256, 362, 202, 257), region(361, 195, 257, 362, 202, 258),
                region(361, 196, 258, 362, 201, 259), region(361, 197, 259, 362, 200, 260)),
        Collections.singletonList(region(385, 201, 255, 386, 208, 262)),
        Arrays.asList(region(399, 204, 264, 400, 207, 265), region(398, 202, 265, 399, 209, 266),
                region(397, 202, 265, 398, 209, 266), region(396, 202, 266, 397, 209, 267),
                region(395, 202, 266, 396, 209, 267), region(394, 204, 267, 395, 207, 268),
                region(400, 204, 264, 401, 207, 265), region(399, 202, 265, 400, 209, 266),
                region(398, 202, 265, 399, 209, 266), region(397, 202, 266, 398, 209, 267),
                region(396, 202, 266, 397, 209, 267), region(395, 204, 267, 396, 207, 268)),
        Collections.singletonList(region(394, 202, 282, 402, 208, 283)),
        Arrays.asList(region(394, 202, 298, 395, 205, 299), region(393, 201, 297, 394, 206, 298),
                region(392, 200, 297, 393, 207, 298), region(391, 200, 296, 392, 207, 297),
                region(390, 201, 295, 391, 206, 296), region(289, 202, 295, 290, 205, 296),
                region(395, 202, 298, 396, 205, 299), region(394, 201, 297, 395, 206, 298),
                region(393, 200, 297, 394, 207, 298), region(392, 200, 296, 393, 207, 297),
                region(391, 201, 295, 392, 206, 296), region(290, 202, 295, 291, 205, 296)),
        Collections.singletonList(region(369, 198, 304, 370, 204, 310)),
        Collections.singletonList(region(247, 193, 302, 347, 200, 310))
    );

    public Spawn() {
        this.elytraCourse = new ElytraCourse();
    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if(CommandSetHome.class.equals(event.getCommand()) && event.getSender() instanceof Player &&
                Utils.isWithin(((Player)event.getSender()).getLocation(), END_PORTAL)) {
            event.getSender().sendMessage(ChatColor.RED + "You cannot set a home here.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        checkSpawnPortal(event.getFrom(), event.getTo(), event);
        elytraCourse.updateElytraCourse(event.getFrom(), event.getTo(), event);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        checkSpawnPortal(event.getFrom(), event.getTo(), event);
        elytraCourse.updateElytraCourse(event.getFrom(), event.getTo(), event);
    }

    private void checkSpawnPortal(Location from, Location to, PlayerMoveEvent event) {
        if(!Utils.isWithin(from, END_PORTAL) && Utils.isWithin(to, END_PORTAL)) {
            Player player = event.getPlayer();
            ItemStack stack = player.getInventory().getItemInMainHand();
            if(stack != null && Material.DIAMOND.equals(stack.getType()) && stack.getAmount() >= 5) {
                stack.setAmount(stack.getAmount() - 5);
                player.getInventory().setItemInMainHand(stack.getAmount() == 0 ? null : stack);
                player.sendMessage(ChatColor.GREEN + "Payment accepted. You may now use the portal.");
            }else{
                player.sendMessage(ChatColor.RED + "To enter this room, you must be holding at least 5 diamonds in your main hand. " +
                        "5 diamonds will be removed from your hand upon entering.");
                event.setCancelled(true);
            }
        }
    }

    private class ElytraCourse {
        final Map<UUID, Integer> elytraCoursePlayers;

        ElytraCourse() {
            this.elytraCoursePlayers = new HashMap<>();
        }

        void updateElytraCourse(Location from, Location to, PlayerMoveEvent event) {
            final Player player = event.getPlayer();

            if(Utils.isWithin(from, ELYTRA_COURSE_REGION) && !Utils.isWithin(to, ELYTRA_COURSE_REGION) && elytraCoursePlayers.containsKey(player.getUniqueId())) {
                elytraCoursePlayers.remove(player.getUniqueId());
                FarLands.getDataHandler().getRADH().store(false, "ingame", player.getUniqueId().toString());
                FarLands.getDataHandler().getRADH().removeCooldown("elytraCourse", player.getUniqueId().toString());
                player.sendMessage(ChatColor.GOLD + "Exiting the elytra course.");
                return;
            }

            if(event instanceof PlayerTeleportEvent)
                return;

            if(elytraCoursePlayers.getOrDefault(player.getUniqueId(), -1) >= 0) {
                if(to.getY() < 175 || !player.isGliding()) {
                    resetElytraCourse(player);
                    return;
                }
                int checkpoint = elytraCoursePlayers.get(player.getUniqueId());
                if(Utils.passedThrough(from, to, ELYTRA_CHECK_POINTS.get(checkpoint))) {
                    if(checkpoint == ELYTRA_CHECK_POINTS.size() - 1)
                        onElytraCourseCompleted(player);
                    else{
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
                        elytraCoursePlayers.put(player.getUniqueId(), checkpoint + 1);
                        FarLands.getDataHandler().getRADH().resetOrSetCooldown(100L, "elytraCourse", player.getUniqueId().toString(),
                                () -> resetElytraCourse(player));
                    }
                }
            }else if(Utils.passedThrough(from, to, ELYTRA_COURSE_START)) {
                if(elytraCoursePlayers.containsKey(player.getUniqueId())) {
                    FarLands.getDataHandler().getRADH().resetOrSetCooldown(100L, "elytraCourse", player.getUniqueId().toString(),
                            () -> resetElytraCourse(player));
                }else{
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
                    player.sendMessage(ChatColor.GREEN + "You have started the elytra course! You have 5 seconds to get from " +
                            "each ring to the next. Make it to the end to get a reward!");
                }
                FarLands.getDataHandler().getRADH().store(true, "ingame", player.getUniqueId().toString());
                elytraCoursePlayers.put(player.getUniqueId(), 0);
            }
        }

        void resetElytraCourse(Player player) {
            player.sendMessage(ChatColor.RED + "You did not complete the elytra course! Resetting...");
            player.setGliding(false);
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 5.0F, 1.0F);
            FarLands.getDataHandler().getRADH().removeCooldown("elytraCourse", player.getUniqueId().toString());
            elytraCoursePlayers.put(player.getUniqueId(), -1);
            FarLands.getScheduler().scheduleSyncDelayedTask(() -> Utils.tpPlayer(player, ELYTRA_COURSE_RESPAWN), 50L);
        }

        void onElytraCourseCompleted(Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
            FarLands.getDataHandler().getRADH().store(false, "ingame", player.getUniqueId().toString());
            FarLands.getDataHandler().getRADH().removeCooldown("elytraCourse", player.getUniqueId().toString());
            elytraCoursePlayers.remove(player.getUniqueId());
            ItemStack stack = FarLands.getDataHandler().getGameRewardSet("spawnElytraCourse").getReward(player);
            player.sendMessage(ChatColor.GOLD + "Course completed! Receiving " + ChatColor.AQUA + Utils.itemName(stack));
            Utils.giveItem(player, stack, true);
            // Spawn fireworks
            for(int i = 0;i < 15;++ i) {
                double theta = Utils.RNG.nextDouble() * 2 * Math.PI, omega = Utils.RNG.nextDouble() * 2 * Math.PI;
                double dx = Math.cos(theta) * Utils.randomDouble(0, 12), dy = Math.sin(theta) * Utils.randomDouble(0, 12),
                        dz = Math.cos(omega) * Utils.randomDouble(0, 12);
                int[] rgb = new int[] {Utils.randomInt(0, 48), Utils.randomInt(0, 175), Utils.randomInt(128, 256)};
                (new FireworkBuilder(1, 1)).addExplosion(FireworkExplosionType.randomType(Utils.RNG), rgb)
                        .setFadeColors(rgb).spawnEntity(ELYTRA_COURSE_FIREWORK_SPAWN_CENTER.clone().add(dx, dy, dz));
            }
        }
    }
}
