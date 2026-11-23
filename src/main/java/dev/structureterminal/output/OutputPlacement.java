package dev.structureterminal.output;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.IGTHatchAdder;

/** Only the exact check of a known dual-purpose element may admit this bus. */
public final class OutputPlacement {

    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private OutputPlacement() {}

    private static final class Scope {

        final Object controller;
        final IGregTechTileEntity tile;

        Scope(Object c, IGregTechTileEntity t) {
            controller = c;
            tile = t;
        }
    }

    public static boolean allowed(Object controller, IGregTechTileEntity tile) {
        Scope scope = ACTIVE.get();
        return scope != null && scope.controller == controller && scope.tile == tile;
    }

    public static <T> IGTHatchAdder<T> guard(IGTHatchAdder<T> original, boolean dual) {
        return (controller, tile, texture) -> {
            if (tile == null || !(tile.getMetaTileEntity() instanceof MixedOutputBus))
                return original.apply(controller, tile, texture);
            if (!dual || !(controller instanceof MixedOutputHost)) return false;
            Scope previous = ACTIVE.get();
            ACTIVE.set(new Scope(controller, tile));
            try {
                boolean accepted = original.apply(controller, tile, texture);
                if (accepted) ((MixedOutputHost) controller).modernization$approvedOutputs()
                    .add((MixedOutputBus) tile.getMetaTileEntity());
                return accepted;
            } finally {
                if (previous == null) ACTIVE.remove();
                else ACTIVE.set(previous);
            }
        };
    }
}
