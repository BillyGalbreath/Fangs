package net.pl3x.bukkit.fangs.listener;

import net.pl3x.bukkit.fangs.Fangs;
import net.pl3x.bukkit.fangs.configure.Config;
import net.pl3x.bukkit.fangs.configure.Lang;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlantListener implements Listener {
    private final Fangs plugin;
    private final Set<String> bitten = new HashSet<>();

    public PlantListener(Fangs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!Config.PLANT_ENABLED) {
            return; // plant disabled
        }

        Location to = event.getTo();
        Location from = event.getFrom();

        if (to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
            return; // did not move a full block
        }

        Block leaves = to.getBlock().getRelative(BlockFace.DOWN);
        if (!Tag.LEAVES.isTagged(leaves.getType())) {
            return; // not leaves
        }

        Block base = leaves.getRelative(BlockFace.DOWN);
        if (base.getType() != Config.PLANT_BASE_TYPE) {
            return; // incorrect base
        }

        // position the plant in the center of the block
        Location plantLoc = to.clone();
        plantLoc.setX(plantLoc.getBlockX() + 0.5);
        plantLoc.setY(plantLoc.getBlockY());
        plantLoc.setZ(plantLoc.getBlockZ() + 0.5);

        // lets spawn our fangs and zombie entities
        EvokerFangs fangs = plantLoc.getWorld().spawn(plantLoc, EvokerFangs.class);
        Silverfish silverfish = plantLoc.getWorld().spawn(plantLoc.subtract(0, 2, 0), Silverfish.class);

        // set silverfish invisible and unmovable
        silverfish.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 60, false, false));
        silverfish.setAI(false);
        silverfish.setInvulnerable(true);
        silverfish.setSilent(true);

        // remove silverfish after 3 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, silverfish::remove, 60);

        // set silverfish as fangs owner (so it doesnt deal "magic" damage)
        fangs.setOwner(silverfish);

        // mark this as our fangs plant
        fangs.setMetadata("FangsPlant", new FixedMetadataValue(plugin, true));
    }

    @EventHandler
    public void onPlantDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EvokerFangs)) {
            return; // plant not causing damage
        }

        if (!(event.getEntity() instanceof Player)) {
            return; // not hurting a player
        }

        Player player = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return; // ignore spectators
        }

        EvokerFangs fangs = (EvokerFangs) event.getDamager();
        List<MetadataValue> metaData = fangs.getMetadata("FangsPlant");
        if (metaData.isEmpty()) {
            return; // not our custom fangs
        }
        for (MetadataValue value : metaData) {
            if (!plugin.equals(value.getOwningPlugin())) {
                continue; // not our metadata value
            }
            if (!value.asBoolean()) {
                continue; // not set to true (for some reason?)
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 1));
            player.sendMessage(ChatColor.GREEN + "You've been bitten by a carnivorous plant!");
            bitten.add(player.getName());
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> bitten.remove(player.getName()),
                    2); // remove from map almost immediately
            return;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!bitten.contains(player.getName())) {
            return; // no attacker
        }
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', Lang.DEATH_BY_PLANT
                .replace("{player}", player.getName())));
    }
}
