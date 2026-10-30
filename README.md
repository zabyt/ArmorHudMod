# ArmorHudMod

zabyt's HUD tweaks: shows your equipped armor next to the hotbar, fully configurable.

A client-side Fabric mod for Minecraft 26.2 that displays your equipped armor pieces right next to your hotbar, and adds a few more HUD niceties.

## Features

- Armor display panel attached to the hotbar (with an optional elytra slot when the Elytra Slot mod is installed)
- Configurable anchor point: hotbar / above hotbar / any screen corner
- X/Y offsets via sliders, or type an exact value for anything past the slider range
- "Drag armor to position..." to place the panel with the mouse
- Connect-to-offhand mode: shifts the panel away from the offhand slot automatically depending on which arm it is on
- Optional durability bars on the armor items
- Optional FPS counter (current : limit), with its own draggable position
- Effect timers HUD showing remaining time on your status effects, with red/pulse thresholds and icon tinting
- Every HUD element can be toggled and positioned independently

## Configuration

Open the config in-game with [ModMenu](https://modrinth.com/mod/modmenu), or through the icon added by [ZabytLib](https://github.com/zabyt/ZabytLib) on the title/pause screen.

## Building

The clone is self-contained: every jar needed for compilation (ZabytLib, ModMenu, Elytra Slot) is in `libs/`.

```
git clone https://github.com/zabyt/ArmorHudMod
cd ArmorHudMod
gradlew.bat build
```

The built jar ends up in `build/libs/`.

## Dependencies

- Minecraft `~26.2`, Fabric Loader `>=0.19.3`, Java `>=25`, Fabric API
- [ZabytLib](https://github.com/zabyt/ZabytLib) at runtime
- [ModMenu](https://modrinth.com/mod/modmenu) at runtime
- [Elytra Slot](https://modrinth.com/mod/elytra-slot) optional, at runtime

## License

MIT
