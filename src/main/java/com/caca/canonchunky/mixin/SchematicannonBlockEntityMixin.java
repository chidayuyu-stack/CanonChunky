package com.caca.canonchunky.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.caca.canonchunky.CannonAreaLoader;
import com.caca.canonchunky.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@Mixin(SchematicannonBlockEntity.class)
public abstract class SchematicannonBlockEntityMixin {
    @Unique
    private CannonAreaLoader canonchunky$loader;

    @Unique
    private CannonAreaLoader canonchunky$loader() {
        if (canonchunky$loader == null)
            canonchunky$loader = new CannonAreaLoader();
        return canonchunky$loader;
    }

    // tickPrinter runs at least once per tick even while the cannon is cooling down, so ticket refreshes are never
    // starved by a long schematicannonDelay.
    @Inject(method = "tickPrinter", at = @At("HEAD"))
    private void canonchunky$holdArea(CallbackInfo ci) {
        SchematicannonBlockEntity self = (SchematicannonBlockEntity) (Object) this;
        if (!Config.enabled || !(self.getLevel() instanceof ServerLevel level))
            return;
        canonchunky$loader().onTick(level, self);
    }

    @WrapOperation(
        method = "tickPrinter",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z"),
        require = 1)
    private boolean canonchunky$gate(Level level, BlockPos target, Operation<Boolean> original) {
        boolean loaded = original.call(level, target);
        if (!Config.enabled || target == null || !(level instanceof ServerLevel serverLevel))
            return loaded;
        SchematicannonBlockEntity self = (SchematicannonBlockEntity) (Object) this;
        // Evaluate the loader first so tickets are requested even when the vanilla check already says "not loaded".
        boolean ready = canonchunky$loader().isReady(serverLevel, self, target);
        return loaded && ready;
    }
}
