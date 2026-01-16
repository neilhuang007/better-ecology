# Minecraft Game Design

**Jens Bergensten**

---

## Contents

* Introduction
* What Is Minecraft?
* Guiding Principles

---

## Introduction

Welcome! If you’re reading this, you probably work directly or indirectly with the Minecraft franchise. This booklet gives background into previous game design decisions, as well as some thoughts that influence future design. The principles and rules described here mostly refer to the vanilla game, but can also help to develop other Minecraft-related products.

This guide will be updated as our perspectives change alongside Minecraft’s growth. We have not set it in stone.

As a disclaimer, I want to call out that this is written from the first person **“I”** because it’s my interpretation of what Minecraft is. The intention is not to take credit for all things Minecraft, but to point out that these ideas are often subjective and up for debate. Game design is very driven by taste and individual experiences with games.

Notch and I worked together on Minecraft for about one year, and I’ve tried to recollect the things he mentioned as inspiration. He would cite games, what kind of gameplay features he liked, and set a direction for the Minecraft universe. He often mentioned *Dwarf Fortress*, *RollerCoaster Tycoon*, and *Dungeon Master*. The real genius was to combine this direction with the interaction model of *Infiminer*.

I’ve also worked on Minecraft at Mojang for many years and have been involved in many features and decisions over time.

— *Jens Bergensten*

---

## What Is Minecraft?

### Minecraft Is **Exploration**

Each Minecraft world is unique, and filled with thousands of things to find and explore. What’s behind that hill? What’s inside this house? Where can I find the best spot for my villa?

Curiosity is a strong motivation to play the game and push forward. The sense of infinity inspires players and boggles their imagination.

It doesn’t matter that most players never go far from the world’s starting point — just the idea of having no boundaries gives the feeling of grandeur.

We should strive towards rewarding players who explore without making it feel like a necessity. As I will return to later, finding things is fun; searching is not.

---

### Minecraft Is **Adventure**

As players explore, there’s always something at stake and rewards to reap. Craft your equipment to prepare for a journey, fight scary monsters, and collect loot. Explore dungeons and find treasure.

Dangers and hazards gate these rewards, but rewards unlock new functionality and boost the player’s ability to take on more challenges. This feedback loop lets players invent their own missions and quests, and provides an overarching goal of defeating the Ender Dragon.

We should keep giving players new challenges, but challenges should give adequate rewards. This balance is often hard to achieve.

---

### Minecraft Is **Creativity**

One of the fascinating things about Minecraft is how it inspires creativity. Once you learn how to create a small dirt house, you realize that you can scale to castles, bridges, great pyramids, or even modern-day imitations of airports or football stadiums.

Creativity is great for fulfillment and satisfaction, as well as creating a social environment for sharing and enjoying others’ work.

Creativity in Minecraft is not limited to construction but also encapsulates storytelling, role-playing, art, music, and much more. This platform for creativity fits nicely with today’s social media and “click-collecting” culture.

---

### Minecraft Is **Engineering**

Notch designed some systems in Minecraft specifically with engineering in mind, such as redstone and farming mechanics.

These systems create gameplay out of understanding how the game works. Other areas, such as the way monsters spawn at night, create ways for players to optimize their progress and “exploit” the game rules in clever ways.

We have since added more tools and mechanics for players to explore. Hoppers, slime blocks, and even the way chorus plants grow are just some examples.

We should remember that some people express their creativity in the form of logic and technology. Nothing beats automating the game.

---

### Minecraft Is **Persistent**

It’s easy to take this for granted, but every time a player removes a block, there’s a permanent mark on that world.

Blocks end up in the player’s inventory, and over time dozens of chests are filled with dirt and stone. Breaking block after block may sometimes feel like grinding, but earning minuscule drops appeals to the psychology of hoarding.

We need to remember that sometimes the journey is more interesting than the destination. A grand castle would not be the same if it appeared with the click of a button.

---

### Minecraft Is **Limitless**

There are no boundaries and there is no end to Minecraft. Your castle could always be more magnificent, the fields of wheat could reach beyond the horizon, and you could attempt to terraform the entire Nether.

Such aspirations may be over-ambitious but never pointless.

No one can rule how players choose to enjoy Minecraft. Strip mining for hours can be calming and meditative, and when you move on from that state, you may have created something awe-inspiring.

---

### Minecraft Is **Scary**

Digging your way to bedrock is often a tense experience. Not only are there monsters in the darkness, but lava pools, cave-ins, and spooky sound effects add to the suspense.

The danger of losing all your equipment in the void while building in the End is nerve-wracking. Not only do you need to worry about your own safety, but monsters also threaten things you’ve personally constructed.

We should remember to keep the players active and engaged. Indifference is a worse enemy than creepers.

---

### Minecraft Is **Simple**

The art style of Minecraft is a result of keeping things simple and giving the world a relatable personality. The blocky characters and low-resolution textures restrict expression, but big crossed eyes, cows looking at players dumbfounded, and the villagers’ bushy brows add to the unique feeling of the universe.

We sometimes refer to this as “ugly cute.”

We keep a low barrier for the community to reimagine textures and models. Anyone should feel that they can create their own animal or monster for the game.

At the same time, we should find out what makes a creature come to life. Such behaviors can be simple things like wolves tilting their heads, pandas rolling over, or the funny noises made by shulkers.

---

### Minecraft Is **Ridiculous**

Lots of the things featured in Minecraft are a bit silly. More often than not, a feature makes it into the game simply because it made us happy.

The irony face of the observer block, the Ender Dragon head, and the way beds look if you put them on a player’s head using commands.

Though it looks ridiculous on a surface level, the characters in Minecraft take things extremely seriously. The absurdity is similar to Monty Python humor: a serious problem does not care if it looks deeply silly when engaging a blaze, and villagers’ hums are real to them.

In a universe that can be scary and sometimes even morbid, we should allow ourselves to have some fun.

---

### Minecraft Is **Multiverse**

Each Minecraft world is not separated by space or time; they co-exist in their unique realities, and what happens in one does not affect another. If there is a better or older world, it would be something else than “vanilla” Minecraft.

This concept may open a few questions for our related projects. Is a story taking place in a specific world relevant to the multiverse of Minecraft? More specifically, is a spin-off game pretending to be real just an isolated event taking place in one of the many worlds?

In the core game, it is better to be unspecific and generic to fit all possibilities, but a static story can have more details, such as named characters and items.

---

## Guiding Principles

### One Block at a Time

The primary principle is that all actions interact with one block at a time. You break one block, pull one lever, and till one block of dirt.

There should not be any copy-paste functionality, building templates, or similar convenience tools. Interacting with a block may impact multitudes of other blocks — such as planting a tree, blowing up TNT, or activating redstone — but from the player’s perspective, it all starts with that singular interaction.

This allows observers to understand what is happening when watching other players. Instant building with templates would break this clarity.

---

### There Is No Steve or Alex

Steve and Alex are placeholders for players.

Players should make their own skins and tell their own stories. Players are playing themselves, or maybe a character that fits their unique world.

Steve and Alex are used as mascots in merchandise, but they should not have an active role in spin-off projects or storylines.

---

### Not Quite an RPG

Minecraft’s inspiration comes from fantasy role-playing games, but it is not a traditional RPG.

The player character does not develop skills over time. The only thing that defines what a player can do are the items they carry and their knowledge of the game.

Experience levels are not character progression but enchanting resources.

---

### But Things Happen… and It’s the Player’s Fault

Disasters are common in Minecraft: falling in lava, creepers blowing up houses.

These accidents happen because players were present to see them happen. Avoiding danger is done by playing differently and more carefully.

Game mechanics should not destroy hours of work without player involvement. When destruction happens, players should be able to stop or prevent it.

---

### New Features Must Respect Existing Ones

New features need to adapt to the balance of other features.

“Different” is more interesting than “better.” The End cities and ocean monuments feel distinct without invalidating one another.

---

### Items Should Be Multi-Purpose

Player inventories are already crowded.

If we add new items, we should consider whether they can have multiple uses or broad applications.

Sometimes the solution is decoration rather than function.

---

### It’s Up to the Players to Build the World

Villages do not expand or build themselves.

If villagers need protection, players must build it.

Minecraft provides a setting; players decide what happens.

---

### You’re Not Wearing a HUD

Information should be conveyed through in-world objects, not UI.

Maps, compasses, and clocks are good examples.

The F3 debug screen exposes engine internals and should not be required for gameplay.

---

### Finding Things Is Fun; Searching Is Not

Exploration should be rewarding but not frustrating.

Tools like exploration maps, Eyes of Ender, and bubble columns help guide players without removing discovery.

---

### Real-Life Animals Should Be Friendly

Animals should be friendly or neutral.

True danger is reserved for nocturnal monsters.

---

### Hostile Mobs Should Be Unique

Minecraft should build its own mythology.

Avoid generic fantasy monsters when possible.

---

### Mobs Need Personality

Visuals, sounds, and AI behaviors define how players relate to mobs.

Small behaviors make a big difference.

---

### Human Characters Are Human Beings

Only players look human.

Exceptions exist only for specific educational or narrative contexts.

---

### No Blood, No Gore

Violence is cartoonish.

Blood and gore are intentionally avoided.

---

### Keep It Vanilla

Minecraft supports many playstyles.

We should expand in all directions without forcing players into one experience.

---

### No Item Is Truly Unique

Named legendary items would connect worlds too strongly.

Each world should feel self-contained.

---

### Expand the Mysteries

Weird things don’t need explanations.

Mystery fuels imagination.

---

### Gender Neutrality Is a Core Principle

Creatures should be genderless unless explicitly necessary.

Players represent individuals and should be portrayed equally.

---

### Death Is Real

Hardcore mode should always be considered.

Gameplay should not rely on respawning.

---

### Bugs Are Not Features

Undefined behavior should not be relied upon.

If something matters, it should be intentional.

---

### Minecraft Is Not an Editor

Minecraft is a game about breaking blocks, finding treasure, and surviving nights.

Creative tools should never replace the core experience — even if an editor mode may exist someday.

