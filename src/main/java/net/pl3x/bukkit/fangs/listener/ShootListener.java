package net.pl3x.bukkit.fangs.listener;

import net.pl3x.bukkit.fangs.Fangs;
import net.pl3x.bukkit.fangs.configure.Lang;
import net.pl3x.bukkit.fangs.event.PVPEvent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShootListener implements Listener {
    private final Fangs plugin;
    private final Map<String, String> combat = new HashMap<>();

    public ShootListener(Fangs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFangArrowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return; // not fired by player
        }

        if (isFangsBow(event.getBow())) {
            event.getProjectile().setMetadata("FangsArrow", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow)) {
            return; // not an arrow
        }

        ProjectileSource source = projectile.getShooter();
        if (!(source instanceof Player)) {
            return; // not shot by player
        }

        Arrow arrow = (Arrow) projectile;
        if (!arrow.hasMetadata("FangsArrow")) {
            return; // not a fangs arrow
        }

        Entity targetEntity = event.getHitEntity();
        if (targetEntity != null) {
            plugin.getFangManager().attackLocation((Player) source, targetEntity.getLocation(), "FangsArrow");
            return;
        }

        Block targetBlock = event.getHitBlock();
        if (targetBlock != null) {
            plugin.getFangManager().attackLocation((Player) source, arrow.getLocation(), "FangsArrow");
        }
    }

    @EventHandler
    public void onPlayerHurtByFangs(EntityDamageByEntityEvent event) {
        if (event instanceof PVPEvent) {
            return; // dont listen to fake event
        }

        if (!(event.getDamager() instanceof EvokerFangs)) {
            return; // fangs not causing damage
        }

        if (!(event.getEntity() instanceof Player)) {
            return; // not hurting a player
        }

        Player damaged = (Player) event.getEntity();
        if (damaged.getGameMode() == GameMode.SPECTATOR) {
            return; // ignore spectators
        }

        EvokerFangs fangs = (EvokerFangs) event.getDamager();
        LivingEntity owner = fangs.getOwner();
        if (owner == null) {
            return; // no owner?
        }

        if (!(owner instanceof Player)) {
            return; // fangs not owned by a player
        }

        List<MetadataValue> metaData = fangs.getMetadata("FangsArrow");
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

            PVPEvent pvpEvent = new PVPEvent(owner, damaged);
            plugin.getServer().getPluginManager().callEvent(pvpEvent);
            if (pvpEvent.isCancelled()) {
                event.setCancelled(true);
                return; // pvp cancelled by another plugin
            }

            combat.put(damaged.getName(), owner.getName());
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> combat.remove(damaged.getName()),
                    100); // remove from map in 5 seconds
            return;
        }

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String attacker = combat.get(player.getName());
        if (attacker == null || attacker.isEmpty()) {
            return; // no attacker
        }
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', Lang.DEATH_BY_ARROW
                .replace("{player}", player.getName())
                .replace("{attacker}", attacker)));
    }

    private boolean isFangsBow(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BOW || !stack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        return (meta.hasLore() && meta.getLore().contains("Fangs!")) ||
                (meta.hasCustomModelData() && meta.getCustomModelData() == 999);
    }
}
