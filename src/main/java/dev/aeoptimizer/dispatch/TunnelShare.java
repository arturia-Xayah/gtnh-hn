package dev.aeoptimizer.dispatch;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Uncommitted craft counts only. Material ownership always remains with AE. */
public final class TunnelShare {

    public String owner = "";
    public final Map<String, Long> owed = new LinkedHashMap<>();

    public void clear() {
        owner = "";
        owed.clear();
    }

    public void write(NBTTagCompound item) {
        if (owed.isEmpty()) return;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("input", owner);
        NBTTagList rows = new NBTTagList();
        for (Map.Entry<String, Long> e : owed.entrySet()) {
            NBTTagCompound row = new NBTTagCompound();
            row.setString("port", e.getKey());
            row.setLong("sets", e.getValue());
            rows.appendTag(row);
        }
        tag.setTag("ports", rows);
        item.setTag("hnShares", tag);
    }

    public void read(NBTTagCompound item) {
        clear();
        NBTTagCompound tag = item.getCompoundTag("hnShares");
        owner = tag.getString("input");
        NBTTagList rows = tag.getTagList("ports", 10);
        for (int i = 0; i < rows.tagCount(); i++) {
            NBTTagCompound row = rows.getCompoundTagAt(i);
            owed.put(row.getString("port"), Math.max(0L, row.getLong("sets")));
        }
    }
}
