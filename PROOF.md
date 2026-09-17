# Is this mod safe?

Hey, I made this mod. And since "trust me bro" isn't a real answer, here's exactly what it does, what it doesn't do, and how you can check my claims yourself. (If your brain can't stop overthinking just like me lol)

## "Will this get me banned?"

Short answer: no. Slightly longer answer: the mod never sends anything to any server, so there's nothing for an anticheat to see. When you sprint, Minecraft itself sends its normal "this player is sprinting" signal, the exact same one it sends when you press Ctrl yourself. The mod adds nothing on top. To a server, you're just a player holding Ctrl.

I mainly built this for Hypixel Skyblock, about as strict a server as they come, and it's built to stay on the right side of that line. But it works the same anywhere.

(Every server makes its own rules, obviously. But there's genuinely nothing hidden here, so keep reading.)

## What this mod actually does

It holds your sprint key down for you. That's it. That's the whole mod.

Picture taping Ctrl to your keyboard. Minecraft even ships its own "toggle sprint" option, same idea. This one just re-sprints you automatically and shows a little HUD telling you *why* you're not sprinting right now (like "Too Hungry").

## What it does NOT do

- It doesn't send anything to any server. Nothing. There is no network code in this mod at all. Unplug your internet, and it works exactly the same, that's how you know it can't be phoning home.
- It doesn't make you faster, doesn't touch your hunger or health, doesn't change your movement.
- It doesn't move your character. It can't walk, jump, or aim for you. It presses one key.
- It doesn't break a single Minecraft rule. Not even a little.

## Minecraft's rules still apply, all of them

You still stop sprinting when you're standing still, too hungry, in shallow water, eating or blocking, gliding, sneaking, crawling, riding something that can't sprint, the full normal list. The mod just tells you which one stopped you.

And the mod doesn't have its own version of the sprint rules. Every check is copied straight from Minecraft's own code, checked line by line against the game files. Same checks, same order. The mod reads the game's rules; it doesn't invent its own. So when Mojang changes how sprinting works in a future update, the mod automatically obeys the new rules, because it was never overriding anything.

And if an update ever adds something the mod doesn't recognize, the HUD straight up says **"Unknown Reason"** instead of faking a "Sprint ON". I'd rather it admit it doesn't know than lie to you.

## Don't trust me, check it yourself

Being skeptical of random mods is the right instinct. So:

- The mod is open source (CC0, read it, copy it, do whatever).
- It's tiny. The entire sprint logic is **one file**, around 150 lines: `src/client/java/dev/mitra/client/sprint/SprintBlocker.java`.
- Every check in that file is a copy of a check from the game's own player code. You can use [mcsrc.dev](https://mcsrc.dev/) and search for `net/minecraft/client/player/LocalPlayer`
