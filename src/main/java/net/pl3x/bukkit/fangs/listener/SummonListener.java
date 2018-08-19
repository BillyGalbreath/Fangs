package net.pl3x.bukkit.fangs.listener;

import net.pl3x.bukkit.fangs.FangManager;
import net.pl3x.bukkit.fangs.Fangs;
import net.pl3x.bukkit.fangs.configure.Config;
import net.pl3x.bukkit.fangs.configure.Lang;
import net.pl3x.bukkit.fangs.event.PVPEvent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummonListener implements Listener {
    private final Fangs plugin;
    private final Map<String, String> combat = new HashMap<>();

    public SummonListener(Fangs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClickAir(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // only listen to main hand packet
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return; // not right clicking
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("fangs.summon")) {
            return; // no permission
        }

        if (Config.SUMMON_REQUIRE_SNEAK_SHIFT && !player.isSneaking()) {
            return; // sneaking is required to start summons
        }

        // check item in hand
        if (player.getInventory().getItemInMainHand().getType() != Config.SUMMON_TOOL) {
            return; // not correct summon tool
        }

        FangManager fangManager = plugin.getFangManager();
        if (fangManager.isOnCooldown(player.getUniqueId())) {
            return; // summon on cooldown
        }

        fangManager.startCooldown(player.getUniqueId());

        // get target livingEntity
        LivingEntity target = fangManager.getTargetEntity(player, 16);
        if (target != null) {
            fangManager.attackTarget(player, target, "FangsSummon");
            return;
        }

        Block block = player.getTargetBlock(16);
        Location location;
        if (block == null) {
            location = fangManager.getTargetLocation(player, 16);
        } else {
            location = block.getLocation();
        }
        fangManager.attackLocation(player, location, "FangsSummon");
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

        List<MetadataValue> metaData = fangs.getMetadata("FangsSummon");
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
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', Lang.DEATH_BY_SUMMONS
                .replace("{player}", player.getName())
                .replace("{attacker}", attacker)));
    }
}
