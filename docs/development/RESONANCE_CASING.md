# ME 谐振外壳 / ME Resonance Casing

[开发文档](README.md) · [玩家使用说明](../wiki/zh/storage.md)

2026-11-23，GTNH 2.9.1、Java 25。以下为 0.4.1 的发布前验证，测试工作区基于 `df504de`。

## 实现

新注册名为 `aestoragevault:resonance_casing`，不复用早期废弃的 `aestoragevault:casing`。外壳是普通方块，没有 TileEntity、库存或 AE 节点。配方使用真实 `Casing_LuV`、`Casing_Coil_Superconductor` 和 AE Drive，每次产出一个；LuV Machine Hull 不能替代配方中的 Machine Casing。

结构检查、StructureLib 材料候选、NEI 示例和终端建造都使用新外壳。未成型时直接复用主机的 `formed_shell_smooth`；成型后由同一个 `VaultBlockRenderer` 和 UV 分区绘制。GT 总线仍通过原生 hatch skin 注入使用相同的面绘制函数。客户端保留成型占地缓存以适配省略相邻主机实体的渲染快照；新外壳不再分配无用的 GT 原生外壳覆盖层。

旧 LuV 外壳不再满足结构。换壳只影响成型与 AE 访问，不改写主机 UUID、容量或库存文件。

## 验证

[机器可读汇总](../../verification/resonance-casing/summary.json)及同目录的原始报告记录：

- 正式构建、格式、静态检查、发行检查和 **183 项单元测试**通过。
- 本地专用服务器运行 `vault`、`vault_io` 两套测试及各自的进程重启，共 **105 项检查组**通过。
- 校验真实工作台查方结果、唯一配方、九格原料位置、单件产量；缺料、调换角/边、错误机壳或线圈均不匹配。
- 三种 NEI 示例 × 六个朝向均成型，普通/带退回总线结构分别含 23/22 个专用外壳，外壳不产生 TileEntity。
- 完整及部分预建结构可在同一服务端 tick 内补齐；客户端同步占地涵盖主机、端口、仓室和外壳，并在失效/卸载时清除。
- 拒绝原生外壳（包括 LuV），修复后恢复访问，主机身份和容量保持；AE 存取、输出与进程重启回归通过。
- 0.4.1 正式包的 339 个生产 class 与服务端测试夹具比对通过，其中 7 个 class 仅版本号字面量不同。正式包不包含测试夹具。

没有启动 Minecraft 客户端或 GL 窗口。材质分区和客户端缓存逻辑接受后台检查；不将其表述为游戏画面的目视验收。

## English

The dedicated passive casing uses a new registry ID and shares the controller's base texture, formed-face renderer and lighting. Native LuV casings remain crafting ingredients but no longer form vault shells. Existing controllers retain their inventory identity and capacity when the shell is replaced.

Validation on GTNH 2.9.1 / Java 25 passed 183 unit tests and 105 dedicated-server check groups, including exact crafting inputs, all 18 preview layouts/orientations, one-tick construction, shell failure/recovery, AE IO and process restarts. All 339 release classes match the executed fixture after accounting for the version literal in seven classes. No Minecraft client or GL window was launched; actual in-game visual acceptance is not claimed.
