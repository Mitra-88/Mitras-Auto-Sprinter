# Mitra's Auto Sprinter

**A simple client-side Auto Sprint mod for Minecraft Fabric 26.2.**

Mitra's Auto Sprinter automatically starts sprinting when you're moving forward, as long as vanilla Minecraft would normally allow you to sprint.

Minimal. Lightweight. Does one thing.

---

## ✨ Features

* 🏃 Automatically starts sprinting while moving forward
* ⌨️ Toggle Auto Sprint with a keybind
* 🔧 Default toggle key: `K`
* 🎮 Keybind is fully rebindable in Minecraft Controls
* 🟢 Small `Sprint ON` HUD indicator
* 🧠 Respects vanilla sprinting restrictions
* 📦 Completely client-side
* ⚡ Extremely lightweight
* 🚫 No mixins

---

## 📥 Installation

1. Install [**Fabric Loader**](https://fabricmc.net/use/installer/) for Minecraft `26.2`.
2. Download and install [**Fabric API**](https://modrinth.com/mod/fabric-api/version/0.159.0+26.2) for Minecraft `26.2`.
3. Download [**Mitra's Auto Sprinter**](https://github.com/Mitra-88/Mitras-Auto-Sprinter/releases/tag/v1.0.0).
4. Put both mod `.jar` files into your Minecraft `mods` folder.
5. Launch Minecraft.

Your folder should look something like this:

```text
.minecraft/
└── mods/
    ├── fabric-api-<version>.jar
    └── mitras-auto-sprinter-<version>-<mc-verison>.jar
```

---

## 🎮 Usage

Press **`K`** to toggle Auto Sprint.

When enabled, the mod automatically starts sprinting whenever you're holding the forward key and vanilla Minecraft allows sprinting.

The key can be changed here:

```text
Options → Controls → Mitra's Auto Sprinter
```

---

## 🧠 Vanilla-friendly behavior

Mitra's Auto Sprinter **doesn't force sprinting**.

It only attempts to start sprinting when vanilla Minecraft would normally allow it.

Auto Sprint will not start while:

* Sneaking
* Using an item
* Elytra flying
* Too hungry to sprint
* Blinded

The mod also **never forces sprinting to stay enabled**.

Vanilla Minecraft still has full control over stopping sprinting because of collisions, hunger, movement changes, or other normal game mechanics.

In practice, it's basically like holding the vanilla sprint key—except your finger gets to chill.

---

## 🟢 HUD

When Auto Sprint is enabled, you'll see:

> **Sprint ON**

in the top-right corner of your screen.

The indicator automatically hides when the vanilla HUD is hidden with `F1`.

---

## ⚡ Lightweight by design

This mod intentionally does almost nothing besides Auto Sprint.

It uses:

* One client tick handler
* One HUD element
* One mutable boolean

When Auto Sprint is disabled, the mod has virtually nothing to do.

---

## 🌐 Multiplayer

Sprinting is handled through Minecraft's normal client behavior.

That said, **always follow the rules of the server you're playing on**.

---

## 📜 License

Licensed under **CC0 1.0 Universal**.

Do whatever you want with it. Seriously.

See [`LICENSE`](LICENSE) for the full license text.

---

<div style="text-align: center">

# 🏃 Mitra's Auto Sprinter

**You hold W. The mod handles the sprinting.**

Made for **Minecraft Fabric 26.2**.

</div>
