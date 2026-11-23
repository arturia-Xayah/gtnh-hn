# Forge and fusion power controls

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/power-controls.md)

> [User guide](../README.en.md) / [Building and machine controls](../README.en.md#building-and-machine-controls)

**On this page**

- [Setting a limit](#setting-a-limit)
- [Input colors](#input-colors)
- [When changes apply](#when-changes-apply)
- [Continuous fusion operation](#continuous-fusion-operation)
- [Copying settings](#copying-settings)

## Setting a limit

Open the existing controller, click **P**, select the input color, enter maximum amperage and a voltage unit, then save. Units range from ULV to MAX. Supported: DTPF, ordinary Fusion Mk-I–V and Compact Fusion Mk-I–V. **0 / -1** means automatic. Fractional amperage is supported and converted conservatively to whole EU/t. Voltage is a display/conversion unit; it does not alter machine or hatch tier. The limit is a budget, not a fixed power draw: recipes, overclocks and supported parallelism determine actual consumption. The panel shows EU/t and equivalent amperage/voltage, using scientific notation for large values with exact values on hover.

## Input colors

Colors and the unpainted group have independent budgets. Internal slots use their input assembly's color. Native shared unpainted ingredients and separation rules remain; recipes fully satisfied by unpainted ingredients use the unpainted budget. Energy hatches do not need colors. Limits do not create isolated power supplies or simultaneous different recipes.

## When changes apply

Edits apply to the next recipe. Re-forming after supply changes clamps excessive manual limits; incomplete structures retain settings. A clamped limit is saved and is not automatically raised when a larger supply is reinstalled. Native energy failures, ingredient shortages and blocked outputs still stop machines normally.

## Continuous fusion operation

Fusion startup EU is separate from processing EU/t. Startup eligibility, native recipe order, caches and continuous handoff remain; no order-priority scheduler is added. Change ingredient availability through AE to select a different recipe. Budgets below base recipe EU/t cannot run it by underclocking.

## Copying settings

Matter Manipulator copies settings through GT's normal configuration path. Data Orb: left-click to capture, right-click to paste. Only settings are copied; no energy, stock or progress. Previous settings and voltage IDs remain compatible.

---

Related guides: [AE order optimization](ae-ordering.md)
