package dev.aeoptimizer.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.structureterminal.output.MixedOutputBus;
import dev.structureterminal.output.MixedOutputHost;
import dev.structureterminal.output.OutputPlacement;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixedOutputMachineMixin implements MixedOutputHost {

    @Shadow
    public ArrayList<MTEHatchOutputBus> mOutputBusses;
    @Unique
    private final Set<MixedOutputBus> modernization$approved = new HashSet<>();

    public Set<MixedOutputBus> modernization$approvedOutputs() {
        return modernization$approved;
    }

    @Inject(method = "clearHatches", at = @At("HEAD"))
    private void modernization$clear(CallbackInfo ci) {
        modernization$approved.clear();
    }

    @Inject(method = { "addOutputBusToMachineList", "addToMachineList" }, at = @At("HEAD"), cancellable = true)
    private void modernization$rejectSingle(IGregTechTileEntity tile, int texture,
        CallbackInfoReturnable<Boolean> cir) {
        if (tile != null && tile.getMetaTileEntity() instanceof MixedOutputBus && !OutputPlacement.allowed(this, tile))
            cir.setReturnValue(false);
    }

    @Redirect(
        method = { "checkStructure", "checkMachine_TT" },
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;checkMachine(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;Lnet/minecraft/item/ItemStack;Ljava/util/List;)V"))
    private void modernization$validate(MTEMultiBlockBase machine, IGregTechTileEntity base, ItemStack controller,
        List<StructureError> errors) {
        modernization$approved.clear();
        machine.checkMachine(base, controller, errors);
        for (MTEHatchOutputBus bus : mOutputBusses)
            if (bus instanceof MixedOutputBus && !modernization$approved.contains(bus)) {
                errors.add(StructureErrorRegistry.UNKNOWN_STRUCTURE_ERROR);
                break;
            }
    }

    @Inject(method = "getOutputHatches()Ljava/util/List;", at = @At("RETURN"))
    private void modernization$fluids(CallbackInfoReturnable<List<IOutputHatch>> cir) {
        for (MixedOutputBus bus : modernization$approved) {
            if (bus.isValid() && mOutputBusses.contains(bus)) cir.getReturnValue()
                .add(bus.fluidEndpoint);
        }
    }
}
