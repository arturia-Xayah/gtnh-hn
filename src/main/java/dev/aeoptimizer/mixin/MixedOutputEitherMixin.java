package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.structureterminal.output.OutputRoles;
import gregtech.api.interfaces.IHatchElement;

@Mixin(targets = "gregtech.api.interfaces.HatchElementEither", remap = false)
public abstract class MixedOutputEitherMixin implements OutputRoles {

    @Shadow
    @Final
    private IHatchElement<?> first;
    @Shadow
    @Final
    private IHatchElement<?> second;

    public int modernization$outputRoles() {
        return OutputRoles.of(first) | OutputRoles.of(second);
    }
}
