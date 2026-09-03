# Mitra's Auto Sprinter

A simple client-side auto sprint mod for Fabric.

It starts sprinting automatically when you move forward, as long as vanilla Minecraft would normally allow sprinting. That's the whole mod.

## Features

- 🏃 Automatically sprints while moving forward
- ⌨️ Toggle keybind, default `K`, rebindable in Controls
- 🖥️ Small "Sprint ON" HUD indicator
- 📜 Follows vanilla sprint rules (see below)
- 💻 Client-side only
- 🪶 Lightweight (one tick handler, one HUD element, one boolean)
- 🚫 No mixins

## Usage

Press `K` to toggle Auto Sprint on or off.

When on, it sprints automatically while you hold forward, provided vanilla would allow sprinting at that moment.

Rebind the key here:

```text
Options → Controls → Mitra's Auto Sprinter
```

## How it works

Mitra's Auto Sprinter just holds the sprint key down for you that's it.
You'd get the exact same result by taping the key down or using vanilla's toggle-sprint option.
Because of that, vanilla's rules still fully apply.
It won't sprint while you're sneaking, eating, blinded, too hungry, elytra flying, and so on.
And if vanilla stops your sprint, say you run into a wall, it just stops, same as normal.
Nothing is forced, faked, or sent to the server, so there's nothing for anti-cheats to flag.

When something does block sprinting, the little HUD indicator tells you why.

## 📜 License

Licensed under **CC0 1.0 Universal**.

Do whatever you want with it. Seriously.

See [`LICENSE`](LICENSE) for the full license text.
