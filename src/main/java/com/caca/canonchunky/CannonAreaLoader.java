package com.caca.canonchunky;

import com.caca.canonchunky.mixin.SchematicPrinterAccessor;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Per-schematicannon state. Holds the chunks a printing cannon needs with a short-lived, memory-only region ticket
 * (level 33 = accessible but not ticking). Tickets are refreshed while the cannon is working; once it stops
 * refreshing, vanilla expires them and unloads the chunks on its own.
 */
public final class CannonAreaLoader {
    // Must stay a single instance: Ticket#equals compares the type by reference, which is what makes re-adding a refresh.
    public static final TicketType<BlockPos> TICKET = TicketType.create("canonchunky_area", Vec3i::compareTo, 200);
    private static final int REFRESH_INTERVAL = 20;
    private static final long NEVER = Long.MIN_VALUE / 2;

    private long lastRefresh = NEVER;
    private long lastReadyCheck = NEVER;
    private boolean ready;
    private boolean degraded;
    private boolean checklistDone;

    private boolean hasRange;
    private int minX, minZ, maxX, maxZ;
    private final int[] wanted = new int[4];

    /** Called at the top of every tickPrinter() call (up to 1000 per game tick), so the fast path must be trivial. */
    public void onTick(ServerLevel level, SchematicannonBlockEntity be) {
        SchematicPrinter printer = be.printer;
        if (!printer.isLoaded()) {
            checklistDone = false;
            return;
        }
        long now = level.getGameTime();
        if (now >= lastRefresh && now - lastRefresh < REFRESH_INTERVAL)
            return;
        if (!shouldHold(be))
            return;
        BlockPos target = printer.getCurrentTarget();
        if (target != null)
            ensure(level, be, target, now);
    }

    /** Called where Create asks whether the current target is loaded. Returns true once everything needed is FULL. */
    public boolean isReady(ServerLevel level, SchematicannonBlockEntity be, BlockPos target) {
        long now = level.getGameTime();
        ensure(level, be, target, now);

        if (ready && now - lastReadyCheck < REFRESH_INTERVAL)
            return true;
        if (!ready && lastReadyCheck == now)
            return false;

        lastReadyCheck = now;
        ready = allFull(level);

        if (ready && !degraded && !checklistDone && Config.refreshChecklist) {
            checklistDone = true;
            be.updateChecklist();
        }
        return ready;
    }

    // Hold while the cannon is running, waiting for a chunk, or waiting for material: in the last case Create keeps
    // polling the target block every tick, so letting the chunk expire would only make it flip between states.
    private static boolean shouldHold(SchematicannonBlockEntity be) {
        return !be.printer.isErrored()
            && (be.state == SchematicannonBlockEntity.State.RUNNING || be.positionNotLoaded || be.missingItem != null);
    }

    private void ensure(ServerLevel level, SchematicannonBlockEntity be, BlockPos target, long now) {
        computeWanted(be, target);
        boolean changed = !hasRange || wanted[0] != minX || wanted[1] != minZ || wanted[2] != maxX || wanted[3] != maxZ;
        if (!changed && now >= lastRefresh && now - lastRefresh < REFRESH_INTERVAL)
            return;

        minX = wanted[0];
        minZ = wanted[1];
        maxX = wanted[2];
        maxZ = wanted[3];
        hasRange = true;
        lastRefresh = now;
        if (changed) {
            ready = false;
            lastReadyCheck = NEVER;
        }

        ServerChunkCache cache = level.getChunkSource();
        BlockPos owner = be.getBlockPos().immutable();
        for (int cx = minX; cx <= maxX; cx++)
            for (int cz = minZ; cz <= maxZ; cz++)
                cache.addRegionTicket(TICKET, new ChunkPos(cx, cz), 0, owner);
    }

    private void computeWanted(SchematicannonBlockEntity be, BlockPos target) {
        SchematicLevel reader = ((SchematicPrinterAccessor) be.printer).canonchunky$getBlockReader();
        BlockPos anchor = be.printer.getAnchor();
        int margin = Config.chunkMargin;

        if (reader != null && anchor != null) {
            BoundingBox b = reader.getBounds();
            int x0 = ((anchor.getX() + b.minX()) >> 4) - margin;
            int x1 = ((anchor.getX() + b.maxX()) >> 4) + margin;
            int z0 = ((anchor.getZ() + b.minZ()) >> 4) - margin;
            int z1 = ((anchor.getZ() + b.maxZ()) >> 4) + margin;
            long count = (long) (x1 - x0 + 1) * (z1 - z0 + 1);
            if (count <= Config.maxAreaChunks) {
                degraded = false;
                wanted[0] = x0;
                wanted[1] = z0;
                wanted[2] = x1;
                wanted[3] = z1;
                return;
            }
        }

        degraded = true;
        int tx = target.getX() >> 4;
        int tz = target.getZ() >> 4;
        wanted[0] = tx - 1;
        wanted[1] = tz - 1;
        wanted[2] = tx + 1;
        wanted[3] = tz + 1;
    }

    // getChunkNow only returns a chunk that has reached FULL, unlike Level#isLoaded which merely checks the ticket level.
    private boolean allFull(ServerLevel level) {
        ServerChunkCache cache = level.getChunkSource();
        for (int cx = minX; cx <= maxX; cx++)
            for (int cz = minZ; cz <= maxZ; cz++)
                if (cache.getChunkNow(cx, cz) == null)
                    return false;
        return true;
    }
}
