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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public class GameHudOverlay {
    private static final ResourceLocation COIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "textures/gui/coin.png");
    private static final int COIN_SIZE = 8;
    private static final int MARGIN = 5;

    private static final long FLOAT_DURATION_MS = 1500;
    private static final float FLOAT_DISTANCE = 20f;
    private static final List<FloatingText> floatingTexts = new ArrayList<>();

    public static void clearFloatingTexts() {
        floatingTexts.clear();
    }

    public static void addFloatingText(long amount) {
        // Top-right: compact "+X"
        floatingTexts.add(new FloatingText(
                String.format("+%,d", amount), FloatingText.Position.TOP_RIGHT));
        // Center-bottom (action bar area): localized "+X gold"
        String localizedText = Component.translatable("message.minersmarket.sold", amount).getString();
        floatingTexts.add(new FloatingText(
                localizedText, FloatingText.Position.CENTER_BOTTOM));
    }

    public static void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Offset below active effect icons (each effect row is ~25px)
        int effectOffset = 0;
        if (mc.player != null) {
            Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
            int visibleCount = (int) effects.stream().filter(MobEffectInstance::showIcon).count();
            if (visibleCount > 0) {
                effectOffset = 25 * visibleCount + 2;
            }
        }

        int y = MARGIN + effectOffset;

        // Render floating sale notifications
        renderFloatingTexts(graphics, mc, screenWidth, screenHeight, y);

        GameState state = ClientGameState.getState();
        if (state == GameState.NOT_STARTED) {
            return;
        }

        // Sales amount display
        long sales = ClientGameState.getSalesAmount();
        String salesText = String.format("%,d / %,d", sales, GameStateManager.TARGET_SALES);
        int textWidth = mc.font.width(salesText);
        int x = screenWidth - textWidth - MARGIN - COIN_SIZE - 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, COIN_TEXTURE, x, y, 0, 0, COIN_SIZE, COIN_SIZE, COIN_SIZE, COIN_SIZE);
        graphics.drawString(mc.font, salesText, x + COIN_SIZE + 2, y, 0xFFFFD700, true);

        // Play time display
        int ticks = ClientGameState.getPlayTime();
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        int timeWidth = mc.font.width(timeText);
        graphics.drawString(mc.font, timeText, screenWidth - timeWidth - MARGIN, y + 12, 0xFFFFFFFF, true);

        // Ranking display (finished players)
        var finishedPlayers = ClientGameState.getFinishedPlayers();
        int nextY = y + 26;
        if (!finishedPlayers.isEmpty()) {
            for (int i = 0; i < finishedPlayers.size(); i++) {
                var entry = finishedPlayers.get(i);
                int ft = entry.finishTimeTicks() / 20;
                String rankText = String.format("#%d %s  %02d:%02d",
                        i + 1, entry.playerName(), ft / 60, ft % 60);
                int rankWidth = mc.font.width(rankText);
                int color = (i == 0) ? 0xFFFFD700 : 0xFFCCCCCC;
                graphics.drawString(mc.font, rankText, screenWidth - rankWidth - MARGIN, nextY, color, true);
                nextY += 11;
            }
        }

        // Price event display
        if (ClientGameState.isPriceEventActive()) {
            renderPriceEvent(graphics, mc, screenWidth, nextY);
        }

        // Market direction marker
        MarketMarkerRenderer.render(graphics, mc, screenWidth, screenHeight);
    }

    private static void renderPriceEvent(GuiGraphics graphics, Minecraft mc,
                                           int screenWidth, int startY) {
        int remainingTicks = ClientGameState.getPriceEventRemainingTicks();
        int secs = remainingTicks / 20;
        float multiplier = ClientGameState.getPriceMultiplier();
        int percent = Math.round(Math.abs(multiplier - 1.0f) * 100);
        String arrow = multiplier >= 1.0f ? "\u2191" : "\u2193";
        int color = multiplier >= 1.0f ? 0xFF55FF55 : 0xFFFF5555;
        String header = Component.translatable("hud.minersmarket.price_event").getString()
                + " " + arrow + percent + "% " + String.format("%d:%02d", secs / 60, secs % 60);
        int headerWidth = mc.font.width(header);
        graphics.drawString(mc.font, header, screenWidth - headerWidth - MARGIN, startY, color, true);
    }

    private static void renderFloatingTexts(GuiGraphics graphics, Minecraft mc,
                                             int screenWidth, int screenHeight, int topRightBaseY) {
        long now = System.currentTimeMillis();
        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            long elapsed = now - ft.startTime;
            if (elapsed > FLOAT_DURATION_MS) {
                it.remove();
                continue;
            }
            float progress = (float) elapsed / FLOAT_DURATION_MS;
            int offsetY = (int) (progress * FLOAT_DISTANCE);
            int alpha = Math.max(4, (int) ((1.0f - progress) * 255));
            int color = (alpha << 24) | 0xFFD700;
            int ftWidth = mc.font.width(ft.text);

            if (ft.position == FloatingText.Position.TOP_RIGHT) {
                graphics.drawString(mc.font, ft.text,
                        screenWidth - ftWidth - MARGIN, topRightBaseY - offsetY, color, true);
            } else {
                // Center-bottom, above the hotbar (vanilla action bar is ~59px above bottom)
                int baseY = screenHeight - 59;
                graphics.drawString(mc.font, ft.text,
                        (screenWidth - ftWidth) / 2, baseY - offsetY, color, true);
            }
        }
    }

    private static class FloatingText {
        enum Position { TOP_RIGHT, CENTER_BOTTOM }

        final String text;
        final long startTime;
        final Position position;

        FloatingText(String text, Position position) {
            this.text = text;
            this.startTime = System.currentTimeMillis();
            this.position = position;
        }
    }
}
