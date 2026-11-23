package dev.structureterminal;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class TerminalConfig {

    public static int cells = 100000;
    public static int scanMillis = 500;
    public static int budget = 25;
    public static int rounds = 10000;
    public static int reach = 8;
    public static int buildMillis = 1000;
    public static boolean demolish = true;
    public static boolean demolishTiles;
    public static int sceneResolution = 1024;
    public static int sceneFps = 30;
    public static int mixedInputId = 31990, mixedAdvancedInputId = 31991, mixedOutputId = 31992;

    private TerminalConfig() {}

    public static void load(File file) {
        Configuration c = new Configuration(file);
        c.load();
        cells = c.getInt("maxPreviewCells", "limits", cells, 256, 1000000, "Maximum cells in a structure scan.");
        scanMillis = c
            .getInt("maxScanMillis", "limits", scanMillis, 50, 5000, "Abort excessively slow structure scans.");
        budget = c.getInt(
            "placementBudget",
            "limits",
            budget,
            1,
            200,
            "Blocks per tick for replacement and demolition; normal builds are synchronous.");
        rounds = c.getInt("maxBuildRounds", "limits", rounds, 1, 100000, "Maximum rounds before a job stops.");
        buildMillis = c.getInt(
            "maxInstantBuildMillis",
            "limits",
            buildMillis,
            50,
            5000,
            "Safety timeout for synchronous construction; an incomplete request stops, never silently continues next tick.");
        reach = c.getInt("controllerReach", "limits", reach, 4, 16, "Player must remain near the controller.");
        demolish = c.getBoolean(
            "allowDemolition",
            "features",
            true,
            "Allow explicitly confirmed removal of matching empty structure blocks.");
        demolishTiles = c.getBoolean(
            "allowTileEntityDemolition",
            "features",
            false,
            "Opt-in: remove inspectably empty tile entities. Disabled until pack-specific inventory/cover behavior is verified.");
        sceneResolution = c.getInt(
            "portalSceneResolution",
            "client",
            sceneResolution,
            256,
            2048,
            "Longest side of a destination scene texture in pixels. Each visible portal has its own texture.");
        sceneFps = c.getInt(
            "portalSceneFps",
            "client",
            sceneFps,
            5,
            60,
            "Maximum live destination camera frames per second per visible portal.");
        mixedInputId = c
            .getInt("mixedInputId", "machines", mixedInputId, 1, 32766, "GT ID for the dual stocking input bus.");
        mixedAdvancedInputId = c.getInt(
            "mixedAdvancedInputId",
            "machines",
            mixedAdvancedInputId,
            1,
            32766,
            "GT ID for the advanced dual stocking input bus.");
        mixedOutputId = c
            .getInt("mixedOutputId", "machines", mixedOutputId, 1, 32766, "GT ID for the dual ME output bus.");
        if (c.hasChanged()) c.save();
    }
}
