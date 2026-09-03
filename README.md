# Mitra's Auto Sprinter

A simple client-side auto sprint mod for Fabric.

It starts sprinting automatically when you move forward, as long as vanilla Minecraft would normally allow sprinting. That's the whole mod.

## Features

- 🏃 Automatically sprints while moving forward
- ⌨️ Toggle keybind, default `K`, rebindable in Controls
- 🖥️ HUD indicator that shows if you're sprinting — and why not when you aren't
- 🖱️ Built-in HUD editor: drag the indicator anywhere on screen
- 🎨 Customizable HUD text, colors, and position via config file
- 📜 Follows vanilla sprint rules (see below)
- 💻 Client-side only
- 🪶 Lightweight (one tick handler, one HUD element, no mixins)

## Usage

Press `K` to toggle Auto Sprint on or off.

When on, it sprints automatically while you hold forward, provided vanilla would allow sprinting at that moment.

Rebind the key here:

```text
Options → Controls → Mitra's Auto Sprinter
```

### HUD indicator

While you're in a world, a small indicator shows the current state:

- **Sprint ON** (green) — you're sprinting
- **Sprint OFF - reason** (yellow) — something is blocking sprinting right now, e.g. `Blindness`, `Too Hungry` or `Hit Wall`
- **Sprint OFF** (gray) — auto sprint is toggled off

### Moving the HUD

There's a second keybind, **Open HUD Editor** (unbound by default, rebind it in Controls). Press it in game, then drag the indicator wherever you like — it can't leave the screen. Press `ESC` to save the position.

## How it works

Mitra's Auto Sprinter just holds the sprint key down for you that's it.
You'd get the exact same result by taping the key down or using vanilla's toggle-sprint option.
Because of that, vanilla's rules still fully apply.
It won't sprint while you're sneaking, eating, blinded, too hungry, elytra flying, and so on.
And if vanilla stops your sprint, say you run into a wall, it just stops, same as normal.
Nothing is forced, faked, or sent to the server, so there's nothing for anti-cheats to flag.

## Configuration

All settings live in `config/mitrasautosprinter.properties`. The file is written whenever you toggle the mod or save the HUD position, and you can edit it while the game is closed to change:

- `sprintEnabled` — whether auto sprint starts enabled
- `hudVisible` / `hudBackground` — hide the HUD or its background
- `hudX` / `hudY` — HUD position (also editable with the HUD editor)
- `hudColorOn` / `hudColorBlocked` / `hudColorOff` / `hudBackgroundColor` — colors, as `#RRGGBBAA`
- `textOn` / `textOff` / `textBlockedFormat` — HUD text; `%s` in the blocked format is replaced with the reason
- `reasonDead`, `reasonHungry`, `reasonBlind`, ... — the label shown for each blocking reason

Text values are capped at 64 characters. If a value is missing or invalid, the default is used.

## 📜 License

Licensed under **CC0 1.0 Universal**.

Do whatever you want with it. Seriously.

See [`LICENSE`](LICENSE) for the full license text.
