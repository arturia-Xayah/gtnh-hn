package dev.structureterminal.output;

import gregtech.api.enums.HatchElement;

/** Proven native output roles; custom adders are deliberately not inferred from class names. */
public interface OutputRoles {

    int modernization$outputRoles();

    static int of(Object element) {
        if (element == HatchElement.OutputBus) return 1;
        if (element == HatchElement.OutputHatch) return 2;
        return element instanceof OutputRoles ? ((OutputRoles) element).modernization$outputRoles() : 0;
    }
}
