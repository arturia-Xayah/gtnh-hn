package dev.aeoptimizer.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.structureterminal.output.OutputRoles;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

@Mixin(targets = "gregtech.api.interfaces.HatchElement", remap = false)
public abstract class MixedOutputAliasMixin implements OutputRoles {

    @Shadow
    @Final
    private IHatchElement<?> mBacking;
    @Shadow
    @Final
    private IGTHatchAdder<?> mAdder;
    @Shadow
    @Final
    private List<Class<? extends IMetaTileEntity>> mClasses;

    public int modernization$outputRoles() {
        return mAdder == null && mClasses == null ? OutputRoles.of(mBacking) : 0;
    }
}
