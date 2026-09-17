# AGENTS.md — Mitra's Auto Sprinter

## What this is

A **client-only Fabric mod** (mod id `mitrasautosprinter`, package `dev.mitra.client`) for **Minecraft 26.2**, Java **25**, Fabric Loader 0.19.3, Loom 1.17.20, Gradle 9.7.1. While toggled on (default key **K**) it holds the vanilla sprint key for you — equivalent to taping the key down or vanilla toggle-sprint — and draws a HUD that explains *why* you aren't sprinting (hunger, sneaking, wall, …). Also works around [MC-263293](https://bugs.mojang.com/browse/MC/issues/MC-263293) (sprint state forgotten after dying/changing worlds). Config UI is **Fzzy Config** 0.7.7+26.2 (ModMenu ≥20.0.2 is suggested but optional; Fzzy provides the screen). ~1228 lines of Java, CC0-1.0, published on Modrinth + GitHub releases.

**HARD REQUIREMENT — 100% client-side.** The mod's only "action" is calling `client.options.keySprint.setDown(true)` locally. It must never send packets or commands, never automate movement, and never be observable by the server in any way. All vanilla sprint restrictions still apply and are only *reported*, never bypassed. Push back on any request that would change this.

## Build & run

```bash
./gradlew build        # compile + jar (this is what CI runs, on Java 25)
./gradlew runClient    # launch a dev Minecraft instance with the mod
```

- No test sourceset exists; `./gradlew build` is the only verification gate — run it before finishing any change.
- Jar output: `build/libs/mitrasautosprinter-<version>-fabric-mc26.2.jar` (the `version` in `gradle.properties` is expanded into `fabric.mod.json` at process-resources time).
- CI (`.github/workflows/build.yml`) builds on every push to `master` (Zulu JDK 25) and, only while `releaseDevBuilds=true` in `gradle.properties`, recreates the `nightly` prerelease with generated notes + file hashes. Dependabot maintains the dependency versions.
- `run/` is a scratch Minecraft instance (gitignored) — don't treat its contents as source.

## How it works

1. **Sprint hold** — `AutoSprint` runs on both `START_CLIENT_TICK` and `END_CLIENT_TICK`; while `config.sprint.sprintEnabled` it holds `options.keySprint` down, and a `holdingSprintKey` latch releases it exactly once when disabled (so it never fights the player's physical sprint key). The toggle flips the config field and saves.
2. **Reasoning** — `SprintBlocker` (enum, 10 reasons) answers two questions: `blocking(player)` (can't *start* sprinting) and `whyNotSprinting(player)` (start-blocker, else the vanilla stop reason via `stopReason`, including the swim-sprint special cases). `HIT_WALL` is stop-only (`startBlocker=false`). This mirrors vanilla `LocalPlayer` sprint logic — keep it in sync with vanilla behavior, don't invent rules.
3. **HUD state** — `SprintHud.update()` (end of tick) picks text/color by state: ON (green) / OFF (gray) / blocked reason (yellow, wrapped in `textBlockedFormat`) / the configurable "Unknown Reason" label (yellow) when vanilla blocks sprint for a reason the mod doesn't model / settling (Joining…, Loading terrain…). `render` (render thread) draws it.
4. **World settling** — `WorldChangeDetector` arms a settle on player-instance change or a >16-block teleport jump, and clears it once no loading screen is open and the chunk at the player is loaded (100-tick cap). This is what makes sprint/HUD survive world changes (the MC-263293 workaround).
5. **HUD editor** — `HudEditorScreen`: drag with snapping (screen edges, center lines, default top, above the bottom HUD cluster, optional 8 px grid), arrow-key nudge, wheel/`+`/`-` scale, `R` reset, `G` grid toggle, `Ctrl` free-drag, double-click to center. Persists on close (`removed()`): anchor → `CUSTOM` plus normalized `hudX`/`hudY`.
6. **Config** — `MitrasConfig` (Fzzy Config) with Sprint / HUD / Text / Reasons sections; UI-gated options use `ValidatedCondition`.

## Codebase tour

All code lives in `src/client/java/dev/mitra/client/` (Loom `splitEnvironmentSourceSets()` — there is no `src/main`; everything is client-only).

**Root package**

- `MitrasAutoSprinterClient` — `ClientModInitializer`. Init order: `MitrasConfig.register()` → `new SprintHud` → `new AutoSprint` → sets the static `MitrasConfig.hudEditorAction` → registers events. Two keybinds in its own `KeyMapping.Category`: toggle (default K) and HUD editor (unbound), consumed in `END_CLIENT_TICK`. `AFTER_CLIENT_LEVEL_CHANGE` / `ClientPlayConnectionEvents.JOIN` arm the HUD settle. A `ScreenEvents.AFTER_INIT` hook keeps drawing the HUD over `LevelLoadingScreen`/`ProgressScreen` while settling.

**`sprint/`**

- `AutoSprint` — the sprint hold (above); also feeds `hud.update(client, enabled)` each tick.
- `SprintBlocker` — the reasoning engine (above); predicates are private static methods on the enum.

**`hud/`**

- `SprintHud` — the whole HUD: `HudElementRegistry.attachElementAfter(MISC_OVERLAYS, …)`, state machine in `update()`, drawing in `drawAt`/`drawAtConfiguredPosition`. Zero-allocation render path (recent perf work): a precomputed code-point glyph layout for rainbow/chroma (surrogate-pair safe — emoji in labels work), a 360-entry `HUE_LUT`, memoized label layout, and fixed-width centered text so the HUD doesn't jitter when labels change (re-measured at most 1×/s). One-strike fault guard: a render `Throwable` sets `renderBroken`, logs a warning, sends the player a chat message, and hides the HUD until the editor is opened or the world changes.
- `HudEditorScreen` — the editor (above). Drawing happens in `extractRenderState` (not `render`) and swallows HUD draw exceptions.
- `WorldChangeDetector` — settling (above); `Reason.JOINING` vs `LOADING_TERRAIN` is chosen by whether `client.player` exists yet.

**`config/`**

- `MitrasConfig` — Fzzy `Config` registered under `Identifier(namespace = mod id, path = mod id)` — this is why lang keys carry the doubled `mitrasautosprinter.mitrasautosprinter.` prefix. `MIN_SCALE`/`MAX_SCALE` (0.25–8) bound both scale sliders. The static `hudEditorAction` `Runnable` lets the config screen's "Open HUD Editor" button open the editor without the config class depending on Minecraft screens.
- Enums: `DisplayMode` (TEXT/ICON), `HudShowMode` (ALWAYS/BLOCKED_ONLY/ON_CHANGE), and `HudAnchor` (presets + CUSTOM) implement Fzzy `EnumTranslatable`; `TextColorMode` (SOLID/RAINBOW/CHROMA) is a plain enum.

**Resources** — `fabric.mod.json` (client entrypoint; depends fabric-api + fzzy_config; `environment: "client"`), `lang/en_us.json` (keys + `.desc` for every setting, enum-constant labels, keybinds, editor strings), `icon.png`. Player-facing docs live at the repo root: `README.md` (features, quick start, HUD reference, editor controls) and `PROOF.md` (plain-English safety explainer). When changing user-visible behavior (keybinds, labels, colors, editor controls, defaults), check both docs and the `.github/ISSUE_TEMPLATE/` placeholders for claims that go stale.

## Rules that matter for edits

- **Client-only contract:** never add networking, commands, or anything server-visible (see *What this is*).
- **No mixins.** The mod is pure Fabric API events; don't introduce a `mixins.json`.
- **Mappings:** Mojang official (`net.minecraft.resources.Identifier`, `GuiGraphicsExtractor`) — don't mix in Yarn names.
- **MC 26.x API shape:** HUD via `HudElementRegistry` + `extractRenderState`/`GuiGraphicsExtractor`; `Screen` input arrives as `KeyEvent`/`MouseButtonEvent` and rendering happens in `extractRenderState`; screens are read/set via `client.gui.screen()`/`setScreen`. Match the existing files — older tutorials won't apply.
- **Threading:** ticks run on the client thread, HUD drawing on the render thread, config-screen edits on the config screen's thread. Cross-thread updates flow through the `volatile` dirty flags (`labelsDirty`, `colorsStale`, set via Fzzy `listenToEntry`) — keep that pattern for new mutable config-derived state.
- **Never crash the game from rendering:** new HUD/editor draw code stays behind the existing try/catch + self-disable (`renderBroken`) pattern.
- **Lang completeness:** every new config setting, enum constant, keybind, or message needs its `en_us.json` key (plus `.desc` for settings), following the doubled-prefix scheme; an `EnumTranslatable.prefix()` must match its lang keys.
- **New reasons** need a `ReasonSection` field, a case in `SprintHud`'s `reasonLabel` switch (exhaustive with no `default` — javac fails the build if a `SprintBlocker` constant has no case), and a lang key + `.desc`. New text labels join the constructor's `listenToEntry` list; `fixedTextWidth()` picks up all labels automatically.
- **Commit style:** conventional commits with a scope, `!` for breaking changes (e.g. `feat(hud): …`, `perf(hud): …`).

## Gotchas & quirks

- `hudX`/`hudY` are normalized 0.0–1.0 over the *travel* (screen size minus element size), which keeps the position stable across GUI scales; convert only through `SprintHud.normalize/denormalize/clampToScreen`.
- Layout constants: `AUTO_CENTER_TOP_Y = 33` (below the boss bar) and `BOTTOM_RESERVED_HUD_HEIGHT = 50` (above the hotbar cluster) — the editor's snap targets use them too.
- Icon mode draws the speed-effect sprite (`Hud.getMobEffectSprite(MobEffects.SPEED)`) at alpha 0.35 when not sprinting; the background box is Text-mode-only (`hudBackground` is a `ValidatedCondition` gated on TEXT).
- ON_CHANGE show mode keeps the HUD visible for 60 ticks (3 s) after any label change.
- The editor persists position in `removed()` (ESC = save) and only when something actually moved/resized; `R` (reset) writes anchor `AUTO_CENTER_TOP` directly.
- The doubled `mitrasautosprinter.mitrasautosprinter.*` lang prefix is intentional (Fzzy id + config id), not a typo.
- The HUD element is suppressed while the editor screen is open or `renderBroken` is set.
- Java 25 idioms are in use (e.g. `_` unnamed lambda parameters) — fine to use.

## Where to look things up

- **`docs-for-agents/` (project root, gitignored)** — the local reference cache: clones of the Fabric and Fzzy Config repositories, ModMenu, and downloaded Fabric/Fzzy documentation. Reference material only, never part of the mod. Check here first for library/API questions; if something isn't there, fall back to the sources jars below.
- **Minecraft 26.2, Mojang mappings** — mapped jar: `C:\Users\Mitra\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar`. For full decompiled sources run `./gradlew genSources`, which writes `minecraft-clientOnly-…-26.2-sources.jar` and `minecraft-common-…-26.2-sources.jar` under the project's `.gradle/loom-cache/minecraftMaven/` (path contains a content hash, so glob for `*sources.jar`). The user-home cache `…\fabric-loom\decompile\v1.zip` also holds decompiled sources, but entries are keyed by content hash with a binary `LOOM NAME` header — you cannot look files up by class path; extract wholesale and grep by content if you must.
- **Library sources jars** under `C:\Users\Mitra\.gradle\caches\modules-2\files-2.1\<group>\<artifact>\<version>\<sha1>\` (grab the `*-sources.jar` in the `<sha1>` folder):
    - `me.fzzyhmstrs\fzzy_config\0.7.7+26.2\` — the config API (`ValidatedCondition`, `EnumTranslatable`, …). Fzzy Config is niche; its sources are more reliable than anything online.
    - `net.fabricmc.fabric-api\fabric-api\0.160.0+26.2\` — events, keymapping helpers, HUD registry.
- Official Fabric docs: https://docs.fabricmc.net/ (mostly pre-26.x; this repo's own code is the best 26.x API reference).

## RTK

RTK (`rtk`) is installed and available on PATH. Use RTK commands whenever an equivalent exists to reduce unnecessary CLI output and context usage.

### Rules

- Prefer `rtk` over the normal command when RTK provides an equivalent.
- Use the normal command when RTK does not provide an appropriate equivalent.
- Do not use RTK if the full/raw output is required for the task.
- Do not run both RTK and the normal command just to compare their output.
- RTK only filters/condenses output; it does not change the underlying command's intended behavior.
- If RTK hides information needed to continue, use `rtk recall` when applicable or run the normal command.

### Common replacements

- `ls` → `rtk ls`
- `tree` → `rtk tree`
- `cat` / file reading → `rtk read`
- `find` → `rtk find`
- `grep` → `rtk grep`
- `rg` → `rtk rg`
- `git ...` → `rtk git ...`
- `gh ...` → `rtk gh ...`
- `glab ...` → `rtk glab ...`
- `npm ...` → `rtk npm ...`
- `pnpm ...` → `rtk pnpm ...`
- `bun ...` → `rtk bun ...`
- `bunx ...` → `rtk bunx ...`
- `npx ...` → `rtk npx ...`
- `pytest ...` → `rtk pytest ...`
- `ruff ...` → `rtk ruff ...`
- `mypy ...` → `rtk mypy ...`
- `cargo ...` → `rtk cargo ...`
- `go ...` → `rtk go ...`
- `dotnet ...` → `rtk dotnet ...`
- `docker ...` → `rtk docker ...`
- `kubectl ...` → `rtk kubectl ...`
- `curl ...` → `rtk curl ...`
- `wget ...` → `rtk wget ...`

### Useful specialized commands

- Use `rtk test` when only test failures/results are needed.
- Use `rtk err` when only errors and warnings are relevant.
- Use `rtk diff` for a compact diff when the full diff is unnecessary.
- Use `rtk json` when inspecting JSON output.
- Use `rtk deps` when checking project dependencies.
- Use `rtk env` when inspecting environment variables.
- Use `rtk summary` or `rtk smart` when a concise command summary is useful.

### Important

Do not blindly replace every command with RTK. Choose the RTK equivalent only when its filtered output contains enough information to complete the task correctly.

When debugging, investigating unexpected behavior, or inspecting exact output, prefer the normal command if RTK's filtering could hide relevant information.

### On this machine

- ZCode's shell is **Git Bash** (win32), not PowerShell — invoke `rtk` normally, never `.\rtk.exe`.
- Installed at `C:\Program Files\rtk-x86_64-pc-windows-msvc\rtk.exe` and on the persisted user PATH.
- Shell env vars don't persist between Bash calls, so `export PATH=...` won't stick. If plain `rtk` isn't found (e.g. ZCode was launched before the PATH entry was added — inherited env is stale until ZCode restarts), call it by absolute path `"/c/Program Files/rtk-x86_64-pc-windows-msvc/rtk.exe"` or fall back to the normal command.
