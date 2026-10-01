# AGENTS.md - Mitra's Auto Sprinter

## What this is

A **client-only Fabric mod** (mod id `mitrasautosprinter`, package `dev.mitra.client`) for **Minecraft 26.2**, Java **25**, Fabric Loader 0.19.3, Loom 1.17.20, Gradle 9.7.1. While toggled on (default key **K**) it holds the vanilla sprint key for you - equivalent to taping the key down or vanilla toggle-sprint - and draws a HUD that explains *why* you aren't sprinting (hunger, sneaking, wall, …). Also works around [MC-263293](https://bugs.mojang.com/browse/MC/issues/MC-263293) (sprint state forgotten after dying/changing worlds). Config UI is **Fzzy Config** 0.7.7+26.2 (ModMenu ≥20.0.2 is suggested but optional; Fzzy provides the screen). ~1228 lines of Java, CC0-1.0, published on Modrinth + GitHub releases.

**HARD REQUIREMENT - 100% client-side.** The mod's only "action" is calling `client.options.keySprint.setDown(true)` locally. It must never send packets or commands, never automate movement, and never be observable by the server in any way. All vanilla sprint restrictions still apply and are only *reported*, never bypassed. Push back on any request that would change this.

## Coding rules (mandatory: apply to every change)

**Purpose:** implement only what is genuinely necessary for the requested feature.

**Core rules**

- No overengineering.
- No unnecessary abstractions.
- No generic framework-like constructs when a simple, direct solution suffices.
- No "future-proofing" without a concrete need.
- No dead helper classes, wrappers, managers, registry layers, or utility collections without a clear current use case.
- No artificially bloated architectures.

**Style guidelines**

- Write simple, direct, readable code.
- Prefer concrete implementations over unnecessary generalization.
- Keep classes small and single-purpose.
- Keep methods short and clear.
- Use self-explanatory names instead of comments. **never add comments in code**, every rationale, invariant, and quirk lives in this file instead, so it has exactly one home and can't drift from the code. Don't re-add inline comments or apis/docs; put the knowledge here.
- Never use em dashes. In any file (docs, config comments, chat messages, code strings) write the sentence with commas, colons, or plain hyphens instead.

**What to avoid**

- AI-typical "enterprise" patterns for small features.
- Excessive use of interfaces without real added value.
- Builders, factories, services, providers, adapters, etc., unless actually needed.
- Defensive abstractions for hypothetical future use cases.
- Multi-layered architecture for trivial logic.
- Duplicated helper logic in "Utils" just to make code look "cleaner".
- Complex configuration or event systems for simple flows.

**Implementation principle, for every change:**

1. What is the specific requirement?
2. What is the smallest clean solution?
3. Implement exactly that, nothing beyond it.

**Refactoring** happens only for a real benefit: better readability, less duplication, clearer responsibilities, or a necessary technical fix, never purely stylistic preference.

**When in doubt, prefer:** less code, fewer files, less abstraction, less magic.

**Goal:** the code should feel pragmatically and deliberately written by an experienced developer, not like generic AI output.

## Ponytail, lazy senior dev mode

Adapted from [DietrichGebert/ponytail](https://github.com/DietrichGebert/ponytail/blob/main/AGENTS.md), commit `b6c04480c03e8db2f035751d7c46289779ec3362`.

You are a lazy senior developer. Lazy means efficient, not careless. The best code is the code never written.

Before writing any code, stop at the first rung that holds:

1. Does this need to be built at all? (YAGNI)
2. Does it already exist in this codebase? Reuse the helper, util, or pattern that's already here, don't re-write it.
3. Does the standard library already do this? Use it.
4. Does a native platform feature cover it? Use it.
5. Does an already-installed dependency solve it? Use it.
6. Can this be one line? Make it one line.
7. Only then: write the minimum code that works.

The ladder runs after you understand the problem, not instead of it: read the task and the code it touches, trace the real flow end to end, then climb.

Bug fix = root cause, not symptom: a report names a symptom. Grep every caller of the function you touch and fix the shared function once, one guard there is a smaller diff than one per caller, and patching only the path the ticket names leaves a sibling caller still broken.

Rules:

- No abstractions that weren't explicitly requested.
- No new dependency if it can be avoided.
- No boilerplate nobody asked for.
- Deletion over addition. Boring over clever. Fewest files possible.
- Shortest working diff wins, but only once you understand the problem. The smallest change in the wrong place isn't lazy, it's a second bug.
- Question complex requests: "Do you actually need X, or does Y cover it?"
- Pick the edge-case-correct option when two stdlib approaches are the same size, lazy means less code, not the flimsier algorithm.
- Mark deliberate simplifications that cut a real corner with a known ceiling (global lock, O(n²) scan, naive heuristic) with a `ponytail:` note naming the ceiling and upgrade path; in this repo that note lives here in AGENTS.md (next to the relevant section or in Gotchas & quirks).

Not lazy about: understanding the problem (read it fully and trace the real flow before picking a rung, a small diff you don't understand is just laziness dressed up as efficiency), input validation at trust boundaries, error handling that prevents data loss, security, accessibility, the calibration real hardware needs (the platform is never the spec ideal, a clock drifts, a sensor reads off), anything explicitly requested. Lazy code without its check is unfinished: non-trivial logic leaves ONE runnable check behind, the smallest thing that fails if the logic breaks (an assert-based demo/self-check or one small test file; no frameworks, no fixtures). Trivial one-liners need no test.

(Yes, this also applies to agents working on this repo. Especially to them.)

## Build & run

```bash
./gradlew build        # compile + jar (this is what CI runs, on Java 25)
./gradlew runClient    # launch a dev Minecraft instance with the mod
```

- No test sourceset exists; `./gradlew build` is the only verification gate - run it before finishing any change.
- Jar output: `build/libs/mitrasautosprinter-<version>-fabric-mc26.2.jar` (the `version` in `gradle.properties` is expanded into `fabric.mod.json` at process-resources time).
- CI (`.github/workflows/build.yml`) builds on every push to `master` (Zulu JDK 25) and, only while `releaseDevBuilds=true` in `gradle.properties`, recreates the `nightly` prerelease with generated notes + file hashes. Dependabot maintains the dependency versions.
- `run/` is a scratch Minecraft instance (gitignored) - don't treat its contents as source.
- `extractAgentSources` (in `build.gradle`) unpacks the `.java` and `.kt` sources of every resolved dependency plus the decompiled Minecraft sources into `agent_sources/src/` (gitignored) for plain-text reading. The jar list comes from an artifact view over the resolved compile classpath using `withVariantReselection()` (the Gradle 9.8 documented way to fetch `-sources` jars), so it always matches the exact versions pinned in `gradle.properties`, never anything newer; the same determinism Maven gives via `pom.xml`. A `.stamp` fingerprint (jar name + size) skips all disk writes while the jar set is unchanged, so changes to the extraction rules need one manual stamp deletion. Extraction is name-sorted `ZipFile` streaming with a zip-slip guard, and it is safe under the configuration cache because no project APIs are touched at execution time.

## How it works

1. **Sprint hold** - `AutoSprint` runs on both `START_CLIENT_TICK` and `END_CLIENT_TICK`; while `config.sprint.sprintEnabled` it holds `options.keySprint` down, and a `holdingSprintKey` latch releases it exactly once when disabled (so it never fights the player's physical sprint key). The toggle flips the config field and saves.
2. **Reasoning** - `SprintBlocker` (enum, 10 reasons) answers two questions: `blocking(player)` (can't *start* sprinting) and `whyNotSprinting(player)` (start-blocker, else the vanilla stop reason via `stopReason`, including the swim-sprint special cases). `HIT_WALL` is stop-only (`startBlocker=false`). This mirrors vanilla `LocalPlayer` sprint logic - keep it in sync with vanilla behavior, don't invent rules.
3. **HUD state** - `SprintHud.update()` (end of tick) picks text/color by state: ON (green) / OFF (gray) / blocked reason (yellow, wrapped in `textBlockedFormat`) / the configurable "Unknown Reason" label (yellow) when vanilla blocks sprint for a reason the mod doesn't model / settling (Joining…, Loading terrain…). `render` (render thread) draws it.
4. **World settling** - `WorldChangeDetector` arms a settle on player-instance change or a >16-block teleport jump, and clears it once no loading screen is open and the chunk at the player is loaded (100-tick cap). This is what makes sprint/HUD survive world changes (the MC-263293 workaround).
5. **HUD editor** - `HudEditorScreen`: drag with snapping (screen edges, center lines, default top, above the bottom HUD cluster, optional 8 px grid), arrow-key nudge, wheel/`+`/`-` scale, `R` reset, `G` grid toggle, `Ctrl` free-drag, double-click to center. Persists on close (`removed()`): anchor → `CUSTOM` plus normalized `hudX`/`hudY`.
6. **Config** - `MitrasConfig` (Fzzy Config) with Sprint / HUD / Text / Reasons sections; UI-gated options use `ValidatedCondition`.

## Codebase tour

All code lives in `src/client/java/dev/mitra/client/` (Loom `splitEnvironmentSourceSets()` - there is no `src/main`; everything is client-only).

**Root package**

- `MitrasAutoSprinterClient` - `ClientModInitializer`. Init order: `MitrasConfig.register()` → `new SprintHud` → `new AutoSprint` → sets the static `MitrasConfig.hudEditorAction` → registers events. Two keybinds in its own `KeyMapping.Category`: toggle (default K) and HUD editor (unbound), consumed in `END_CLIENT_TICK`. `AFTER_CLIENT_LEVEL_CHANGE` / `ClientPlayConnectionEvents.JOIN` arm the HUD settle. A `ScreenEvents.AFTER_INIT` hook keeps drawing the HUD over `LevelLoadingScreen`/`ProgressScreen` while settling.

**`sprint/`**

- `AutoSprint` - the sprint hold (above); also feeds `hud.update(client, enabled)` each tick.
- `SprintBlocker` - the reasoning engine (above); predicates are private static methods on the enum.

**`hud/`**

- `SprintHud` - the whole HUD: `HudElementRegistry.attachElementAfter(MISC_OVERLAYS, …)`, state machine in `update()`, drawing in `drawAt`/`drawAtConfiguredPosition`. Zero-allocation render path (recent perf work): a precomputed code-point glyph layout for rainbow/chroma (surrogate-pair safe - emoji in labels work), a 360-entry `HUE_LUT`, memoized label layout, and fixed-width centered text so the HUD doesn't jitter when labels change (re-measured at most 1×/s). One-strike fault guard: a render `Throwable` sets `renderBroken`, logs a warning, sends the player a chat message, and hides the HUD until the editor is opened or the world changes.
- `HudEditorScreen` - the editor (above). Drawing happens in `extractRenderState` (not `render`) and swallows HUD draw exceptions.
- `WorldChangeDetector` - settling (above); `Reason.JOINING` vs `LOADING_TERRAIN` is chosen by whether `client.player` exists yet.

**`config/`**

- `MitrasConfig` - Fzzy `Config` registered under `Identifier(namespace = mod id, path = mod id)` - this is why lang keys carry the doubled `mitrasautosprinter.mitrasautosprinter.` prefix. `MIN_SCALE`/`MAX_SCALE` (0.25-8) bound both scale sliders. The static `hudEditorAction` `Runnable` lets the config screen's "Open HUD Editor" button open the editor without the config class depending on Minecraft screens.
- Enums: `DisplayMode` (TEXT/ICON), `HudShowMode` (ALWAYS/BLOCKED_ONLY/ON_CHANGE), and `HudAnchor` (presets + CUSTOM) implement Fzzy `EnumTranslatable`; `TextColorMode` (SOLID/RAINBOW/CHROMA) is a plain enum.

**Resources** - `fabric.mod.json` (client entrypoint; depends fabric-api + fzzy_config; `environment: "client"`), `lang/en_us.json` (keys + `.desc` for every setting, enum-constant labels, keybinds, editor strings), `icon.png`. Player-facing docs live at the repo root: `README.md` (features, quick start, HUD reference, editor controls) and `PROOF.md` (plain-English safety explainer). When changing user-visible behavior (keybinds, labels, colors, editor controls, defaults), check both docs and the `.github/ISSUE_TEMPLATE/` placeholders for claims that go stale.

## Rules that matter for edits

- **Client-only contract:** never add networking, commands, or anything server-visible (see *What this is*).
- **No mixins.** The mod is pure Fabric API events; don't introduce a `mixins.json`.
- **Mappings:** Mojang official (`net.minecraft.resources.Identifier`, `GuiGraphicsExtractor`) - don't mix in Yarn names.
- **MC 26.x API shape:** HUD via `HudElementRegistry` + `extractRenderState`/`GuiGraphicsExtractor`; `Screen` input arrives as `KeyEvent`/`MouseButtonEvent` and rendering happens in `extractRenderState`; screens are read/set via `client.gui.screen()`/`setScreen`. Match the existing files - older tutorials won't apply.
- **Threading:** ticks run on the client thread, HUD drawing on the render thread, config-screen edits on the config screen's thread. Cross-thread updates flow through the `volatile` dirty flags (`labelsDirty`, `colorsStale`, set via Fzzy `listenToEntry`) - keep that pattern for new mutable config-derived state.
- **Never crash the game from rendering:** new HUD/editor draw code stays behind the existing try/catch + self-disable (`renderBroken`) pattern.
- **Lang completeness:** every new config setting, enum constant, keybind, or message needs its `en_us.json` key (plus `.desc` for settings), following the doubled-prefix scheme; an `EnumTranslatable.prefix()` must match its lang keys.
- **New reasons** need a `ReasonSection` field, a case in `SprintHud`'s `reasonLabel` switch (exhaustive with no `default` - javac fails the build if a `SprintBlocker` constant has no case), and a lang key + `.desc`. New text labels join the constructor's `listenToEntry` list; `fixedTextWidth()` picks up all labels automatically.
- **Commit style:** conventional commits with a scope, `!` for breaking changes (e.g. `feat(hud): …`, `perf(hud): …`).

## Gotchas & quirks

- `hudX`/`hudY` are normalized 0.0-1.0 over the *travel* (screen size minus element size), which keeps the position stable across GUI scales; convert only through `SprintHud.normalize/denormalize/clampToScreen`.
- Layout constants: `AUTO_CENTER_TOP_Y = 33` (below the boss bar) and `BOTTOM_RESERVED_HUD_HEIGHT = 50` (above the hotbar cluster) - the editor's snap targets use them too.
- Icon mode draws the speed-effect sprite (`Hud.getMobEffectSprite(MobEffects.SPEED)`) at alpha 0.35 when not sprinting; the background box is Text-mode-only (`hudBackground` is a `ValidatedCondition` gated on TEXT).
- ON_CHANGE show mode keeps the HUD visible for 60 ticks (3 s) after any label change.
- The editor persists position in `removed()` (ESC = save) and only when something actually moved/resized; `R` (reset) writes anchor `AUTO_CENTER_TOP` directly.
- The doubled `mitrasautosprinter.mitrasautosprinter.*` lang prefix is intentional (Fzzy id + config id), not a typo.
- The HUD element is suppressed while the editor screen is open or `renderBroken` is set.
- Java 25 idioms are in use (e.g. `_` unnamed lambda parameters) - fine to use.

## Where to look things up

- **`docs-for-agents/` (project root, gitignored)** - the local reference cache: clones of the Fabric and Fzzy Config repositories, ModMenu, and downloaded Fabric/Fzzy documentation. Reference material only, never part of the mod. Check here first for library/API questions; if something isn't there, fall back to the sources jars below.
- **Minecraft 26.2, Mojang mappings** - mapped jar: `C:\Users\Mitra\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar`. For full decompiled sources run `./gradlew genSources`, which writes `minecraft-clientOnly-…-26.2-sources.jar` and `minecraft-common-…-26.2-sources.jar` under the project's `.gradle/loom-cache/minecraftMaven/` (path contains a content hash, so glob for `*sources.jar`). The user-home cache `…\fabric-loom\decompile\v1.zip` also holds decompiled sources, but entries are keyed by content hash with a binary `LOOM NAME` header - you cannot look files up by class path; extract wholesale and grep by content if you must.
- **Library sources jars** under `C:\Users\Mitra\.gradle\caches\modules-2\files-2.1\<group>\<artifact>\<version>\<sha1>\` (grab the `*-sources.jar` in the `<sha1>` folder):
    - `me.fzzyhmstrs\fzzy_config\0.7.7+26.2\` - the config API (`ValidatedCondition`, `EnumTranslatable`, …). Fzzy Config is niche; its sources are more reliable than anything online.
    - `net.fabricmc.fabric-api\fabric-api\0.161.0+26.2\` - events, keymapping helpers, HUD registry.

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

Files and search:
- `ls` → `rtk ls`
- `tree` → `rtk tree`
- `cat` / file reading → `rtk read`
- `find` → `rtk find`
- `grep` → `rtk grep`
- `rg` → `rtk rg`
- `ast-grep` → `rtk ast-grep`
- `wc` → `rtk wc`
- `diff` → `rtk diff`

Git and code hosting:
- `git ...` → `rtk git ...`
- `gh ...` → `rtk gh ...`
- `glab ...` → `rtk glab ...`
- `gt ...` → `rtk gt ...`

JavaScript / TypeScript:
- `npm ...` → `rtk npm ...`
- `pnpm ...` → `rtk pnpm ...`
- `bun ...` → `rtk bun ...`
- `bunx ...` → `rtk bunx ...`
- `npx ...` → `rtk npx ...`
- `deno ...` → `rtk deno ...`
- `tsc ...` → `rtk tsc ...`
- `eslint ...` → `rtk lint ...`
- `prettier ...` → `rtk prettier ...`
- `next build` → `rtk next build`
- `jest ...` → `rtk jest ...`
- `vitest ...` → `rtk vitest ...`
- `playwright ...` → `rtk playwright ...`
- `prisma ...` → `rtk prisma ...`

Python:
- `pytest ...` → `rtk pytest ...`
- `ruff ...` → `rtk ruff ...`
- `mypy ...` → `rtk mypy ...`
- `pip ...` → `rtk pip ...`
- `uv run ...` → `rtk uv run ...`

Other languages and build tools:
- `cargo ...` → `rtk cargo ...`
- `go ...` → `rtk go ...`
- `golangci-lint ...` → `rtk golangci-lint ...`
- `dotnet ...` → `rtk dotnet ...`
- `ctest ...` → `rtk ctest ...`
- `mvn ...` / `mvnd ...` → `rtk mvn ...` / `rtk mvnd ...`
- `gradlew ...` → `rtk gradlew ...`
- `sbt ...` → `rtk sbt ...`
- `php artisan ...` → `rtk php ...`
- `phpunit` / `pest` / `paratest` / `phpstan` → `rtk phpunit` / `rtk pest` / `rtk paratest` / `rtk phpstan`
- `rake test` / `rspec` / `rubocop` → `rtk rake` / `rtk rspec` / `rtk rubocop`

Infrastructure, data, and network:
- `docker ...` → `rtk docker ...`
- `kubectl ...` → `rtk kubectl ...`
- `oc ...` → `rtk oc ...`
- `aws ...` → `rtk aws ...`
- `psql ...` → `rtk psql ...`
- `sqlfluff ...` → `rtk sqlfluff ...`
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
