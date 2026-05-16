package dev.marblegate.skywarddive.client.renderer;

import dev.marblegate.skywarddive.client.waypoint.SkywardDiveWaypoint;
import dev.marblegate.skywarddive.client.waypoint.WaypointRegistry;
import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.config.ClientConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = SkywardDive.MODID, value = Dist.CLIENT)
public class WaypointHudRenderer {
    private static float EXPAND_PROGRESS = 0f;

    private static final float BAR_ARC      = 120f;
    private static final float BAR_HEIGHT   = 22f;
    private static final float BAR_MARGIN_X = 60f;
    private static final float BAR_TOP_Y    = 6f;
    private static final float BAR_CORNER   = 3f;
    private static final int   BAR_BG       = 0x88000000;
    private static final int   BAR_BORDER   = 0x44FFFFFF;
    private static final float TICK_H       = 5f;
    private static final float PIN_ICON_Y   = 4f;
    private static final float ICON_SIZE    = 8f;
    private static final float MARGIN       = 10f;
    private static final float ARROW_SIZE   = 6f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { EXPAND_PROGRESS = 0f; return; }

        var phase = mc.player.getExistingDataOrNull(SDAttachmentTypes.PHASE);
        boolean gliding = phase == LaunchSession.Phase.GLIDING;

        float target = gliding ? 1f : 0f;
        EXPAND_PROGRESS = Mth.lerp(0.18f, EXPAND_PROGRESS, target);
        if (EXPAND_PROGRESS < 0.002f) EXPAND_PROGRESS = 0f;
        if (EXPAND_PROGRESS > 0.998f) EXPAND_PROGRESS = 1f;
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (EXPAND_PROGRESS <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        float alpha = easeOut(EXPAND_PROGRESS);

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = mc.font;
        int scrW = mc.getWindow().getGuiScaledWidth();
        int scrH = mc.getWindow().getGuiScaledHeight();

        ResourceKey<Level> currentDim = mc.level.dimension();
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        List<SkywardDiveWaypoint> visible = WaypointRegistry.collectAll(mc.player).stream()
                .filter(wp -> wp.dimension().equals(currentDim))
                .toList();

        float slideY = (1f - alpha) * -(BAR_HEIGHT + BAR_TOP_Y + 4f);
        float barX0  = BAR_MARGIN_X;
        float barX1  = scrW - BAR_MARGIN_X;
        float barW   = barX1 - barX0;
        float barY0  = BAR_TOP_Y + slideY;

        fillRoundRect(gui, barX0, barY0, barX1, barY0 + BAR_HEIGHT, BAR_CORNER, withAlpha(BAR_BG, alpha));
        drawRoundRectBorder(gui, barX0, barY0, barX1, barY0 + BAR_HEIGHT, BAR_CORNER, withAlpha(BAR_BORDER, alpha));

        float yaw = mc.player.getYRot();
        for (SkywardDiveWaypoint wp : visible) {
            renderCompassPin(gui, font, wp, camPos, yaw, barX0, barY0, barW, alpha);
        }

        float guiScale = (float) mc.getWindow().getGuiScale();
        int halfW = mc.getWindow().getWidth() / 2;
        int halfH = mc.getWindow().getHeight() / 2;

        int maxDist = ClientConfig.WAYPOINT_RENDER_DISTANCE.getAsInt();
        List<SkywardDiveWaypoint> inRange = visible.stream()
                .filter(wp -> wp.worldPos().distanceTo(camPos) <= maxDist)
                .toList();

        for (SkywardDiveWaypoint wp : inRange) {
            renderProjected(gui, font, wp, camPos, scrW, scrH,
                    guiScale, halfW, halfH, alpha);
        }
    }

    private static void renderCompassPin(
            GuiGraphicsExtractor gui, Font font,
            SkywardDiveWaypoint wp, Vec3 camPos,
            float playerYaw, float barX0, float barY0, float barW, float alpha) {

        Vec3 delta = wp.worldPos().subtract(camPos);
        double dist = delta.length();
        if (dist < 0.001) return;

        float wpYaw  = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float diff   = wrapDeg(wpYaw - playerYaw);
        float halfArc = BAR_ARC * 0.5f;
        int   wpColor = withAlpha(wp.color(), alpha);
        String label  = wp.icon() + " " + formatDist(dist);

        if (diff >= -halfArc && diff <= halfArc) {
            float pinX = barX0 + (diff / BAR_ARC + 0.5f) * barW;
            // centre tick
            gui.fill((int) pinX, (int) barY0, (int) pinX + 1,
                    (int) (barY0 + TICK_H), withAlpha(0xFFFFFFFF, alpha * 0.5f));
            int lw = font.width(label);
            gui.text(font, label, (int) (pinX - lw * 0.5f), (int) (barY0 + PIN_ICON_Y), wpColor, false);
        } else {
            boolean isRight = diff > 0;
            float edgeX = isRight ? barX0 + barW - 4f : barX0 + 4f;
            String caret = isRight ? "›" : "‹";
            gui.text(font, caret, (int) (edgeX - font.width(caret) * 0.5f),
                    (int) (barY0 + PIN_ICON_Y), withAlpha(wp.color(), alpha * 0.85f), false);
        }
    }

    private static void renderProjected(
            GuiGraphicsExtractor gui, Font font,
            SkywardDiveWaypoint wp, Vec3 camPos,
            int scrW, int scrH,
            float guiScale, int halfW, int halfH,
            float alpha) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;

        Vec3 delta = wp.worldPos().subtract(camPos);
        double dist = delta.length();
        if (dist < 0.001) return;

        Vec3 projected = mc.gameRenderer.projectPointToScreen(wp.worldPos());
        float ndcX = (float) projected.x;
        float ndcY = (float) projected.y;
        boolean behind = projected.z > 1.0;

        float sx = (float) (projected.x * halfW + halfW) / guiScale;
        float sy = (float) (halfH - projected.y * halfH) / guiScale;

        int iconColor = withAlpha(wp.color(), alpha);
        int distColor = withAlpha(0xFFCCCCCC, alpha);
        String iconChar  = wp.icon();
        String distLabel = formatDist(dist);

        boolean onScreen = !behind && ndcX >= -1f && ndcX <= 1f && ndcY >= -1f && ndcY <= 1f;

        if (onScreen) {
            drawCenteredText(gui, font, iconChar,  (int) sx, (int) (sy - ICON_SIZE * 0.5f), iconColor);
            drawCenteredText(gui, font, distLabel, (int) sx, (int) (sy + ICON_SIZE * 0.5f + 1f), distColor);
        } else {
            float edgeNdcX = behind ? -ndcX : ndcX;
            float edgeNdcY = behind ? -ndcY : ndcY;
            float[] edge   = clampToEdge(edgeNdcX, edgeNdcY, scrW, scrH, MARGIN);
            float edgeX    = edge[0];
            float edgeY    = edge[1];
            float angle    = (float) Math.atan2(-edgeNdcY, edgeNdcX) + (float) (Math.PI * 0.5);

            gui.pose().pushMatrix();
            gui.pose().translate(edgeX, edgeY);
            gui.pose().rotate(angle);
            drawArrow(gui, ARROW_SIZE, iconColor);
            gui.pose().popMatrix();

            gui.text(font, iconChar + " " + distLabel,
                    (int) (edgeX + ARROW_SIZE + 3f),
                    (int) (edgeY - font.lineHeight * 0.5f),
                    distColor, false);
        }
    }

    private static float[] clampToEdge(float ndcX, float ndcY, int scrW, int scrH, float margin) {
        float cx = scrW * 0.5f, cy = scrH * 0.5f;
        float halfW = cx - margin, halfH = cy - margin;
        float len = (float) Math.sqrt(ndcX * ndcX + ndcY * ndcY);
        if (len < 1e-5f) return new float[]{cx, cy};
        float nx = ndcX / len, ny = ndcY / len;
        float tX = nx != 0 ? halfW / Math.abs(nx) : Float.MAX_VALUE;
        float tY = ny != 0 ? halfH / Math.abs(ny) : Float.MAX_VALUE;
        float t  = Math.min(tX, tY);
        return new float[]{
            Mth.clamp(cx + nx * t, margin, scrW - margin),
            Mth.clamp(cy - ny * t, margin, scrH - margin)
        };
    }

    private static void drawArrow(GuiGraphicsExtractor gui, float size, int color) {
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF,
            g = (color >>  8) & 0xFF, b = color & 0xFF;
        int c = net.minecraft.util.ARGB.color(a, r, g, b);
        int tipY = (int) -size, baseY = (int) (size * 0.5f), halfBase = (int) (size * 0.65f);
        for (int y = tipY; y <= baseY; y++) {
            float frac = (float) (y - tipY) / Math.max(1, baseY - tipY);
            int hw = Math.max(1, (int) (frac * halfBase));
            gui.fill(-hw, y, hw + 1, y + 1, c);
        }
    }

    private static void fillRoundRect(GuiGraphicsExtractor gui,
            float x0, float y0, float x1, float y1, float r, int color) {
        gui.fill((int)(x0+r),(int)y0,(int)(x1-r),(int)y1,color);
        gui.fill((int)x0,(int)(y0+r),(int)(x0+r),(int)(y1-r),color);
        gui.fill((int)(x1-r),(int)(y0+r),(int)x1,(int)(y1-r),color);
        int ir = (int) r;
        fillCornerCircle(gui,(int)x0,    (int)y0,    ir, 1, 1,color);
        fillCornerCircle(gui,(int)(x1-r),(int)y0,    ir,-1, 1,color);
        fillCornerCircle(gui,(int)x0,    (int)(y1-r),ir, 1,-1,color);
        fillCornerCircle(gui,(int)(x1-r),(int)(y1-r),ir,-1,-1,color);
    }

    private static void fillCornerCircle(GuiGraphicsExtractor gui,
            int ox, int oy, int r, int sx, int sy, int color) {
        for (int y = 0; y < r; y++)
            for (int x = 0; x < r; x++)
                if ((x+0.5f)*(x+0.5f)+(y+0.5f)*(y+0.5f) <= (float)r*r) {
                    int px = sx>0 ? ox+x : ox+r-1-x;
                    int py = sy>0 ? oy+y : oy+r-1-y;
                    gui.fill(px,py,px+1,py+1,color);
                }
    }

    private static void drawRoundRectBorder(GuiGraphicsExtractor gui,
            float x0, float y0, float x1, float y1, float r, int color) {
        gui.fill((int)(x0+r),(int)y0,    (int)(x1-r),(int)(y0+1),color);
        gui.fill((int)(x0+r),(int)(y1-1),(int)(x1-r),(int)y1,    color);
        gui.fill((int)x0,    (int)(y0+r),(int)(x0+1),(int)(y1-r),color);
        gui.fill((int)(x1-1),(int)(y0+r),(int)x1,    (int)(y1-r),color);
    }

    private static void drawCenteredText(GuiGraphicsExtractor gui, Font font,
            String text, int x, int y, int color) {
        gui.text(font, text, x - font.width(text) / 2, y, color, false);
    }

    private static float wrapDeg(float deg) {
        deg = deg % 360f;
        if (deg >  180f) deg -= 360f;
        if (deg <= -180f) deg += 360f;
        return deg;
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Mth.clamp((int)(alpha * ((argb >> 24) & 0xFF)), 0, 255);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    private static String formatDist(double dist) {
        if (dist >= 1000.0) return String.format("%.1fkm", dist / 1000.0);
        return String.format("%dm", (int) dist);
    }
}
