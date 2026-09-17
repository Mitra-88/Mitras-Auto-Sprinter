# Mitra's Auto Sprinter

<img src="images/preview.webp" width="768" height="432" alt="Preview">

Press **K** to turn on auto-sprint. Then just hold `W`, and you run without ever touching the sprint key. When something stops you from sprinting, the little HUD tells you why.

Also fixes [MC-263293](https://bugs.mojang.com/browse/MC/issues/MC-263293), where the game forgets your sprint toggle after dying or changing worlds.

## Quick start

1. Install [Fabric Loader](https://fabricmc.net/), [Fabric API](https://modrinth.com/mod/fabric-api), and [Fzzy Config](https://modrinth.com/mod/fzzy-config). [Mod Menu](https://modrinth.com/mod/modmenu) is optional but recommended, it's how you get to the settings.
2. Drop the mod's `.jar` into your `mods` folder.
3. Launch the game and join any world or server.
4. Press **K**, then hold `W`.

Press **K** again to turn it off. Your setting is remembered between sessions (it starts off the first time). Supported Minecraft versions are listed on the [Modrinth page](https://modrinth.com/mod/mitras-auto-sprinter) and the [GitHub releases](https://github.com/Mitra-88/Mitras-Auto-Sprinter).

## What you'll see

- **Sprint ON** (green): sprinting.
- **Sprint OFF** (gray): auto-sprint is off.
- **Sprint OFF (*reason*)** (yellow): auto-sprint is on, but something is blocking it: *Not Moving*, *Too Hungry*, *Sneaking*, *In Vehicle*, *Shallow Water*, *Using Item*, *Flying* (elytra), *Crawling*, *Hit Wall*, *Restricted*, or *Unknown Reason* on a Minecraft version this mod hasn't learned yet.
- **Joining... / Loading terrain...** (gray): shown briefly after joining or teleporting, until the world is ready.

Prefer no text? Icon mode replaces it with the speed-effect icon, bright while sprinting and faded when not.

By default, the HUD sits top-center under the boss bar. You can put it anywhere.

## Moving and resizing the HUD

Bind the **HUD editor** key in **Options → Controls → Mitra's Auto Sprinter** (unbound by default), or use the *Open HUD Editor* button in the config screen. Press it in-game, then:

- **Drag** the HUD to move it. It snaps to the screen edges, center lines, its default spot, and the safe area above the hotbar. Hold **Ctrl** to drag freely.
- **Mouse wheel** or **+** / **-** to resize.
- **Arrow keys** nudge it 1 pixel; **Shift + arrows** in bigger steps.
- **Double-click** to center it horizontally.
- **G** toggles an 8-pixel alignment grid, **R** resets the position.
- **ESC** saves everything.

## Settings

Most things live on the keybind and HUD editor above. For everything else: **Mod Menu → Mitra's Auto Sprinter → Configure**. There you'll find show/hide rules (always / only when blocked / flash on change), text vs icon mode, background, shadow, scales, solid / rainbow / chroma text colors, and the text of every label the HUD can show, blocked reasons included.

## Is it safe?

Yes. It's client-side only, nothing is ever sent to servers, and every vanilla sprint rule still applies. The plain-English explanation (and how to verify it yourself) is in [PROOF.md](PROOF.md).

## Links

- [Download on Modrinth](https://modrinth.com/mod/mitras-auto-sprinter)
- [Source code on GitHub](https://github.com/Mitra-88/Mitras-Auto-Sprinter)

## License

CC0 1.0. Do whatever you want with it. A shoutout is appreciated though (❁´◡`❁).

See the [LICENSE](LICENSE) file for the full text.
