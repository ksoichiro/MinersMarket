package com.minersmarket.event;

import com.minersmarket.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class PlayerSpawnHandler {
    public static void onPlayerRespawn(ServerPlayer player, boolean conqueredEnd, Entity.RemovalReason removalReason) {
        if (conqueredEnd) return;
        // Teleport to world spawn to bypass Minecraft's safe-spawn search
        // that places players on the roof of the market structure
        if (player.getRespawnConfig() == null) {
            teleportToWorldSpawn(player);
        }
        grantEquipment(player);
        applyNightVision(player);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!hasPickaxe(player)) {
            teleportToWorldSpawn(player);
            grantEquipment(player);
        }
        applyNightVision(player);
    }

    private static void teleportToWorldSpawn(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        BlockPos spawnPos = overworld.getRespawnData().pos();
        player.teleportTo(overworld,
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);
    }

    private static boolean hasPickaxe(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.MINERS_PICKAXE.get())) {
                return true;
            }
        }
        return false;
    }

    public static void resetPlayer(ServerPlayer player) {
        player.getInventory().clearContent();
        grantEquipment(player);
        applyNightVision(player);
    }

    private static void grantEquipment(ServerPlayer player) {
        // Miner's Pickaxe with Fortune III
        ItemStack pickaxe = new ItemStack(ModItems.MINERS_PICKAXE.get());
        pickaxe.enchant(
                player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                3
        );
        player.getInventory().add(pickaxe);

        // 1 stack of Bread
        player.getInventory().add(new ItemStack(Items.BREAD, 64));
    }

    private static void applyNightVision(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                Integer.MAX_VALUE,
                0,
                true,  // ambient (no swirling particles)
                false, // visible (no particles)
                false  // showIcon (hide from HUD)
        ));
    }
}
