package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded, incremental scanning. One tick never processes more than the
 * configured budget. Render cache swaps only when a full pass completes,
 * preventing the old 0.6.0 bug that cleared outlines mid-pass.
 */
public final class OreHighlight {
    private static final VoxelShape OUTLINE = Shapes.box(.002, .002, .002, .998, .998, .998);
    private static List<BlockPos> visible = List.of();
    private static final List<BlockPos> building = new ArrayList<>();
    private static BlockPos origin;
    private static Object levelIdentity;
    private static int cursor;
    private static int scanRadius = -1;
    private static int verticalRadius = -1;

    private OreHighlight() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null || !VisionModClient.isOreEnabled()) {
                reset();
                return;
            }

            int radius = VisionPerformance.oreRadius;
            int vertical = Math.min(radius, 6);
            BlockPos playerPos = client.player.blockPosition();
            int moveThreshold = Math.max(2, radius / 2);
            if (origin == null || levelIdentity != client.level || scanRadius != radius ||
                origin.distSqr(playerPos) > moveThreshold * moveThreshold) {
                origin = playerPos.immutable();
                levelIdentity = client.level;
                scanRadius = radius;
                verticalRadius = vertical;
                cursor = 0;
                building.clear();
                visible = List.of();
            }

            int side = radius * 2 + 1;
            int height = verticalRadius * 2 + 1;
            int total = side * side * height;
            int budget = VisionPerformance.checksPerTick;
            int limit = VisionPerformance.outlineLimit;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int n = 0; n < budget && cursor < total; n++) {
                int index = cursor++;
                // y is the middle axis; x and z have equal horizontal ranges.
                int dx = index % side - radius;
                int dy = (index / side) % height - verticalRadius;
                int dz = index / (side * height) - radius;
                pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                // Keep scanning cheap if we've already collected enough outlines.
                if (building.size() >= limit || !client.level.hasChunkAt(pos)) continue;
                BlockState state = client.level.getBlockState(pos);
                Block block = state.getBlock();
                if (!OreSelection.enabled(block)) continue;
                for (Direction sideDirection : Direction.values()) {
                    if (!client.level.getBlockState(pos.relative(sideDirection)).isSolidRender()) {
                        building.add(pos.immutable());
                        break;
                    }
                }
            }

            if (cursor >= total) {
                visible = List.copyOf(building);
                building.clear();
                cursor = 0;
            }
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!VisionModClient.isOreEnabled() || visible.isEmpty() ||
                context.consumers() == null || context.matrices() == null) return;
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            Vec3 camera = client.gameRenderer.getMainCamera().position();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
            int radius = VisionPerformance.oreRadius + 2;
            double radiusSqr = (double) radius * radius * 3;
            for (BlockPos pos : visible) {
                double dx = pos.getX() - camera.x;
                double dy = pos.getY() - camera.y;
                double dz = pos.getZ() - camera.z;
                if (dx * dx + dy * dy + dz * dz > radiusSqr) continue;
                Block block = client.level.getBlockState(pos).getBlock();
                if (!OreSelection.enabled(block)) continue;
                ShapeRenderer.renderShape(context.matrices(), lines, OUTLINE,
                    dx, dy, dz, OreColors.colorFor(block), 2.0F);
            }
        });
    }

    private static void reset() {
        if (origin == null && visible.isEmpty()) return;
        origin = null;
        levelIdentity = null;
        scanRadius = -1;
        cursor = 0;
        building.clear();
        visible = List.of();
    }
}
