package com.caca.canonchunky.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.schematics.SchematicPrinter;

import net.createmod.catnip.levelWrappers.SchematicLevel;

@Mixin(SchematicPrinter.class)
public interface SchematicPrinterAccessor {
    @Accessor("blockReader")
    SchematicLevel canonchunky$getBlockReader();
}
