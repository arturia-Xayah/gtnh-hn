package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.structureterminal.output.MixedOutputBus;
import gregtech.api.interfaces.ITexture;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;

/** GT allocates its inventory and UI handler together. Request two native slots only for our registered bus. */
@Mixin(value = MTEHatchOutputBusME.class, remap = false)
public abstract class MixedOutputSlotsMixin {

    @ModifyArg(
        method = "<init>(ILjava/lang/String;Ljava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEHatchOutputBus;<init>(ILjava/lang/String;Ljava/lang/String;I[Ljava/lang/String;I)V"),
        index = 5,
        require = 1)
    private static int hn$registeredSlots(int id, String name, String title, int tier, String[] description,
        int slots) {
        return MixedOutputBus.MACHINE_NAME.equals(name) ? 2 : slots;
    }

    @ModifyArg(
        method = "<init>(Ljava/lang/String;I[Ljava/lang/String;[[[Lgregtech/api/interfaces/ITexture;)V",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEHatchOutputBus;<init>(Ljava/lang/String;II[Ljava/lang/String;[[[Lgregtech/api/interfaces/ITexture;)V"),
        index = 2,
        require = 1)
    private static int hn$placedSlots(String name, int tier, int slots, String[] description, ITexture[][][] textures) {
        return MixedOutputBus.MACHINE_NAME.equals(name) ? 2 : slots;
    }
}
