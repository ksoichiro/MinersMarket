package com.minersmarket.event;

import com.minersmarket.registry.ModItems;
import net.minecraft.core.registries.Registries;
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
        grantEquipment(player);
        applyNightVision(player);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!hasPickaxe(player)) {
            grantEquipment(player);
        }
        applyNightVision(player);
    }

    private static boolean hasPickaxe(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.MINERS_PICKAXE.get())) {
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
                player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.FORTUNE),
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
