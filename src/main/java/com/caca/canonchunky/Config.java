package com.caca.canonchunky;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Canonchunky.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
        .comment("Keep the chunks covered by a running Schematicannon's blueprint loaded while it prints.")
        .define("enabled", true);

    private static final ModConfigSpec.IntValue CHUNK_MARGIN = BUILDER
        .comment("Extra chunks held around the blueprint area (covers neighbour updates, beds, belts, water ticks).")
        .defineInRange("chunkMargin", 1, 0, 4);

    private static final ModConfigSpec.IntValue MAX_AREA_CHUNKS = BUILDER
        .comment("If the blueprint area (including margin) needs more chunks than this, only the 3x3 chunks around the",
            "current print target are held instead of the whole area.")
        .defineInRange("maxAreaChunks", 1024, 9, 16384);

    private static final ModConfigSpec.BooleanValue REFRESH_CHECKLIST = BUILDER
        .comment("Recompute the material checklist / progress once the whole area has finished loading.",
            "Create skips unloaded blocks when it first builds the checklist, which makes it inaccurate.")
        .define("refreshChecklistOnReady", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static volatile boolean enabled = true;
    public static volatile int chunkMargin = 1;
    public static volatile int maxAreaChunks = 1024;
    public static volatile boolean refreshChecklist = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        bake();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        bake();
    }

    private static void bake() {
        enabled = ENABLED.get();
        chunkMargin = CHUNK_MARGIN.get();
        maxAreaChunks = MAX_AREA_CHUNKS.get();
        refreshChecklist = REFRESH_CHECKLIST.get();
    }
}
