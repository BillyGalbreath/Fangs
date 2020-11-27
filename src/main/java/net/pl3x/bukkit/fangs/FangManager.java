package net.pl3x.bukkit.fangs;

import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityEvokerFangs;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IEntitySelector;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MovingObjectPositionEntity;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.VoxelShape;
import net.pl3x.bukkit.fangs.configure.Config;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FangManager {
    private final Fangs plugin;
    private final Map<UUID, SummonCooldown> summonCooldowns = new HashMap<>();

    public FangManager(Fangs plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID uuid) {
        return summonCooldowns.containsKey(uuid);
    }

    public void startCooldown(UUID uuid) {
        SummonCooldown summonCooldownTask = new SummonCooldown(this, uuid);
        summonCooldowns.put(uuid, summonCooldownTask);
        summonCooldownTask.runTaskLater(plugin, Config.SUMMON_COOLDOWN);
    }

    public void stopCooldown(UUID uuid) {
        SummonCooldown summonCooldownTask = summonCooldowns.remove(uuid);
        if (summonCooldownTask != null) {
            summonCooldownTask.cancel();
        }
    }

    public void cancelAllCooldowns() {
        summonCooldowns.values().forEach(BukkitRunnable::cancel);
        summonCooldowns.clear();
    }

    public void attackTarget(Player player, LivingEntity target, String reason) {
        Location playerLoc = player.getLocation();
        double originX = playerLoc.getX();
        double originY = playerLoc.getY();
        double originZ = playerLoc.getZ();

        EntityLiving entityliving = ((CraftLivingEntity) target).getHandle();

        double minY = Math.min(entityliving.locY(), originY);
        double maxY = Math.max(entityliving.locY(), originY) + 1.0D;
        float yaw = (float) MathHelper.d(entityliving.locZ() - originZ, entityliving.locX() - originX);
        int warmUp;
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer.h(entityliving) < 9.0D) {
            float delta;
            for (warmUp = 0; warmUp < 5; ++warmUp) {
                delta = yaw + (float) warmUp * (float) Math.PI * 0.4F;
                spawnFangs(entityPlayer, originX + (double) MathHelper.cos(delta) * 1.5D, originZ + (double) MathHelper.sin(delta) * 1.5D, minY, maxY, delta, 0, reason);
            }
            for (warmUp = 0; warmUp < 8; ++warmUp) {
                delta = yaw + (float) warmUp * (float) Math.PI * 2.0F / 8.0F + 1.2566371F;
                spawnFangs(entityPlayer, originX + (double) MathHelper.cos(delta) * 2.5D, originZ + (double) MathHelper.sin(delta) * 2.5D, minY, maxY, delta, 3, reason);
            }
        } else {
            for (warmUp = 0; warmUp < 16; ++warmUp) {
                double offset = 1.25D * (double) (warmUp + 1);
                spawnFangs(entityPlayer, originX + (double) MathHelper.cos(yaw) * offset, originZ + (double) MathHelper.sin(yaw) * offset, minY, maxY, yaw, warmUp, reason);
            }
        }

    }

    public void attackLocation(Player player, Location location, String reason) {
        double originX = location.getX();
        double originY = location.getY();
        double originZ = location.getZ();

        double minY = originY - 8;
        double maxY = originY + 8;
        float yaw = location.getYaw();
        int warmUp;
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        float delta;
        spawnFangs(entityPlayer, originX, originZ, minY, maxY, location.getYaw(), 0, reason);
        for (warmUp = 0; warmUp < 5; ++warmUp) {
            delta = yaw + (float) warmUp * (float) Math.PI * 0.4F;
            spawnFangs(entityPlayer, originX + (double) MathHelper.cos(delta) * 1.5D, originZ + (double) MathHelper.sin(delta) * 1.5D, minY, maxY, delta, 0, reason);
        }
        for (warmUp = 0; warmUp < 8; ++warmUp) {
            delta = yaw + (float) warmUp * (float) Math.PI * 2.0F / 8.0F + 1.2566371F;
            spawnFangs(entityPlayer, originX + (double) MathHelper.cos(delta) * 2.5D, originZ + (double) MathHelper.sin(delta) * 2.5D, minY, maxY, delta, 3, reason);
        }
    }

    public void spawnFangs(EntityLiving owner, double x, double z, double minY, double maxY, float yaw, int warmUp, String reason) {
        BlockPosition blockposition = new BlockPosition(x, maxY, z);
        boolean flag = false;
        double d4 = 0.0D;
        do {
            BlockPosition blockposition1 = blockposition.down();
            IBlockData iblockdata = owner.world.getType(blockposition1);
            if (iblockdata.d(owner.world, blockposition1, EnumDirection.UP)) {
                if (!owner.world.isEmpty(blockposition)) {
                    IBlockData iblockdata1 = owner.world.getType(blockposition);
                    VoxelShape voxelshape = iblockdata1.getCollisionShape(owner.world, blockposition);
                    if (!voxelshape.isEmpty()) {
                        d4 = voxelshape.c(EnumDirection.EnumAxis.Y);
                    }
                }
                flag = true;
                break;
            }
            blockposition = blockposition.down();
        } while (blockposition.getY() >= MathHelper.floor(minY) - 1);

        if (flag) {
            EntityEvokerFangs fangs = new EntityEvokerFangs(owner.world, x, (double) blockposition.getY() + d4, z, yaw, warmUp, owner);
            owner.world.addEntity(fangs);
            fangs.getBukkitEntity().setMetadata(reason, new FixedMetadataValue(plugin, true));
        }
    }

    public LivingEntity getTargetEntity(Player player, int maxDistance) {
        EntityPlayer owner = ((CraftPlayer) player).getHandle();
        Vec3D start = owner.getEyePosition(1.0F);
        Vec3D direction = owner.getLookDirection();
        Vec3D end = start.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);
        List<Entity> entityList = owner.world.getEntities(owner, owner.getBoundingBox().expand(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance).grow(1.0D, 1.0D, 1.0D), IEntitySelector.notSpectator().and(Entity::isInteractable).and(e -> e instanceof EntityLiving));
        double distance = 0.0D;
        MovingObjectPositionEntity result = null;
        for (Entity entity : entityList) {
            AxisAlignedBB aabb = entity.getBoundingBox().grow(entity.getCollisionBorderSize());
            Optional<Vec3D> rayTraceResult = aabb.calculateIntercept(start, end);
            if (rayTraceResult.isPresent()) {
                Vec3D rayTrace = rayTraceResult.get();
                double distanceTo = start.distanceSquared(rayTrace);
                if (distanceTo < distance || distance == 0.0D) {
                    result = new MovingObjectPositionEntity(entity, rayTrace);
                    distance = distanceTo;
                }
            }
        }
        return result == null ? null : (LivingEntity) result.getEntity().getBukkitEntity();
    }

    public Location getTargetLocation(Player player, int maxDistance) {
        Entity entity = ((CraftPlayer) player).getHandle();
        Vec3D start = new Vec3D(entity.locX(), entity.locY() + entity.getHeadHeight(), entity.locZ());
        org.bukkit.util.Vector dir = player.getLocation().getDirection().multiply(maxDistance);
        Vec3D end = new Vec3D(start.x + dir.getX(), start.y + dir.getY(), start.z + dir.getZ());
        return new Location(player.getWorld(), end.x, end.y, end.z);
    }

    public static class SummonCooldown extends BukkitRunnable {
        private final FangManager fangManager;
        private final UUID uuid;

        public SummonCooldown(FangManager fangManager, UUID uuid) {
            this.fangManager = fangManager;
            this.uuid = uuid;
        }

        @Override
        public void run() {
            fangManager.stopCooldown(uuid);
        }
    }
}
