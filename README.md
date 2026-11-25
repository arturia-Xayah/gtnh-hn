> 本 Mod 由 **GPT-6（Codex）** 实现移植、开发与整合。

<div align="center">

<a id="gtnh-handy--nimble"></a>
<img src="docs/assets/branding/hn-logo.png" alt="GTNH: Handy & Nimble — HN Logo" width="820">

**GTNH-HN** · 顺手，轻巧。

**简体中文** · [English](README.en.md) · [版本下载](https://github.com/arturia-Xayah/gtnh-hn/releases)

[![Download JAR](https://img.shields.io/github/v/release/arturia-Xayah/gtnh-hn?include_prereleases&label=Download)](https://github.com/arturia-Xayah/gtnh-hn/releases)

Minecraft **1.7.10** · GTNH **2.9.1**

</div>

**GTNH-HN** 是面向 **GT New Horizons** 的轻量功能扩展与体验优化，重点是减少日常游玩中的重复操作。它提供更方便的多方块建造、AE 样板与下单工具，以及机器功率、集中存储和传送管理。

**[使用手册](docs/wiki/README.md)** · [下载 JAR](https://github.com/arturia-Xayah/gtnh-hn/releases/download/v0.4.3-GTNH-2.9.1/gtnh-hn-0.4.3-GTNH-2.9.1.jar) · [更新日志](CHANGELOG.md)

## 目录

| 分类 | 功能与使用说明 |
| --- | --- |
| [安装与更新](docs/wiki/README.md#安装与更新) | <a id="安装与旧版升级"></a>[安装与旧版升级](docs/wiki/zh/installation.md) · <a id="客户端更新"></a>[客户端更新](docs/wiki/zh/updates.md) |
| [建造与机器控制](docs/wiki/README.md#建造与机器控制) | <a id="结构终端"></a>[高级结构终端](docs/wiki/zh/structure-terminal.md) · <a id="bc-范围设置"></a>[BC 范围设置](docs/wiki/zh/buildcraft-range.md) · <a id="锻炉与聚变功率控制"></a>[锻炉与聚变功率控制](docs/wiki/zh/power-controls.md) |
| [AE 网络与自动化](docs/wiki/README.md#ae-网络与自动化) | <a id="样板原料标记"></a>[样板原料标记](docs/wiki/zh/craftable-inputs.md) · <a id="集成样板"></a>[集成样板](docs/wiki/zh/integrated-patterns.md) · <a id="me-打包胶带"></a>[ME 打包胶带](docs/wiki/zh/packing-tape.md) · <a id="ae-下单优化"></a>[AE 下单优化](docs/wiki/zh/ae-ordering.md) · <a id="me-进阶发信器"></a>[ME 进阶发信器](docs/wiki/zh/level-emitter.md) · <a id="二合一-me-输入总线"></a>[二合一 ME 输入总线](docs/wiki/zh/dual-input-bus.md) · <a id="二合一-me-输出总线"></a>[二合一 ME 输出总线](docs/wiki/zh/dual-output-bus.md) |
| [ME 存储](docs/wiki/README.md#me-存储) | <a id="me元件聚合存储器"></a>[ME元件聚合存储器](docs/wiki/zh/storage.md) · <a id="读取缓存配置"></a>[读取缓存配置](docs/wiki/zh/storage-cache.md) |
| [传送与场景](docs/wiki/README.md#传送与场景) | <a id="传送门投影主机"></a>[传送门投影主机](docs/wiki/zh/portal-projector.md) |
| [兼容性与配置](docs/wiki/README.md#兼容性与配置) | <a id="兼容性与存档"></a>[兼容性与配置](docs/wiki/zh/compatibility.md) |
| [项目资料](docs/wiki/README.md#项目资料) | [更新日志](CHANGELOG.md) · <a id="开发者文档"></a>[开发文档](docs/development/README.md) · [许可证](LICENSE) · [鸣谢](THIRD_PARTY_NOTICES.md) |

## 快速安装

当前版本 **0.4.3**，适配 **GTNH 2.9.1 / Minecraft 1.7.10**。

1. 从 [Releases](https://github.com/arturia-Xayah/gtnh-hn/releases) 下载对应 GTNH 版本的 JAR，放入 `mods`；联机客户端和服务器使用相同版本。
2. 替换旧版 GTNH-HN／GTNH现代化升级／结构终端 JAR，并移除重复安装的独立 AE Order Optimizer、Plasma Forge Power、AE Storage Vault。
3. 升级前备份整个世界。**使用 0.2.x 聚合器的存档需先导出库存**，按[升级说明](docs/wiki/zh/installation.md)迁移。

原“GTNH现代化升级”已更名为 **GTNH-HN**，首次换到改名版需手动替换 JAR。依赖使用目标 GTNH 整合包自带版本。详细步骤见[安装指南](docs/wiki/zh/installation.md)与[游戏内更新](docs/wiki/zh/updates.md)。

## 关于文档

详细操作、设置与兼容性说明统一收录在[使用手册](docs/wiki/README.md)。构建、实现细节、实验和验证记录集中在[开发者文档](docs/development/README.md)。
