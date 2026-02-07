package com.minersmarket.hud;

import com.minersmarket.MinersMarket;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class GameHudOverlay {
    private static final ResourceLocation COIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "textures/gui/coin.png");
    private static final int COIN_SIZE = 9;
    private static final int MARGIN = 5;

    public static void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        GameState state = ClientGameState.getState();
        if (state == GameState.NOT_STARTED) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // Sales amount display
        long sales = ClientGameState.getSalesAmount();
        String salesText = String.format("%,d / %,d", sales, GameStateManager.TARGET_SALES);
        int textWidth = mc.font.width(salesText);
        int x = screenWidth - textWidth - MARGIN - COIN_SIZE - 2;
        int y = MARGIN;

        graphics.blit(COIN_TEXTURE, x, y, 0, 0, COIN_SIZE, COIN_SIZE, COIN_SIZE, COIN_SIZE);
        graphics.drawString(mc.font, salesText, x + COIN_SIZE + 2, y, 0xFFD700, true);

        // Play time display
        int ticks = ClientGameState.getPlayTime();
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        int timeWidth = mc.font.width(timeText);
        graphics.drawString(mc.font, timeText, screenWidth - timeWidth - MARGIN, y + 12, 0xFFFFFF, true);
    }
}
