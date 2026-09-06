# Mitra's Auto Sprinter

<img src="images/preview.png" width="960" height="540" alt="Preview">

**TL;DR: Press K once to toggle sprint on or off.** If you can't sprint, the screen shows you why. It's safe for multiplayer servers because it just holds the sprint key for you. That's it.

## Why?

I made this because I couldn't find an auto sprint mod I trusted, and I wanted an indicator that tells me why I'm not sprinting so my brain could finally stop overthinking it. also fixes [MC-263293 (Dying or world change causes toggle sprint to untoggle)](https://bugs.mojang.com/browse/MC/issues/MC-263293)

## Features

- 🏃 Sprint automatically whenever you move forward
- ⌨️ Toggle it with `K` (rebindable, obviously)
- 📊 A little HUD that shows if you're sprinting and *why not* when you aren't
- 🖱️ Drag the HUD anywhere on screen with a built-in editor
- 🎨 Change the HUD text and colors
- 💻 Client-side only
- ⚡ Tiny and lightweight

## What you need

[Fabric Loader](https://fabricmc.net/) + [Fabric API](https://modrinth.com/mod/fabric-api). For the supported Minecraft versions, check the [Modrinth page](https://modrinth.com/mod/mitras-auto-sprinter) or the [latest release on GitHub](https://github.com/Mitra-88/Mitras-Auto-Sprinter) both always show the current one.

## How to use

1. Drop the jar in your `mods` folder
2. Join a world or server
3. Press **K**

Now hold `W` and run. Press **K** again to turn it off.

The mod remembers your choice between restarts, but it starts *off* the first time you play so if nothing happens at first, just press K. Don't panic.

Keybinds live in the usual spot: **Options → Controls → Mitra's Auto Sprinter**.

### The HUD

While you play, a small indicator shows what's going on:

- **Sprint ON** (green) - you're running
- **Sprint OFF - Too Hungry** (yellow) - you *would* be sprinting, but something's in the way. It tells you what: *Hit Wall*, *Using Item*, *Blindness*, and so on. All the normal vanilla stuff.
- **Joining... / Loading terrain...** (gray) - you just joined or warped somewhere new. It disappears the moment the world is actually ready, not a fake timer.
- **Sprint OFF** (gray) - the mod is off

By, default it sits centered at the top, just below the boss bar.

### Moving the HUD

Don't like where it sits? Fair.

1. In **Options → Controls → Mitra's Auto Sprinter**, set a key for **Open HUD Editor** (it's unbound by default)
2. Press it in game
3. Drag the HUD wherever you want - it can't go off-screen
4. Press **ESC** to save

Tip: press **R** while your mouse is over the HUD to snap it back to its default spot.

In icon mode you can also resize it right in the editor: scroll your mouse wheel over the HUD (or press `+` / `-` on the keyboard, numpad works too) and it scales up and down live.

## Is this safe on servers?

YES! The mod just holds your sprint key down for you automatically that's really all it's doing. It's the same as taping the key to your keyboard, or using vanilla's own toggle-sprint option. Nothing fancy going on under the hood.

Because it's just automating a normal keypress, all of Minecraft's usual sprint rules still apply exactly like they would if you were holding the key yourself. It won't start sprinting while you're sneaking, eating, flying with an elytra, or doing anything else the game normally blocks sprinting for. Walk into a wall? Sprint stops, just like always.

So really, it's not doing anything the game doesn't already allow it's just saving your finger the trouble.

## Config (optional)

You never *have* to touch this - the keybind and HUD editor cover everything you normally need. But if you like tinkering, everything lives in `config/mitrasautosprinter.properties`. It shows up after your first toggle.

**Everything hot-reloads**: save the file while playing, and it applies within a second - text, colors, position, toggles, all of it. No restart needed.

- `sprintEnabled` - whether the mod starts on or off
- `hudVisible` / `hudBackground` - hide the HUD or its background box
- `displayMode` - what the HUD shows: `text` (the label, default) or `icon` (the speed effect icon - full color while sprinting, faded when not, and it follows your resource packs)
- `hudIconScale` - icon size multiplier when `displayMode` is `icon` (0.25 to 8; resize it live in the editor with the scroll wheel or `+` / `-`)
- `hudX` / `hudY` - HUD position. By, default the HUD is centered at the top, just below the boss bar, and stays centered at any GUI scale or resolution. Set `hudX` or `hudY` to `-1` to auto-center that axis, or use the editor to place it freely
- `hudColorOn` / `hudColorBlocked` / `hudColorOff` / `hudBackgroundColor` - the colors
- `textOn` / `textOff` / `textBlockedFormat` / `textJoining` / `textTerrain` - the HUD text (`%s` gets replaced with the reason; the joining and terrain labels show while a world or server is loading, and disappear as soon as it is ready)
- `reasonHungry`, `reasonBlind`, etc. - rename each "why not" message

**Colors are just normal hex codes** - like `#55FF55`. Grab one from any color picker website and paste it in.

## Links

- 📥 [Download on Modrinth](https://modrinth.com/mod/mitras-auto-sprinter)
- 💻 [Source code on GitHub](https://github.com/Mitra-88/Mitras-Auto-Sprinter)

## License

CC0 1.0. do whatever you want with it, no credit needed. A shoutout would be pretty cool tho.
See [`LICENSE`](LICENSE) for the full legal text.
