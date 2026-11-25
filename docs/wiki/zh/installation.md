# 安装与旧版升级

[手册目录](../README.md) · [项目首页](../../../README.md) · [English](../en/installation.md)

> [使用手册](../README.md) / [安装与更新](../README.md#安装与更新)

## 下载与版本

当前版本为 **0.4.3**，适配 **GTNH 2.9.1**。[下载 JAR](https://github.com/arturia-Xayah/gtnh-hn/releases/download/v0.4.3-GTNH-2.9.1/gtnh-hn-0.4.3-GTNH-2.9.1.jar) · [查看更新日志](../../../CHANGELOG.md)。

## 升级与安装

原“GTNH现代化升级”已更名为 GTNH-HN。首次换到改名版请手动替换 JAR，之后可继续使用游戏内更新；历史 Release 保留原文件名。

1. 获取 `gtnh-hn-0.4.3-GTNH-2.9.1.jar`，放入实例的 `mods`。联机时客户端与服务器安装相同版本；单人游戏只需安装在客户端。
2. 移除旧版 **GTNH现代化升级 / 结构终端**，以及独立的 **AE Order Optimizer、Plasma Forge Power、AE Storage Vault** JAR。以上功能已包含在整合版中，不能重复安装。
3. 升级前备份整个世界。终端、AE 元件和功率设置保留原注册 ID。**0.3.0 更改了聚合器库存格式，不能直接读取 0.2.x 的仓库文件**；已有聚合器需先在旧版导出库存，升级后新建仓库并重新投入容量来源。不要删除旧文件来解除锁定。

依赖使用目标整合包自带的 GregTech、StructureLib、GTNHLib、AE2、ModularUI2 和 UniMixins。无需另外安装原来的独立 Mod。

---

相关指南：[客户端更新](updates.md) · [兼容性与配置](compatibility.md)
