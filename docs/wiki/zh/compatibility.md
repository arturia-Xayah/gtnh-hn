# 兼容性与配置

[手册目录](../README.md) · [项目首页](../../../README.md) · [English](../en/compatibility.md)

> [使用手册](../README.md) / [兼容性与配置](../README.md#兼容性与配置)

## 版本与存档

- 当前目标为 **GTNH 2.9.1**；其他整合包版本请使用对应适配版。
- 沿用游戏内的 GTO 高级终端图标。历史内部模块 ID 保留，用于识别旧存档；所有功能来自同一个 JAR。
- 结构终端配置仍在 `config/structureterminal.cfg`，调整后重启。仓库仍使用 `<世界目录>/data/aestoragevault/`。
- 聚合器支持结构终端搭建与 NEI 多方块预览。预览尺寸 1／2／3 分别展示链接仓、主动输出、带普通退回总线的示例；仓室实际位置仍可自由选择。
- 请在停服时整体备份、迁移或恢复世界，勿单独回滚库存文件或主机 NBT。移除整合 Mod 后，其新增方块和库存将无法正常访问。

## 常用配置文件

| 文件 | 用途 |
| --- | --- |
| `config/structureterminal.cfg` | 结构终端、胶带白名单、传送门画面等选项 |
| `config/gtnh-modernization.cfg` | 聚合器读取缓存的条数与字节预算 |
| `config/gtnh-modernization-client.cfg` | 终端深色模式、聚合器动画等本地显示偏好 |
| `config/gtnh-modernization-updates.cfg` | 检查更新、版本识别与下载设置 |

配置文件沿用旧名称，项目更名不会清空已有设置。各项的生效方式见对应功能说明。

---

相关指南：[安装与旧版升级](installation.md) · [ME元件聚合存储器](storage.md)
