# AE 下单优化 / AE order optimization

[中文首页](../../README.md) · [English README](../../README.en.md)

## 元件与使用

ME 下单优化元件的合成配方：

```text
金锭  石英    金锭
红石  工作台  红石
金锭  石英    金锭
```

连接到下单所用的网络，右键可开启“详细选路报告”。不占频道，待机耗电 1 AE/t，每个网络最多一个。网络合并后多出的元件会断开并提示冲突，移除冲突后尝试恢复连接。

有效元件存在时，网络使用改造后的 Lite 下单计算和支持接收端上的批量发配。没有有效元件时保留 AE 原本选定的模式。子网使用其他网络样板时，以下单网络为准。

当前路线遇到循环或多输出样板会停止，并在玩家聊天及服务器日志报告。未选择的其他路线不会阻断订单。“详细模式”仅报告主产物及其配方数，不改变选择顺序。环报告只显示成环的主产物链。带颜色、下划线的主产物可点击查看样板方块坐标；当前维度已加载位置高亮 30 秒，跨维度报告维度名及坐标。定位链接有效期为 10 分钟，移动样板后需重新计算获取新位置。无环、单输出仍可能因为缺少原料或样板而无法下单。

## 批量发配

无需倍增样板。CPU 会按库存、AE 能源、协处理器和接收端能力合并投料，一批最多 4096 份。批量投料不提高机器的物理并行。

原生 ME 接口可使用批量投料；第三方接收端需明确支持。工作台配方及其他不适用的场景继续逐份发配；严格阻挡接口未安装限制批量发配卡时仍按一套发配。

目前路径检查会先展开所选样板图，因此即使部分中间材料已有库存，也可能继续检查其上游样板。遇到报告时请按提示修正相应路径。

## 限制批量发配卡

合成：**高级卡 + 红石比较器**，无序合成。安装在存放样板的发送接口的升级槽中，最多一张；支持方块及片状 ME 接口，以及 AE2FC 二合一接口。与高级阻挡卡的安装位置不同：高级阻挡卡仍装在接收子网的接口上。新卡使用 AE 原生升级槽、升级计数与安装上限；支持设备按高级阻挡卡注册，列入 AE 创造栏。物品仍使用本 Mod 自己的 ID。

物品说明默认显示用途与设置入口，按住 Shift 查看同类原料合计、严格／智能阻挡、超限处理及动态生成的可安装设备列表。

点击发送接口左侧的 `≤`，分别填写“每种物品 / 单批（件）”和“每种流体 / 单批（mB）”。`0` 表示不限，支持 K/M/G/T 十进制后缀，如 `1M = 1,000,000`、`2.5M = 2,500,000`。两项会一起保存到接口；拔卡后设置保留，但不再生效。仅以下单网络是否有有效优化器为准。

批数取各原料 `上限 ÷ 单套用量` 向下取整后的最小值，并受到已有库存、电力和并行能力限制。它限制的是**每种原料**，不是所有原料总量；同一种原料在多个槽位出现时先合计，物品 NBT 不同则分别计算。单套用量超过上限时该接口暂停，并在设置界面及服务器日志说明；不会拆开一套配方或越过上限强行发送。

例：每种流体限制 `1M`，每种物品限制 `2M`；配方消耗 A/B/C 流体分别为 100k/50k/20k mB、铁锭 250k 个，最多发送 **8 套/批**。

- 严格阻挡：允许整批发送，下一批仍由 AE 原版阻挡条件判断；接收子网使用高级阻挡卡时，等待对应子网清空。
- 智能阻挡：保留同配方连续发送规则，每批都受卡片限制。一个 tick 可能发送多批。
- 普通接收容器／未装高级阻挡卡的子网：仅限制单批数量，接收和缓存遵循原生逻辑。

上限不足一套时，接口 `≤` 按钮的悬停文字及设置页红字显示“单套原料超过上限，发配已暂停”；服务器日志记录维度、接口坐标和两个上限。同一接口重复日志至多每 200 tick 一次，不发聊天消息或弹窗。AE 原生合成状态栏仍使用“阻挡模式”这一通用原因。

不再存在额外的强制清空保护。上限应与接收端实际可用空间匹配：智能阻挡、其他写入来源或不均匀消耗仍可能造成积压；新卡不为不同流体预留储罐。设置不会回收已经进入接口发送缓存的原料。

## ME 输出临时超限

右键优化器，切换“ME 输出临时超限”。默认开启，旧存档保持开启；设置随优化器保存，仅作用于同一网络的原生 ME 输出总线、ME 输出仓及二合一 ME 输出总线。隔离子网各自设置，不使用下单网络或样板来源网络的开关。

开启后，完成产物可以暂存至超过配置容量，正常输出端有空间时优先使用；后续配方的开工检查仍遵守原容量和过滤设置。关闭后恢复原版接收规则，已经存在的超额缓存不会被清空、截断或阻止输出。严格容量检查也能处理关闭前留下的超额缓存。

这个开关不改变下单计算、批量发配、限制批量发配卡或自动重命名。它控制的是额外接收保护，不是永久扩容；关闭时，尚未完成的配方会按原版输出规则处理。

## 装配线自动重命名

右键 ME 下单优化元件，开启“装配线自动重命名”（默认关闭）。在该网络的样板终端中从 NEI 导入装配线配方时，每种相同原料的第一份保留原名，第二、第三份依次使用固定名称 `重命名1`、`重命名2`。数量与输入位置不变；不同原料分别编号，流体和已有自定义名称的原料不改名。若目标名称已被同类原料占用，会跳过该编号。

名称在中英文客户端都相同。把成型机的名称模具命名为完全相同的 `重命名1`、`重命名2`，得到的物品就能匹配样板；此功能不会额外添加物品 NBT 标记。成型机仍需实际供货和加工，不会直接产生改名物品。

开启第二个开关“导入时生成改名样板”后，从 NEI 导入时就会额外生成缺少的 `1 个原料 → 1 个改名原料` 加工样板，放入玩家背包。主样板仍需正常点击编码。辅助样板不把常驻名称模具列为消耗材料；请将它们安装到使用对应名称模具的成型机接口。

每张辅助样板消耗一张普通空白样板，依次从终端空白样板槽、背包、AE 网络取用。网络提取遵守权限及耗电。背包空间或空白样板不足时取消本次导入，保留原配方；若网络实际提取少于预检数量，已取出的空白样板会留在终端空白样板槽，不生成半套样板。

自动去重检查当前网络可见的已安装样板、背包样板及终端样板输出槽。仅跳过输入、输出和 NBT 精确一致的单输入、单输出、1→1 加工样板；不会因为“这个产物可合成”就误跳过。未安装且存放在其他箱子或 ME 库存中的样板不在此次去重范围。关闭第二个开关时，只修改导入配方的重复输入，不生成辅助样板。

仅影响开关开启后的 NEI 装配线导入，不追溯修改已编码样板，也不修改其他机器配方或手动编辑。以样板终端当前连接的网络为准，不读取提供样板的其他网络的开关。

## 原料＋标记

独立于下单优化元件：原料在当前网络已有合成样板时显示＋。支持物品及流体。物品区分 metadata/NBT，流体匹配遵守该 AE 版本原生规则。

标记只表示样板存在，不能代替缺料检查。

## English

Craft the optimizer with gold/quartz/gold, redstone/crafting table/redstone, gold/quartz/gold. Connect one to the submitting grid; right-click for detail mode. It uses 1 AE/t and no channel. Merged-grid duplicates disconnect until the conflict is resolved.

The optimizer also has an independent **ME output overflow** switch, defaulting to ON for new and existing worlds. It covers native ME Output Buses, ME Output Hatches and Dual ME Output Buses on the output device’s own grid. ON accepts completion overflow while leaving new recipe checks at native capacity. OFF restores native acceptance without truncating existing excess or stopping its normal flush. The setting persists; isolated subnets are independent. It does not disable planning, batch dispatch, the limit card or renaming.

An active optimizer selects the modified Lite planner and supported bulk dispatch. Without it, native planning and dispatch remain. The submitting grid governs cross-grid pattern borrowing. Selected-route cycles or multi-output patterns stop the order and report the issue. Detail mode reports main outputs and recipe counts without changing native route selection. Cycles display their main-output chain. Colored, underlined outputs link to provider coordinates and a 30-second outline in the current dimension; other dimensions report name and coordinates. Links expire after ten minutes; recalculate after moving patterns. Missing stock or patterns can still prevent crafting.

Keep unit patterns. Batches contain at most 4096 crafts and follow stock, energy, coprocessor and receiver limits. Native ME interfaces support batching; unsupported receivers and workbench recipes retain unit dispatch. Strict Blocking retains unit dispatch unless the interface has a Batch Dispatch Limit Card. Batching does not add processing parallelism.

The current validator expands the selected pattern graph before stock consumption, so it may inspect upstream patterns even for stocked intermediates.

The input **+** badge needs no optimizer. It indicates a pattern in the current grid, not enough stock for every dependency. Item identity preserves metadata/NBT; fluids follow this AE version's native matching.

**Assembly-line auto-renaming:** enable the optional switch in the optimizer (off by default). NEI assembly-line imports retain the first occurrence of each exact item and name later occurrences `重命名1`, `重命名2`, etc. Different items number independently; counts/positions, fluids and existing custom names are preserved. Occupied names are skipped. Names are fixed across client languages and match a Forming Press using a Name Mold with that exact name. The second, default-off switch creates missing 1:1 renaming processing patterns immediately on import and delivers them to your inventory. The main pattern is still encoded normally. Blanks are consumed from the terminal slot, your inventory, then the powered ME network. Install the auxiliary patterns at Forming Presses with matching Name Molds; molds are not consumed recipe inputs. Exact unit processing patterns already installed in the current crafting grid, carried in your inventory or in the terminal output slot are skipped. Loose patterns in other containers/ME storage are not scanned. Insufficient space or blanks cancels the import without replacing the old matrix; a partial network extraction is retained as blank patterns in the terminal slot. The terminal's own active grid controls this feature; existing patterns, manual edits and other recipe maps are unaffected.

[开发与内部实现 / Development](../development/README.md)

### Batch Dispatch Limit Card

Craft an Advanced Card with a redstone comparator (shapeless). Install one card in the **sending** block or part ME interface, including AE2FC dual interfaces. Use the left `≤` button to set independent per-item-type and per-fluid-type caps. Fluids use mB; `0` is unlimited; K/M/G/T are decimal suffixes. Limits apply only when the **ordering** network has an optimizer. They persist on the interface after card removal but remain inactive without a card.

Repeated ingredients are added before division; item NBT variants remain distinct. A recipe larger than a cap pauses this interface without withdrawing inputs. The 1M-fluid / 2M-item example (100k, 50k, 20k mB and 250k items per recipe) sends eight sets per batch. One batch consumes one CPU operation.

Strict Blocking may now send a whole batch, then uses native waiting rules. Smart Blocking retains consecutive dispatch and may send several batches in a tick. The previous forced-empty guard is removed. Limits do not reserve tank space or change native partial-acceptance queues; size them for the receiving inventory. Editing limits does not recall materials already queued for delivery.

The card uses AE upgrade-slot validation/counting and the Advanced Blocking Card's supported-host list, but keeps its own mod item ID. Hold Shift for detailed rules and the localized supported-device list. Too-small caps show a warning on the interface button/settings and in rate-limited server logs; AE's crafting status currently uses its generic blocking reason.
