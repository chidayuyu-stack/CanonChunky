package com.caca.canonchunky;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Canonchunky.MODID)
public class Canonchunky {
    public static final String MODID = "canonchunky";

    public Canonchunky(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
