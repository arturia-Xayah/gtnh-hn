package dev.aeoptimizer.mixin.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import dev.aeoptimizer.mixin.NEIPatternPacketAccessor;
import dev.aeoptimizer.pattern.AssemblyLineRenamer;

/** Carries recipe provenance only; the server decides whether the terminal's network opted in. */
@Mixin(value = NEEPatternTerminalHandler.class, remap = false)
public abstract class AssemblyLineImportMixin {

    @Shadow
    private PacketNEIPatternRecipe packProcessRecipe(IRecipeHandler recipe, int index, int multiplier) {
        throw new AssertionError();
    }

    @Unique
    private boolean hn$separateSlots;

    @Inject(method = "getMergedInputs", at = @At("HEAD"), cancellable = true)
    private void hn$unmerged(List<PositionedStack> inputs, IRecipeProcessor processor, IRecipeHandler recipe, int index,
        String identifier, CallbackInfoReturnable<List<PositionedStack>> cir) {
        if (hn$separateSlots) {
            List<PositionedStack> copies = new ArrayList<>(inputs.size());
            for (PositionedStack input : inputs) copies.add(input.copy());
            cir.setReturnValue(copies);
        }
    }

    @Redirect(
        method = "packRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/vfyjxf/nee/nei/NEEPatternTerminalHandler;packProcessRecipe(Lcodechicken/nei/recipe/IRecipeHandler;II)Lcom/github/vfyjxf/nee/network/packet/PacketNEIPatternRecipe;"))
    private PacketNEIPatternRecipe hn$pack(NEEPatternTerminalHandler self, IRecipeHandler recipe, int index,
        int multiplier) {
        if (!AssemblyLineRenamer.RECIPE_ID.equals(recipe.getOverlayIdentifier()))
            return packProcessRecipe(recipe, index, multiplier);
        PacketNEIPatternRecipe separate;
        hn$separateSlots = true;
        try {
            separate = packProcessRecipe(recipe, index, multiplier);
        } finally {
            hn$separateSlots = false;
        }
        PacketNEIPatternRecipe normal = packProcessRecipe(recipe, index, multiplier);
        NBTTagCompound n = ((NEIPatternPacketAccessor) normal).aeo$getInput();
        n.setTag("hnAssemblySlots", ((NEIPatternPacketAccessor) separate).aeo$getInput());
        n.setString(AssemblyLineRenamer.SOURCE_KEY, AssemblyLineRenamer.RECIPE_ID);
        return normal;
    }
}
