package dev.aeoptimizer.dispatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.helpers.DualityInterface;
import appeng.parts.p2p.PartP2PInterface;
import dev.aeoptimizer.api.ProgressAccess;

/** Snapshot of only the providers of the current pattern. No grid-wide search or tick handler. */
public final class TunnelDispatch {

    private final Map<ICraftingMedium, Group> membership = new IdentityHashMap<>();
    private final Map<ICraftingMedium, String> keys = new IdentityHashMap<>();
    private final List<ICraftingMedium> providers;
    private final TunnelShare share;
    private Group hungry;
    private final Map<ICraftingMedium, Integer> indices = new IdentityHashMap<>();

    private static final class Group {

        final PartP2PInterface input;
        final TunnelPolicy policy;
        final String key;
        final List<ICraftingMedium> ports = new ArrayList<>();

        Group(PartP2PInterface input) {
            this.input = input;
            policy = ((TunnelPolicy.Host) input).hn$policy();
            key = address(input) + "/" + input.getFrequency();
        }

        void dirty() {
            input.getTileEntity()
                .markDirty();
        }
    }

    private static String address(PartP2PInterface part) {
        TileEntity t = part.getTileEntity();
        return t.getWorldObj().provider.dimensionId + "/"
            + t.xCoord
            + "/"
            + t.yCoord
            + "/"
            + t.zCoord
            + "/"
            + part.getSide()
                .ordinal();
    }

    public static TunnelDispatch inspect(List<ICraftingMedium> providers, ProgressAccess task) {
        TunnelDispatch route = null;
        Map<PartP2PInterface, Group> groups = null;
        for (ICraftingMedium medium : providers) {
            if (!(medium instanceof DualityInterface)) continue;
            Object host = ((DualityInterface) medium).getHost();
            if (!(host instanceof PartP2PInterface)) continue;
            PartP2PInterface part = (PartP2PInterface) host;
            PartP2PInterface input = part.isOutput() ? part.getInput() : part;
            if (!(input instanceof TunnelPolicy.Host)
                || ((TunnelPolicy.Host) input).hn$policy().mode == TunnelPolicy.DEFAULT) continue;
            if (route == null) {
                route = new TunnelDispatch(providers, task.hn$share(true));
                groups = new IdentityHashMap<>();
            }
            Group group = groups.computeIfAbsent(input, Group::new);
            route.membership.put(medium, group);
            if (part.isOutput()) {
                group.ports.add(medium);
                route.keys.put(medium, address(part));
            }
        }
        if (route == null) {
            TunnelShare old = task.hn$share(false);
            if (old != null) old.clear();
        } else {
            boolean valid = false;
            for (Group group : groups.values()) {
                group.ports.sort(Comparator.comparing(route.keys::get));
                for (int i = 0; i < group.ports.size(); i++) route.indices.put(group.ports.get(i), i);
                if (group.policy.mode == TunnelPolicy.EVEN && group.key.equals(route.share.owner)
                    && group.ports.size() == route.share.owed.size()) {
                    valid = group.ports.stream()
                        .map(route.keys::get)
                        .allMatch(route.share.owed::containsKey);
                }
            }
            if (!valid) route.share.clear();
        }
        return route;
    }

    private TunnelDispatch(List<ICraftingMedium> providers, TunnelShare share) {
        this.providers = providers;
        this.share = share;
    }

    public List<ICraftingMedium> order(int first) {
        hungry = null;
        List<ICraftingMedium> result = new ArrayList<>(providers.size());
        Set<Group> seen = new HashSet<>();
        for (int i = 0; i < providers.size(); i++) {
            ICraftingMedium m = providers.get((first + i) % providers.size());
            Group group = membership.get(m);
            if (group == null) {
                result.add(m);
                continue;
            }
            if (!seen.add(group) || group.ports.isEmpty()) continue;
            int start = group.policy.mode == TunnelPolicy.ROUND ? group.policy.next(group.ports.size()) : 0;
            for (int j = 0; j < group.ports.size(); j++) result.add(group.ports.get((start + j) % group.ports.size()));
        }
        return result;
    }

    /** Called even for busy ports so a round can advance past them. */
    public long allowance(ICraftingMedium m, long max, long tick) {
        Group g = membership.get(m);
        if (!share.owner.isEmpty() && (g == null || !g.key.equals(share.owner))) return 0;
        if (g == null) return max;
        int size = g.ports.size();
        if (g.policy.mode == TunnelPolicy.ROUND) {
            if (!g.policy.visit(indices.get(m), size, tick)) return 0;
            g.dirty();
            return max;
        }
        if (share.owed.isEmpty()) {
            allot(g, (int) max);
        }
        long available = share.owed.get(keys.get(m));
        if (available == 0 && !m.isBusy()) hungry = g;
        return Math.min(max, available);
    }

    private void allot(Group group, int count) {
        int size = group.ports.size();
        share.owner = group.key;
        int start = group.policy.splitStart(count, size);
        for (int i = 0; i < size; i++) {
            String port = keys.get(group.ports.get((start + i) % size));
            long amount = count / size + (i < count % size ? 1 : 0);
            share.owed.merge(port, amount, Math::addExact);
        }
        group.dirty();
    }

    /** All existing shares were tried. Let a free port start its share of the next funded batch. */
    public boolean extend(long batch, long funded) {
        if (hungry == null) return false;
        for (long reserved : share.owed.values()) {
            if (reserved >= funded) return false;
            funded -= reserved;
        }
        int count = (int) Math.min(batch, funded);
        if (count == 0) return false;
        allot(hungry, count);
        return true;
    }

    public void accepted(ICraftingMedium m, long count) {
        if (share.owed.isEmpty()) return;
        String key = keys.get(m);
        share.owed.put(key, share.owed.get(key) - count);
        if (share.owed.values()
            .stream()
            .allMatch(n -> n == 0)) share.clear();
    }
}
