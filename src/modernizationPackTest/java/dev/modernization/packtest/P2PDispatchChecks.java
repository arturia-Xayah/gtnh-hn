package dev.modernization.packtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.helpers.DualityInterface;
import appeng.me.cache.CraftingGridCache;
import appeng.parts.p2p.PartP2PInterface;
import appeng.tile.misc.TileInterface;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.ItemBatchLimitCard;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

final class P2PDispatchChecks {

    private final WorldServer world;
    private final int x, y, z;
    private final PartP2PInterface[] outputs = new PartP2PInterface[4];
    private final IGregTechTileEntity[] buses = new IGregTechTileEntity[4];
    private final ItemStack dualTunnel = new ItemStack(GameRegistry.findItem("ae2fc", "part_fluid_p2p_interface"));
    private PartP2PInterface input;
    final List<Map<String, Object>> observations = new ArrayList<>();

    P2PDispatchChecks(WorldServer world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private IPartHost cable(int px, int pz) {
        appeng.api.definitions.IDefinitions defs = AEApi.instance()
            .definitions();
        world.setBlock(
            px,
            y,
            pz,
            defs.blocks()
                .multiPart()
                .maybeBlock()
                .get(),
            0,
            3);
        IPartHost host = (IPartHost) world.getTileEntity(px, y, pz);
        host.addPart(
            defs.parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            null);
        return host;
    }

    void place(Block iface) {
        appeng.api.definitions.IDefinitions defs = AEApi.instance()
            .definitions();
        for (int d = 2; d <= 10; d++) {
            int px = x - d;
            IPartHost host = cable(px, z);
            if (d % 2 != 0) continue;
            host.addPart(dualTunnel.copy(), ForgeDirection.SOUTH, null);
            PartP2PInterface part = (PartP2PInterface) host.getPart(ForgeDirection.SOUTH);
            if (d == 2) {
                input = part;
                continue;
            }
            int i = (d - 4) / 2;
            outputs[i] = part;
            int busZ = z + 1;
            if (i % 2 == 0) {
                world.setBlock(px, y, z + 1, iface, 0, 3);
                TileInterface receiver = (TileInterface) world.getTileEntity(px, y, z + 1);
                receiver.getInterfaceDuality()
                    .getUpgrades()
                    .setInventorySlotContents(
                        0,
                        defs.materials()
                            .cardAdvancedBlocking()
                            .maybeStack(1)
                            .get());
                IPartHost storage = cable(px, z + 2);
                storage.addPart(
                    defs.parts()
                        .storageBus()
                        .maybeStack(1)
                        .get(),
                    ForgeDirection.SOUTH,
                    null);
                world.setBlock(
                    px,
                    y - 1,
                    z + 2,
                    defs.blocks()
                        .energyCellCreative()
                        .maybeBlock()
                        .get(),
                    0,
                    3);
                busZ = z + 3;
            }
            ItemStack stack = ItemList.Hatch_Input_Bus_ULV.get(1);
            check(
                ((ItemBlock) stack.getItem()).placeBlockAt(
                    stack,
                    FakePlayerFactory.getMinecraft(world),
                    world,
                    px,
                    y,
                    busZ,
                    1,
                    0,
                    0,
                    0,
                    stack.getItemDamage()),
                "GT bus placed");
            buses[i] = (IGregTechTileEntity) world.getTileEntity(px, y, busZ);
            buses[i].setFrontFacing(ForgeDirection.NORTH);
        }
    }

    void link(ItemStack pattern) throws Exception {
        input.getProxy()
            .getP2P()
            .updateFreq(input, 32771);
        for (int i = 0; i < 4; i++)
            outputs[i] = (PartP2PInterface) outputs[i].convertToOutput(null, dualTunnel.copy(), 32771);
        input.getInterfaceDuality()
            .getPatterns()
            .setInventorySlotContents(0, pattern);
    }

    void configure(TileInterface sender, ICraftingPatternDetails pattern) throws Exception {
        ItemStack card = new ItemStack(ItemBatchLimitCard.INSTANCE);
        ItemStack plain = AEApi.instance()
            .definitions()
            .parts()
            .p2PTunnelMEInterface()
            .maybeStack(1)
            .get();
        PartP2PInterface ordinary = (PartP2PInterface) ((IPartItem) plain.getItem()).createPartFromItemStack(plain);
        check(
            !ordinary.getInterfaceDuality()
                .getUpgrades()
                .isItemValidForSlot(0, card),
            "ordinary P2P input rejects limit card");
        ordinary.output = true;
        check(
            ordinary.getInterfaceDuality()
                .getUpgrades()
                .isItemValidForSlot(0, card),
            "ordinary P2P output accepts limit card");
        check(
            !input.getInterfaceDuality()
                .getUpgrades()
                .isItemValidForSlot(0, card),
            "dual P2P input rejects limit card");
        CraftingGridCache cache = sender.getProxy()
            .getGrid()
            .getCache(ICraftingGrid.class);
        List<ICraftingMedium> mediums = cache.getMediums(pattern);
        for (int i = 0; i < outputs.length; i++) {
            PartP2PInterface part = outputs[i];
            DualityInterface d = part.getInterfaceDuality();
            check(
                part.getInput() == input && part.getProxy()
                    .isActive() && mediums.contains(d),
                "linked provider " + i);
            d.getConfigManager()
                .putSetting(Settings.BLOCK, i % 2 == 0 ? YesNo.YES : YesNo.NO);
            d.getConfigManager()
                .putSetting(Settings.SMART_BLOCK, YesNo.NO);
            if (i < 2) {
                check(
                    d.getUpgrades()
                        .isItemValidForSlot(0, card),
                    "live P2P output accepts limit card");
                d.getUpgrades()
                    .setInventorySlotContents(0, card.copy());
                check(
                    !d.getUpgrades()
                        .isItemValidForSlot(1, card),
                    "P2P rejects duplicate");
                net.minecraftforge.common.util.FakePlayer player = FakePlayerFactory.getMinecraft(world);
                player.setPosition(part.getTileEntity().xCoord + .5, y + .5, z + .5);
                appeng.container.implementations.ContainerInterface menu = new appeng.container.implementations.ContainerInterface(
                    player.inventory,
                    part);
                player.openContainer = menu;
                ((BatchLimitSettings) menu).setBatchLimits(i == 0 ? 32 : 16, 0);
                check(
                    ((BatchLimitSettings) d).batchItemLimit() == (i == 0 ? 32 : 16),
                    "P2P menu stores this endpoint's limit");
            }
        }
        // The example specifies the visit order. Keep all real providers, including the input facing air.
        List<DualityInterface> order = Arrays.asList(
            outputs[0].getInterfaceDuality(),
            outputs[1].getInterfaceDuality(),
            outputs[2].getInterfaceDuality(),
            outputs[3].getInterfaceDuality(),
            input.getInterfaceDuality());
        mediums.sort(java.util.Comparator.comparingInt(order::indexOf));
        buses[3].setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 32));
    }

    void expect(String mode, int[] delivered, int[] pending) {
        int[] inDestination = new int[4], inQueue = new int[4];
        for (int i = 0; i < 4; i++) {
            ItemStack iron = buses[i].getStackInSlot(0);
            long count = (iron == null ? 0 : iron.stackSize) - (i == 3 ? 32 : 0);
            check(count == 4L * delivered[i], "output " + (i + 1) + " delivered " + count + " iron");
            List<appeng.api.storage.data.IAEStack<?>> queue = outputs[i].getInterfaceDuality()
                .getWaitingToSend();
            long backlog = queue == null ? 0
                : queue.stream()
                    .mapToLong(stack -> stack.getStackSize())
                    .sum();
            check(backlog == 4L * pending[i], "output " + (i + 1) + " pending " + backlog + " iron");
            inDestination[i] = (int) (count / 4);
            inQueue[i] = (int) (backlog / 4);
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("mode", mode);
        row.put("destination_sets", inDestination);
        row.put("queued_sets", inQueue);
        observations.add(row);
    }

    void reset() {
        for (int i = 0; i < 4; i++) {
            List<appeng.api.storage.data.IAEStack<?>> queue = outputs[i].getInterfaceDuality()
                .getWaitingToSend();
            if (queue != null) queue.clear();
            buses[i].setInventorySlotContents(0, i == 3 ? new ItemStack(Items.iron_ingot, 32) : null);
        }
    }

    void smartThirdWithoutCard() {
        DualityInterface third = outputs[2].getInterfaceDuality();
        third.getUpgrades()
            .setInventorySlotContents(0, null);
        third.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.YES);
    }

    void prepareIndependentQueues() {
        for (PartP2PInterface output : outputs) output.getInterfaceDuality()
            .getConfigManager()
            .putSetting(Settings.BLOCK, YesNo.NO);
        limitSecond(1);
    }

    void limitSecond(int amount) {
        ((BatchLimitSettings) outputs[1].getInterfaceDuality()).setBatchLimits(amount, 0);
    }

    void mode(int expected) {
        net.minecraftforge.common.util.FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.setPosition(input.getTileEntity().xCoord + .5, y + .5, z + .5);
        appeng.container.implementations.ContainerInterface menu = new appeng.container.implementations.ContainerInterface(
            player.inventory,
            input);
        player.openContainer = menu;
        ((dev.aeoptimizer.dispatch.TunnelPolicy.Menu) menu).hn$cycle();
        check(
            ((dev.aeoptimizer.dispatch.TunnelPolicy.Host) input).hn$policy().mode == expected,
            "set routing through GUI");
    }

    void prepareFirstAvailable(ItemStack secondPattern) {
        reset();
        for (int i = 0; i < outputs.length; i++) {
            buses[i].setInventorySlotContents(0, null);
            DualityInterface port = outputs[i].getInterfaceDuality();
            port.getUpgrades()
                .setInventorySlotContents(0, new ItemStack(ItemBatchLimitCard.INSTANCE));
            ((BatchLimitSettings) port).setBatchLimits(0, 0);
            port.getConfigManager()
                .putSetting(Settings.BLOCK, YesNo.YES);
            port.getConfigManager()
                .putSetting(Settings.SMART_BLOCK, YesNo.YES);
        }
        input.getInterfaceDuality()
            .getPatterns()
            .setInventorySlotContents(1, secondPattern);
        mode(dev.aeoptimizer.dispatch.TunnelPolicy.FIRST_AVAILABLE);
    }

    int onlyFilledPort() {
        int first = -1;
        for (int i = 0; i < buses.length; i++) {
            if (buses[i].getStackInSlot(0) == null) continue;
            check(first == -1, "first batch stayed together");
            first = i;
        }
        check(first != -1, "first batch reached a port");
        return first;
    }

    void expectFirstAvailable(String label, int first, int firstCount, int otherCount) {
        int[] counts = new int[buses.length];
        int total = 0;
        for (int i = 0; i < buses.length; i++) {
            ItemStack stack = buses[i].getStackInSlot(0);
            if (stack != null) {
                check(stack.getItem() == Items.iron_ingot, "received recipe ingredient");
                counts[i] = stack.stackSize;
                total += stack.stackSize;
                if (i != first) check(stack.stackSize == otherCount, "one fallback destination");
            }
            List<?> waiting = outputs[i].getInterfaceDuality()
                .getWaitingToSend();
            check(waiting == null || waiting.isEmpty(), "no leftover input at port " + i);
        }
        check(counts[first] == firstCount && total == firstCount + otherCount, label + " " + Arrays.toString(counts));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("mode", label);
        row.put("iron_per_destination", counts);
        observations.add(row);
    }

    void checkRebind() {
        PartP2PInterface old = outputs[0];
        PartP2PInterface replacement = (PartP2PInterface) old
            .convertToOutput(null, dualTunnel.copy(), input.getFrequency());
        outputs[0] = replacement;
        check(replacement != old && replacement.isOutput(), "memory card rebind replaces endpoint");
        BatchLimitSettings limits = (BatchLimitSettings) replacement.getInterfaceDuality();
        check(limits.hasBatchLimitCard() && limits.batchItemLimit() == 32, "rebind preserves local card and limits");
        check(
            ((BatchLimitSettings) outputs[1].getInterfaceDuality()).batchItemLimit() == 16,
            "sibling limit unchanged");
    }

    void zeroLimitThird() {
        outputs[2].getInterfaceDuality()
            .getUpgrades()
            .setInventorySlotContents(0, new ItemStack(ItemBatchLimitCard.INSTANCE));
    }
}
