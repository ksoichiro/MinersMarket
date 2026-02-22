package com.minersmarket.hud;

import com.minersmarket.MinersMarket;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.GameState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class MarketMarkerRenderer {
    private static final ResourceLocation MARKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "textures/gui/market_marker.png");
    private static final int ICON_SIZE = 16;
    private static final int MIN_DISTANCE = 30;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80000000;

    public static void render(GuiGraphics graphics, Minecraft mc, int screenWidth, int screenHeight) {
        if (mc.level == null || mc.player == null) return;

        GameState state = ClientGameState.getState();
        if (state == GameState.NOT_STARTED) return;

        BlockPos spawnPos = mc.level.getSharedSpawnPos();
        Vec3 playerPos = mc.player.position();
        Vec3 marketPos = Vec3.atCenterOf(spawnPos);

        double distance = playerPos.distanceTo(marketPos);
        if (distance < MIN_DISTANCE) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        double dx = marketPos.x - cameraPos.x;
        double dy = marketPos.y - cameraPos.y;
        double dz = marketPos.z - cameraPos.z;

        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        double forwardDist = dx * look.x + dy * look.y + dz * look.z;
        double leftDist = dx * left.x + dy * left.y + dz * left.z;
        double upDist = dx * up.x + dy * up.y + dz * up.z;

        // Only show when the market is in front of the camera
        if (forwardDist <= 0.01) return;

        double fov = mc.options.fov().get();
        double halfFovRad = Math.toRadians(fov / 2.0);
        double tanHalfFov = Math.tan(halfFovRad);
        double aspectRatio = (double) screenWidth / screenHeight;

        double screenXNorm = -leftDist / (forwardDist * tanHalfFov * aspectRatio);
        double screenYNorm = upDist / (forwardDist * tanHalfFov);

        int markerX = (int) ((screenXNorm + 1.0) / 2.0 * screenWidth);
        int markerY = (int) ((1.0 - screenYNorm) / 2.0 * screenHeight);

        // Only show when within screen bounds
        if (markerX < -ICON_SIZE || markerX > screenWidth + ICON_SIZE
                || markerY < -ICON_SIZE || markerY > screenHeight + ICON_SIZE) {
            return;
        }

        // Draw icon centered at projected position
        int iconX = markerX - ICON_SIZE / 2;
        int iconY = markerY - ICON_SIZE / 2;
        graphics.blit(RenderType::guiTextured, MARKER_TEXTURE, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Draw label and distance below icon
        String label = Component.translatable("hud.minersmarket.market_marker").getString();
        String fullText = label + String.format(" (%dm)", (int) distance);
        int textWidth = mc.font.width(fullText);
        int textX = markerX - textWidth / 2;
        int textY = iconY + ICON_SIZE + 2;

        // Clamp text X to screen bounds
        textX = Math.max(2, Math.min(screenWidth - textWidth - 2, textX));

        // Background behind text
        graphics.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + mc.font.lineHeight + 1, BG_COLOR);
        graphics.drawString(mc.font, fullText, textX, textY, TEXT_COLOR, true);
    }
}
