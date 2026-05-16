package dev.marblegate.skywarddive.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.config.ClientConfig;
import dev.marblegate.skywarddive.config.Config;
import java.util.*;
import java.util.stream.StreamSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = SkywardDive.MODID, value = Dist.CLIENT)
public class SkyDiveTargetingRenderer {
    private static final double VIEW_DOT = 0.5;
    private static final Map<Integer, Integer> targetLock = new LinkedHashMap<>();
    private static final Map<Integer, Long> lockTime = new HashMap<>();
    private static final int CIRCLE_SEGMENTS = 32;

    private record LockColor(float r, float g, float b) {}

    private static LockColor getEntityColor(Entity entity) {
        if (isBoss(entity)) return new LockColor(0.85f, 0.0f, 1.0f);
        if (isNeutral(entity)) return new LockColor(1.0f, 0.75f, 0.0f);
        if (entity.getType().getCategory() == MobCategory.MONSTER) return new LockColor(1.0f, 0.15f, 0.1f);
        if (entity instanceof Player) return new LockColor(0.0f, 0.85f, 1.0f);
        return new LockColor(0.2f, 0.75f, 1.0f);
    }

    private static boolean isBoss(Entity e) {
        return e instanceof EnderDragon
                || e instanceof WitherBoss
                || e instanceof ElderGuardian
                || e instanceof Ravager
                || e instanceof Warden;
    }

    private static boolean isNeutral(Entity e) {
        MobCategory cat = e.getType().getCategory();
        if (cat != MobCategory.CREATURE && cat != MobCategory.MISC) return false;
        return e instanceof Wolf
                || e instanceof PolarBear
                || e instanceof Bee
                || e instanceof Goat
                || e instanceof ZombifiedPiglin
                || e instanceof EnderMan
                || e instanceof Spider
                || e instanceof CaveSpider;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var phase = mc.player.getExistingDataOrNull(SDAttachmentTypes.PHASE);
        if (phase != LaunchSession.Phase.GLIDING || !ClientConfig.RENDER_TARGETING.get()) {
            targetLock.clear();
            lockTime.clear();
            return;
        }

        var eyePos = mc.player.getEyePosition();
        var look = mc.player.getLookAngle();
        Set<Integer> visible = new HashSet<>();

        StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).toList().forEach(entity -> {
            if (entity == mc.player) return;
            if (!(entity instanceof LivingEntity)) return;
            var ep = entity.getExistingDataOrNull(SDAttachmentTypes.PHASE);
            if (ep != null && ep != LaunchSession.Phase.DONE) return;
            var center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            var dir = center.subtract(eyePos);
            double dist = dir.length();
            if (dist < 0.001 || dist > Config.TARGET_MAX_RANGE.getAsDouble()) return;
            if (look.dot(dir.scale(1.0 / dist)) < VIEW_DOT) return;
            ClipContext clipCtx = new ClipContext(eyePos, center,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
            BlockHitResult hit = mc.level.clip(clipCtx);
            if (hit.getType() != HitResult.Type.MISS
                    && hit.getLocation().distanceTo(eyePos) < dist - 0.5)
                return;
            visible.add(entity.getId());
        });

        targetLock.keySet().retainAll(visible);
        lockTime.keySet().retainAll(targetLock.keySet());

        for (int id : visible) {
            var prev = targetLock.getOrDefault(id, 0);
            var next = Math.min(prev + 1, Config.TARGET_LOCK_TICKS.getAsInt());
            targetLock.put(id, next);
            if (prev < Config.TARGET_LOCK_TICKS.getAsInt() && next == Config.TARGET_LOCK_TICKS.getAsInt()) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f, 1.5f);
                lockTime.put(id, mc.level.getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || targetLock.isEmpty()) return;
        if (!ClientConfig.RENDER_TARGETING.get()) return;
        boolean advanced = ClientConfig.RENDER_ADVANCED_TARGETING.get();

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        long gameTime = mc.level.getGameTime();

        Quaternionf worldToCam = new Quaternionf(mc.gameRenderer.getMainCamera().rotation()).conjugate();
        float halfTanFov = (float) Math.tan(Math.toRadians(mc.options.fov().get()) * 0.5);
        Vector3f vDY = new Vector3f(0, 1, 0);
        worldToCam.transform(vDY);

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-camPos.x, -camPos.y, -camPos.z);

        event.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.lines(), (pose, vc) -> {
            Matrix4f mat = pose.pose();

            for (Map.Entry<Integer, Integer> entry : targetLock.entrySet()) {
                Entity entity = mc.level.getEntity(entry.getKey());
                if (entity == null) continue;

                float progress = entry.getValue() / (float) Config.TARGET_LOCK_TICKS.getAsInt();
                float alpha = 0.3f + 0.7f * progress;
                float scale = Mth.lerp(progress, 3.0f, 1.0f);

                double ex = Mth.lerp(pt, entity.xOld, entity.getX());
                double ey = Mth.lerp(pt, entity.yOld, entity.getY());
                double ez = Mth.lerp(pt, entity.zOld, entity.getZ());
                double centerY = ey + entity.getBbHeight() * 0.5;
                double dist = camPos.distanceTo(new Vec3(ex, centerY, ez));
                double halfExtent = dist * 0.12 * scale;
                double halfW = entity.getBbWidth() * 0.5 * scale;
                double halfH = entity.getBbHeight() * 0.5 * scale;
                double w = Mth.lerp(progress, halfExtent, halfW);
                double h = Mth.lerp(progress, halfExtent, halfH);

                LockColor col = getEntityColor(entity);
                int color = ARGB.colorFromFloat(alpha, col.r(), col.g(), col.b());

                drawBox(vc, mat, pose, ex - w, centerY - h, ez - w, ex + w, centerY + h, ez + w, color);

                if (advanced && progress < 1.0f) {
                    float arm = (1.0f - progress) * 0.7f;
                    double ax = w * arm, ay = h * arm, az = w * arm;
                    double[][] corners = {
                            { ex - w, centerY - h, ez - w, +1, +1, +1 },
                            { ex + w, centerY - h, ez - w, -1, +1, +1 },
                            { ex - w, centerY + h, ez - w, +1, -1, +1 },
                            { ex + w, centerY + h, ez - w, -1, -1, +1 },
                            { ex - w, centerY - h, ez + w, +1, +1, -1 },
                            { ex + w, centerY - h, ez + w, -1, +1, -1 },
                            { ex - w, centerY + h, ez + w, +1, -1, -1 },
                            { ex + w, centerY + h, ez + w, -1, -1, -1 },
                    };
                    for (double[] c : corners) {
                        double x = c[0], y = c[1], z = c[2];
                        double sx = c[3], sy = c[4], sz = c[5];
                        drawLine(vc, mat, pose, x, y, z, x + sx * ax, y, z, color);
                        drawLine(vc, mat, pose, x, y, z, x, y + sy * ay, z, color);
                        drawLine(vc, mat, pose, x, y, z, x, y, z + sz * az, color);
                    }
                }

                if (advanced && progress >= 1.0f) {
                    long elapsed = gameTime - lockTime.getOrDefault(entry.getKey(), gameTime);
                    float beamT = Mth.clamp((elapsed + pt) / 12.0f, 0f, 1f);
                    float pulse = 0.75f + 0.25f * Mth.sin((elapsed + pt) * 0.25f);
                    int pulseColor = ARGB.colorFromFloat(alpha * pulse, col.r(), col.g(), col.b());
                    double bw = 0.07;
                    boolean above = entity.getY() > mc.player.getY();

                    float beamMax = 28.0f;
                    Vector3f vBase = new Vector3f(
                            (float) (ex - camPos.x), (float) (centerY - camPos.y), (float) (ez - camPos.z));
                    worldToCam.transform(vBase);
                    if (vBase.z < 0) {
                        float sign = above ? -1f : 1f;
                        float ndcLimit = above ? -0.88f : 0.88f;
                        float computedMax = clampedBeamHeight(vBase, vDY, halfTanFov, ndcLimit, sign);
                        if (computedMax > 0 && computedMax < beamMax) beamMax = computedMax;
                    }
                    float beamH = Math.min(beamT * beamT * beamT * 28.0f, beamMax);

                    float ringR = (float) Math.max(3.0, dist * 0.07);
                    drawCircle(vc, mat, pose, ex, centerY, ez, ringR, pulseColor);

                    if (above) {
                        drawBox(vc, mat, pose, ex - bw, centerY - beamH, ez - bw, ex + bw, centerY, ez + bw, pulseColor);
                    } else {
                        drawBox(vc, mat, pose, ex - bw, centerY, ez - bw, ex + bw, centerY + beamH, ez + bw, pulseColor);
                    }
                }
            }
        });

        if (advanced) {
            for (Map.Entry<Integer, Integer> entry : targetLock.entrySet()) {
                Entity entity = mc.level.getEntity(entry.getKey());
                if (entity == null) continue;
                float progress = entry.getValue() / (float) Config.TARGET_LOCK_TICKS.getAsInt();
                if (progress < 1.0f) continue;
                long elapsed = gameTime - lockTime.getOrDefault(entry.getKey(), gameTime);
                float beamT = Mth.clamp((elapsed + pt) / 12.0f, 0f, 1f);
                if (beamT < 1.0f) continue;

                double ex = Mth.lerp(pt, entity.xOld, entity.getX());
                double ey = Mth.lerp(pt, entity.yOld, entity.getY());
                double ez = Mth.lerp(pt, entity.zOld, entity.getZ());
                double centerY = ey + entity.getBbHeight() * 0.5;
                double dist = camPos.distanceTo(new Vec3(ex, centerY, ez));
                boolean above = entity.getY() > mc.player.getY();

                float beamMax = 28.0f;
                Vector3f vBase = new Vector3f(
                        (float) (ex - camPos.x), (float) (centerY - camPos.y), (float) (ez - camPos.z));
                worldToCam.transform(vBase);
                if (vBase.z < 0) {
                    float sign = above ? -1f : 1f;
                    float ndcLimit = above ? -0.88f : 0.88f;
                    float computedMax = clampedBeamHeight(vBase, vDY, halfTanFov, ndcLimit, sign);
                    if (computedMax > 0 && computedMax < beamMax) beamMax = computedMax;
                }
                float beamH = Math.min(28.0f, beamMax);

                double tipY = above ? centerY - beamH : centerY + beamH;
                double textOff = dist * 0.07;
                double textY = above ? tipY - textOff : tipY + textOff;
                float nameScale = (float) (dist * halfTanFov * 0.008f);

                FormattedCharSequence nameSeq = entity.getName().getVisualOrderText();
                ps.pushPose();
                ps.translate(ex, textY, ez);
                ps.mulPose(mc.gameRenderer.getMainCamera().rotation());
                ps.scale(nameScale, -nameScale, nameScale);
                event.getSubmitNodeCollector().submitText(ps,
                        -mc.font.width(nameSeq) * 0.5f, 0,
                        nameSeq, false,
                        Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT,
                        DyeColor.WHITE.getTextColor(), 0, 0);
                ps.popPose();
            }
        }

        ps.popPose();
    }

    private static void drawLine(VertexConsumer vc, Matrix4f mat, PoseStack.Pose pose,
            double x1, double y1, double z1,
            double x2, double y2, double z2, int color) {
        int r = ARGB.red(color), g = ARGB.green(color), b = ARGB.blue(color), a = ARGB.alpha(color);
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(mat, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(2f);
        vc.addVertex(mat, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(2f);
    }

    private static void drawBox(VertexConsumer vc, Matrix4f mat, PoseStack.Pose pose,
            double x0, double y0, double z0,
            double x1, double y1, double z1, int color) {
        drawLine(vc, mat, pose, x0, y0, z0, x1, y0, z0, color);
        drawLine(vc, mat, pose, x1, y0, z0, x1, y0, z1, color);
        drawLine(vc, mat, pose, x1, y0, z1, x0, y0, z1, color);
        drawLine(vc, mat, pose, x0, y0, z1, x0, y0, z0, color);
        drawLine(vc, mat, pose, x0, y1, z0, x1, y1, z0, color);
        drawLine(vc, mat, pose, x1, y1, z0, x1, y1, z1, color);
        drawLine(vc, mat, pose, x1, y1, z1, x0, y1, z1, color);
        drawLine(vc, mat, pose, x0, y1, z1, x0, y1, z0, color);
        drawLine(vc, mat, pose, x0, y0, z0, x0, y1, z0, color);
        drawLine(vc, mat, pose, x1, y0, z0, x1, y1, z0, color);
        drawLine(vc, mat, pose, x1, y0, z1, x1, y1, z1, color);
        drawLine(vc, mat, pose, x0, y0, z1, x0, y1, z1, color);
    }

    private static void drawCircle(VertexConsumer vc, Matrix4f mat, PoseStack.Pose pose,
            double cx, double cy, double cz, float radius, int color) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / CIRCLE_SEGMENTS;
            float a1 = (float) ((i + 1) * Mth.TWO_PI / CIRCLE_SEGMENTS);
            drawLine(vc, mat, pose,
                    cx + Mth.cos(a0) * radius, cy, cz + Mth.sin(a0) * radius,
                    cx + Mth.cos(a1) * radius, cy, cz + Mth.sin(a1) * radius,
                    color);
        }
    }

    private static float clampedBeamHeight(Vector3f vBase, Vector3f vDY, float halfTanFov,
            float ndcLimit, float sign) {
        float denom = sign * (vDY.y + ndcLimit * halfTanFov * vDY.z);
        if (Math.abs(denom) < 1e-6f) return Float.MAX_VALUE;
        float h = (-ndcLimit * halfTanFov * vBase.z - vBase.y) / denom;
        return h > 0 ? h : Float.MAX_VALUE;
    }
}
