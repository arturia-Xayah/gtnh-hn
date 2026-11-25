# ME Dual Storage Bus

Craft one ME Storage Bus and one ME Fluid Storage Bus together in any arrangement. Mount the result on grid A, facing a Dual Interface on grid B. Grid A uses one channel to access the items and fluids exposed on that face. An ordinary chest exposes items and a tank exposes fluids.

Open the bus and drag items or NEI fluids into the same filter. It starts with 18 slots. Each Capacity Card adds nine, up to 63. An empty filter allows both types. Mark iron and water to allow only those two, or mark only water to block every item. An Inverter Card switches the entire list to a blacklist. A Fuzzy Card affects item matching only.

Strict extraction is enabled by default. The standard controls set extraction mode, read/write access, priority and Reshuffle access. The partition button fills slots with items and then fluids from the attached storage.

## Type Limit Card

Craft one advanced card and one chest together. Install one card in the upgrade slots of an item, fluid or dual ME Storage Bus on the distribution network. The first successful insertion claims the type until the target empties. Dual buses share a single type across items and fluids. If another writer mixes types, extract down to one type to resume. GT programmed circuits of every setting can pass without claiming the type.

For assembly lines, connect a separate subnet to each slice and give the distribution buses descending priorities in ingredient order. Size each target for a full batch. Excess may spill into the next slice. Use assembly-line input renaming when duplicate ingredients need separate slices.

With a subnet per slice, install the card on the bus feeding that slice. Two patterns that both use iron can feed the same slice. If the assembly line needs identical ingredients in separate positions, distinguish those positions with renamed inputs or fixed filters.
