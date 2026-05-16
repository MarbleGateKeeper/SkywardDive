package dev.marblegate.skywarddive.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = SkywardDive.MODID, value = Dist.CLIENT)
public class SpeedLineRenderer {
    private static final int LINE_COUNT = 80;
    private static final float MIN_LENGTH = 1.5f;
    private static final float MAX_LENGTH = 4.5f;

    private static final float[] ANGLES = new float[LINE_COUNT];
    private static final float[] LENGTHS = new float[LINE_COUNT];
    private static final float[] OFFSETS = new float[LINE_COUNT];
    private static final float[] RADII = new float[LINE_COUNT];

    static {
        Random rng = new Random(0xDEADBEEF);
        for (int i = 0; i < LINE_COUNT; i++) {
            ANGLES[i] = rng.nextFloat() * Mth.TWO_PI;
            LENGTHS[i] = MIN_LENGTH + rng.nextFloat() * (MAX_LENGTH - MIN_LENGTH);
            OFFSETS[i] = rng.nextFloat();
            RADII[i] = 2.0f + rng.nextFloat() * 4.0f;
        }
    }

    @SubscribeEvent
    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        var phase = mc.player.getExistingDataOrNull(SDAttachmentTypes.PHASE);
        if (phase != LaunchSession.Phase.LAUNCHING) return;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float animPhase = (mc.level.getGameTime() + pt) / 20.0f % 1.0f;
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        double px = Mth.lerp(pt, mc.player.xOld, mc.player.getX());
        double py = Mth.lerp(pt, mc.player.yOld, mc.player.getY()) + mc.player.getEyeHeight();
        double pz = Mth.lerp(pt, mc.player.zOld, mc.player.getZ());

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-camPos.x, -camPos.y, -camPos.z);

        event.getSubmitNodeCollector().submitCustomGeometry(ps, RenderTypes.linesTranslucent(), (pose, vc) -> {
            Matrix4f mat = pose.pose();
            for (int i = 0; i < LINE_COUNT; i++) {
                float t = (animPhase + OFFSETS[i]) % 1.0f;
                float alpha = Mth.clamp((1.0f - t) * 1.2f, 0f, 0.8f);
                if (alpha < 0.01f) continue;

                float x = (float) (px + Mth.cos(ANGLES[i]) * RADII[i]);
                float cy = (float) (py + Mth.lerp(t, 8.0f, -3.0f));
                float z = (float) (pz + Mth.sin(ANGLES[i]) * RADII[i]);
                float hl = LENGTHS[i] * 0.5f;

                int a = (int) (alpha * 255);
                vc.addVertex(mat, x, cy - hl, z).setColor(255, 255, 255, a).setNormal(pose, 0, 1, 0).setLineWidth(2.0f);
                vc.addVertex(mat, x, cy + hl, z).setColor(255, 255, 255, 0).setNormal(pose, 0, 1, 0).setLineWidth(2.0f);
            }
        });

        ps.popPose();
    }
}
