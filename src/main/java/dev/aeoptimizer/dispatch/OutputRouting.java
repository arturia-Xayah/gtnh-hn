package dev.aeoptimizer.dispatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.util.GTUtility;

/** Native priority order first, emergency overflow second; the helper still commits each original exactly once. */
public final class OutputRouting {

    private OutputRouting() {}

    public static <T> PeekingIterator<T> order(Iterator<T> source, Map<T, T> normal) {
        if (normal == null || normal.isEmpty()) return Iterators.peekingIterator(source);
        return Iterators.peekingIterator(new Iterator<T>() {

            private final List<T> fallback = new ArrayList<>();
            private int cursor;

            public boolean hasNext() {
                return source.hasNext() || cursor < fallback.size();
            }

            public T next() {
                if (source.hasNext()) {
                    T original = source.next();
                    T first = normal.get(original);
                    if (first == null) return original;
                    fallback.add(original);
                    return first;
                }
                if (cursor >= fallback.size()) throw new NoSuchElementException();
                return fallback.get(cursor++);
            }
        });
    }

    public static IOutputBusTransaction normal(IOutputBusTransaction original) {
        return new ItemNormal(original);
    }

    public static IOutputHatchTransaction normal(IOutputHatchTransaction original) {
        return new FluidNormal(original);
    }

    private abstract static class Normal<ID, S> implements IOutputTransaction<ID, S> {

        final IOutputTransaction<ID, S> original;
        private final OverflowRoute route;

        Normal(IOutputTransaction<ID, S> original) {
            this.original = original;
            this.route = (OverflowRoute) original;
        }

        public boolean isFiltered() {
            return original.isFiltered();
        }

        public boolean isFilteredTo(ID id) {
            return original.isFilteredTo(id);
        }

        public boolean hasAvailableSpace() {
            return route.modernization$normalSpace() && original.hasAvailableSpace();
        }

        public boolean storePartial(ID id, S stack, long total, long perParallel) {
            return hasAvailableSpace() && original.storePartial(id, stack, total, perParallel);
        }

        public boolean needsTotalParallelData() {
            return original.needsTotalParallelData();
        }

        public void complete(ID id) {
            original.complete(id);
        }

        public void commit() {
            original.commit();
        }
    }

    private static final class ItemNormal extends Normal<GTUtility.ItemId, ItemStack> implements IOutputBusTransaction {

        ItemNormal(IOutputBusTransaction original) {
            super(original);
        }

        public IOutputBus getBus() {
            return ((IOutputBusTransaction) original).getBus();
        }
    }

    private static final class FluidNormal extends Normal<GTUtility.FluidId, FluidStack>
        implements IOutputHatchTransaction {

        FluidNormal(IOutputHatchTransaction original) {
            super(original);
        }

        public IOutputHatch getHatch() {
            return ((IOutputHatchTransaction) original).getHatch();
        }
    }
}
