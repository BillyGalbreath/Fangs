package net.pl3x.bukkit.fangs.event;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PVPEvent extends EntityDamageByEntityEvent {
    public PVPEvent(Entity damager, Entity damagee) {
        super(damager, damagee, DamageCause.ENTITY_ATTACK,
                ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, 0D),
                ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D)));
    }
}
