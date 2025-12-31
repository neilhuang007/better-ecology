# Minecraft Game Design: Summary and Vanilla Mod Design Guide

**A comprehensive guide based on Jens Bergensten's internal Mojang design document and developer insights**

---

## PART 1: THE MOST CRITICAL DESIGN PRINCIPLES

These are the non-negotiable rules that define what makes Minecraft feel like Minecraft.

---

### 1. ONE BLOCK AT A TIME

**THIS IS THE SINGLE MOST IMPORTANT PRINCIPLE IN ALL OF MINECRAFT DESIGN.**

All player actions interact with one block at a time. You break one block, pull one lever, till one block of dirt.

**WHAT THIS MEANS:**
- **NO COPY-PASTE FUNCTIONALITY**
- **NO BUILDING TEMPLATES**
- **NO CONVENIENCE TOOLS THAT PLACE MULTIPLE BLOCKS**

Interacting with a block may impact many other blocks (planting a tree, exploding TNT, activating redstone), but from the player's perspective, it all starts with that singular interaction.

**WHY THIS MATTERS:**
This principle maintains the core interaction model and sets essential constraints for multiplayer. A player watching someone else build can understand what is happening and assist. Instantaneous template-based building would make it impossible for observers to help or learn.

**EXAMPLE OF VIOLATION:**
A mod that lets you select an area and fill it with blocks instantly. This breaks the fundamental interaction model even if it seems like a quality-of-life improvement.

**EXAMPLE OF COMPLIANCE:**
The Redstone system. One lever activates a complex network, but the player only interacted with one block. The complexity emerges from the system, not from bypassing the one-block rule.

---

### 2. BAD THINGS HAPPEN, BUT IT IS TECHNICALLY THE PLAYER'S FAULT

Disaster is common in Minecraft. Falling in lava, house blown up by creepers, losing items in the void. **THE KEY REQUIREMENT: THESE ACCIDENTS MUST HAPPEN WHEN PLAYERS ARE PRESENT TO SEE THEM.**

**THE CORE RULES:**
- **THE GAME SHOULD NEVER CREATE SITUATIONS THAT ARE IMPOSSIBLE TO AVOID**
- **WHEN UNAVOIDABLE EVENTS OCCUR, THEY MUST HAVE REASONABLY SMALL IMPACT**
- **IF A MECHANIC DESTROYS PLAYER WORK, PLAYERS MUST BE THE ONES INITIATING IT**
- **PLAYERS MUST BE ABLE TO STOP OR PREVENT DESTRUCTION ENTIRELY**

**EXAMPLES OF THIS PRINCIPLE IN ACTION:**

The worst natural disaster in vanilla Minecraft is a lightning strike, which is relatively tame.

Endermen can only pick up natural, plentiful blocks like dirt and stone. They cannot grab player-placed building materials.

**Iron golems ignore creepers.** If golems attacked creepers, the creeper would explode and destroy buildings without any player present. This would violate the principle.

**EXAMPLE FOR MODDERS:**
If you add a mob that destroys blocks, ensure the player must be nearby for it to happen. Never let destruction occur in unloaded chunks or when the player is away.

---

### 3. DEATH IS REAL

**ALL FEATURES MUST BE DESIGNED WITH HARDCORE DIFFICULTY IN MIND.**

Death and demise are permanent in Hardcore mode. No gameplay mechanic should rely on respawning.

**EXAMPLE OF VIOLATION:**
Mojang rejected an achievement for "dying spectacularly" because it cannot be earned in Hardcore without ending the entire run.

**EXAMPLE FOR MODDERS:**
If your mod includes a mechanic that requires the player to die and respawn to progress, it breaks this principle. Redesign it so death is never required.

---

### 4. IT IS UP TO THE PLAYERS TO BUILD THE WORLD

When villages were added in beta, Mojang made a conscious decision: **VILLAGERS WILL NOT BUILD HOUSES. THERE ARE NO MECHANICS FOR AUTOMATIC VILLAGE EXPANSION.**

If a village needs a protective wall, the player must construct it.

**THE PRINCIPLE:**
Minecraft provides a setting for players to interact with. **PLAYERS DECIDE WHAT, WHEN, AND WHERE DEVELOPMENT HAPPENS.** The game does not build for them.

**EXAMPLE FOR MODDERS:**
Avoid mods where NPCs automatically construct buildings or expand settlements without player involvement. The player should always be the one placing blocks and making decisions about the world.

---

## PART 2: PLAYER CHARACTER AND IDENTITY RULES

---

### 5. THERE IS NO STEVE OR ALEX

Steve and Alex are placeholder skins. **THEY ARE NOT CHARACTERS WITH PERSONALITIES OR STORIES.**

Players should make their own skins and create their own narratives. Players are playing themselves or a character that fits their unique world.

**THE RULES:**
- Steve and Alex can appear in merchandise as mascots
- **STEVE AND ALEX MUST NOT HAVE ACTIVE ROLES IN SPIN-OFF PROJECTS OR STORYLINES**
- Players represent themselves, not pre-defined characters

**EXAMPLE:**
In Minecraft: Story Mode, all characters are framed as "players on a server" rather than giving Steve or Alex canonical personalities.

---

### 6. HUMAN CHARACTERS ARE HUMAN BEINGS

**ONLY REAL PLAYERS MAY LOOK HUMAN.**

There are no human NPCs in Minecraft. Villagers have distinctive non-human features (large noses). All non-player characters must be visually distinct from player models.

**EXCEPTIONS:**
- Minecraft: Education Edition added human-looking villager skins for tutorial NPCs
- Minecraft: Story Mode treats all characters as "players on a server"

**EXAMPLE FOR MODDERS:**
If you add NPCs, give them non-human features. Large heads, unusual proportions, distinctive facial features, or non-human skin colors. Never add NPCs that look like default player skins.

---

### 7. NOT QUITE AN RPG

Minecraft evolved from games like Dwarf Fortress and Dungeon Master toward fantasy RPG elements. **BUT MINECRAFT IS NOT A TRADITIONAL RPG.**

**THE KEY DISTINCTION:**
**PLAYER CHARACTERS DO NOT DEVELOP SKILLS OVER TIME.**

The only things that define what a player can do are:
1. The items they carry
2. Their knowledge of the game

**COMMON MISCONCEPTION:**
The experience levels players earn from killing monsters or trading are **NOT EXPERIENCE LEVELS IN THE RPG SENSE.** They are enchanting levels, a currency spent to improve equipment. The player character does not become inherently stronger.

**EXAMPLE FOR MODDERS:**
Avoid adding skill trees, permanent stat upgrades, or abilities that increase as the player plays. If you want progression, tie it to items and equipment, not character stats.

---

## PART 3: CONTENT DESIGN PRINCIPLES

---

### 8. NEW FEATURES MUST BE RESPECTFUL OF EXISTING ONES

New content must adapt to the balance of existing features.

**THE CORE RULE:**
**"DIFFERENT" IS MORE INTERESTING THAN "BETTER"**

This applies to both gameplay mechanics and power metrics.

**EXAMPLE:**
End cities and ocean monuments offer completely different moods and gameplay challenges. Neither invalidates the other. They provide variety, not power creep.

**EXAMPLE FOR MODDERS:**
If vanilla has a sword that deals 7 damage, do not add a sword that deals 14 damage. Instead, add a weapon with different properties: faster attack speed, special effects, situational advantages. Make players choose based on situation, not raw power.

---

### 9. ITEMS SHOULD BE MULTI-PURPOSE

Player inventories are already crowded with hundreds of items. **WHEN ADDING NEW ITEMS, CONSIDER WHETHER THEY CAN SERVE MULTIPLE PURPOSES OR HAVE BROADER USE-CASES.**

**ACCEPTABLE SINGLE-USE ITEMS:**
The totem of undying has only one function, but its gameplay impact is significant enough to justify the inventory slot.

**SOLUTIONS FOR NEW DROPS:**
- Make the item a potion-brewing ingredient
- Allow it to craft into decoration blocks
- Give it multiple functions across different systems

**EXAMPLE FOR MODDERS:**
If a new mob drops a unique item, do not make it craft into just one thing. Let it work as a brewing ingredient, a decoration component, and perhaps a fuel source or trading item.

---

### 10. FINDING THINGS IS FUN, SEARCHING FOR THEM IS NOT

The Minecraft world is infinite. Everything that can exist does exist somewhere. **THE PROBLEM IS HOW TO FIND WHAT YOU WANT.**

Randomly walking around looking for new content is frustrating.

**THE PRINCIPLE:**
Reward exploration, but give players reasonable ways to find things. **DO NOT MAKE FINDING CONTENT FEEL LIKE A NECESSITY OR A CHORE.**

**EXAMPLES OF GOOD DISCOVERY MECHANICS:**
- Exploration maps that lead to woodland mansions
- Eyes of ender that guide to strongholds
- Bubble columns visible from the surface that indicate underwater ruins

**EXAMPLE FOR MODDERS:**
If you add a rare structure, include a way to locate it. A craftable compass, a villager trade for a map, or visual indicators in the world. Do not force players to wander aimlessly.

---

### 11. NO ITEM IS TRULY UNIQUE

Each Minecraft world exists in a multiverse where all possibilities coexist. **ADDING NAMED UNIQUE ITEMS CREATES UNWANTED CONNECTIONS BETWEEN WORLDS.**

**WHAT TO AVOID:**
Named weapons like a crossbow called "Heartseeker" or a sword called "Dragonbane." These imply a shared history across all worlds, which undermines the multiverse concept.

**THE PRINCIPLE:**
Every world and player experience should feel special and self-contained. Items should be types, not individuals.

**EXAMPLE FOR MODDERS:**
Instead of adding "The Blade of the Ancient King," add "Ancient Blade" as a weapon type that can appear in multiple worlds without implying they are the same item.

---

### 12. EXPAND THE MYSTERIES

Minecraft contains many weird and strange things that do not make logical sense. **IT IS NOT THE DEVELOPER'S JOB TO EXPLAIN THEM.**

Why are there monsters at night? What are the illagers? Who built the stronghold?

**THE PRINCIPLE:**
These questions can start player stories. **EVEN IF DEVELOPERS HAVE THEIR OWN EXPLANATIONS, THEY SHOULD KEEP THEM PRIVATE.**

**EXAMPLE - THE ENDERMEN:**
Mojang's internal explanation: The endermen are trying to bring about the collapse of all dimensions by displacing blocks key to the world's existence. They do this one block at a time, hilariously slowly. But they have eternity on their side.

This explanation exists but was never officially revealed. The mystery fuels player imagination.

**EXAMPLE FOR MODDERS:**
If you add mysterious content, do not feel obligated to explain everything. Leave room for player interpretation and theorycrafting.

---

### 13. KEEP IT VANILLA

The Minecraft community is extremely creative. Hundreds of thousands of mods, adventure maps, minigames, and texture packs exist.

**THE PRINCIPLE:**
Minecraft appeals to many different player types looking for different experiences. **THE BASE GAME SHOULD EXPAND IN ALL DIRECTIONS RATHER THAN DIVING DEEP INTO ONE AREA.**

Let the community create specialized content. The vanilla game provides a broad foundation.

**EXAMPLE:**
Vanilla Minecraft has basic automation with redstone. The community creates elaborate tech mods. Vanilla has simple magic with enchanting and potions. The community creates complex magic mods. Vanilla stays broad; mods go deep.

---

## PART 4: CREATURE DESIGN RULES

---

### 14. REAL-LIFE ANIMALS SHOULD BE FRIENDLY

**ALL REAL ANIMALS IN MINECRAFT MUST BE FRIENDLY OR NEUTRAL AT WORST.**

This supports the "ugly cute" aesthetic and ensures content is available in peaceful difficulty.

**ANIMALS THAT CAN DEFEND THEMSELVES:**
Wolves and polar bears can fight back when provoked, but they do not hunt players.

**REAL DANGERS ARE RESERVED FOR NOCTURNAL MONSTERS AND FANTASY CREATURES.**

**FANTASTICAL ABILITIES ARE ACCEPTABLE:**
- Turtle shells as potion ingredients
- Dolphins increasing swimming speed
- Cats scaring away creepers

These abilities are clearly fantastical and do not misrepresent real animal behavior.

**EXAMPLE FOR MODDERS:**
If you add a real animal like a lion or shark, do not make it hostile. Make it neutral at most. If you want a hostile predator, create a fantasy creature instead.

---

### 15. HOSTILE MOBS SHOULD BE UNIQUE

Minecraft has the opportunity to create entirely new mythology. **WHEN DESIGNING HOSTILE MONSTERS, LOOK BEYOND TRADITIONAL FANTASY BEASTS.**

**WHAT TO AVOID:**
Minotaurs, unicorns, griffins, and other creatures from existing mythology.

**WHAT WORKS:**
Creepers, endermen, shulkers, guardians. These are unique to Minecraft.

**EXCEPTIONS:**
- Undead (zombies, skeletons) serve as reference enemies players can compare other monsters against
- The Ender Dragon was a traditional element Notch specifically wanted

**EXAMPLE FOR MODDERS:**
Instead of adding a werewolf, design an original creature with unique behaviors and appearance. Create new mythology rather than borrowing from existing sources.

---

### 16. MOBS NEED PERSONALITY

Every mob, hostile or friendly, needs to feel alive. **VISUALS, SOUNDS, AND AI BEHAVIORS ALL CONTRIBUTE TO HOW PLAYERS RELATE TO CREATURES.**

**THE MINIMUM REQUIREMENTS:**
1. **MOBS MUST HAVE EYES**
2. **MOBS MUST LOOK AT PLAYERS WHEN NEARBY**
3. **EACH MOB NEEDS AT LEAST ONE STANDOUT BEHAVIOR**

**EXAMPLES OF STANDOUT BEHAVIORS:**
- Llamas spitting at threats
- Shulkers peeking out of shells
- Ocelots sneaking up on chickens
- Wolves tilting their heads
- Pandas rolling over

**EXAMPLE FOR MODDERS:**
Before finalizing a mob, identify its one distinctive behavior. What does it do that no other mob does? If you cannot answer this question, the mob needs more development.

---

### 17. GENDER NEUTRALITY IS A CORE PRINCIPLE

**ALL CREATURES AND MONSTERS SHOULD BE ASSUMED GENDERLESS OR OF UNKNOWN GENDER.**

In English, avoid words like "brethren," "king," "queen," or "duchess" for mobs.

**PLAYER CHARACTERS:**
Players represent individuals and can be "he" or "she." When depicting player characters, ensure they are portrayed with equal ability and purpose regardless of gender presentation.

**EXAMPLE FOR MODDERS:**
When writing descriptions or dialogue involving mobs, use "it" or "they" rather than gendered pronouns. If you add a boss mob, do not call it "King" or "Queen" anything.

---

## PART 5: VISUAL AND PRESENTATION RULES

---

### 18. NO BLOOD, NO GORE

Cartoon violence in Minecraft is prominent, but **BLOOD AND GORE MUST BE AVOIDED IN ALL VISUAL ELEMENTS.**

This includes animations, particles, textures, and sound design.

**EXAMPLES OF CHANGES MADE:**
- The killer bunny texture was modified to remove blood
- The zombie pigman texture was changed to remove blood details

**EXAMPLE FOR MODDERS:**
If a mob takes damage, use generic hurt particles. Do not add blood splatter effects. If a mob dies, it should poof into smoke, not explode into gore.

---

### 19. YOU ARE NOT WEARING A HUD

**INFORMATION SHOULD BE CONVEYED THROUGH IN-WORLD OBJECTS WHENEVER POSSIBLE, NOT THROUGH TEXT MESSAGES OR HEADS-UP DISPLAYS.**

**GOOD EXAMPLES OF IN-WORLD INFORMATION:**
- Maps show location through a physical item
- Compasses point to spawn through a physical needle
- Clocks show time through a physical dial

**THE PRINCIPLE:**
If information is important enough to display, design an in-character tool to show it. Avoid floating text, screen overlays, or meta-game indicators.

**JEB'S POSITION ON THE F3 DEBUG SCREEN:**
He personally opposes it. Players should not need to know coordinates, block states, or light levels. If these matter for gameplay, in-game tools should display them.

**EXAMPLE FOR MODDERS:**
Instead of adding a HUD element that shows nearby ore, create a craftable dowsing rod item that points toward valuable blocks. Keep information delivery within the game world.

---

### 20. MINECRAFT IS SIMPLE - THE "UGLY CUTE" AESTHETIC

The blocky characters and low-resolution textures are intentional. **THEY KEEP THE BARRIER LOW FOR COMMUNITY CONTENT CREATION.**

Anyone should feel they can create their own animal, monster, or texture for the game and have it feel relevant.

**WHAT BRINGS CREATURES TO LIFE:**
Simple behaviors like wolves tilting their heads, pandas rolling over, or shulkers making funny noises. Personality comes from behavior, not visual complexity.

**EXAMPLE FOR MODDERS:**
Do not create hyper-detailed textures that clash with vanilla aesthetics. Match the resolution and style of existing content. Focus creative energy on behavior and personality rather than visual fidelity.

---

## PART 6: TECHNICAL AND DEVELOPMENT RULES

---

### 21. BUGS ARE NOT FEATURES

Quirks and glitches are often fun and charming, but **DESIGNS SHOULD NOT RELY ON UNDEFINED BEHAVIOR.**

If players depend on a bug, it can be accidentally "fixed" and break their experience.

**THE SOLUTION:**
When players rely on unintended behavior, implement it as an intentional feature with defined rules.

**EXAMPLES:**
- Booster tracks (a bug) were removed and replaced with powered rails (a feature)
- Negative durability bug was removed and replaced with the "unbreakable" item tag

**EXAMPLE FOR MODDERS:**
If your mod has an unintended interaction that players enjoy, formalize it. Document it, test it, and make it part of the design rather than hoping it continues to work.

---

### 22. MINECRAFT IS NOT AN EDITOR

**MINECRAFT IS A GAME FIRST.**

It is about breaking and placing blocks, finding treasure, hoarding items, and defeating monsters. Creative mode is for building with friends.

**THE PRINCIPLE:**
Game rules, command blocks, and configuration pages should not take away from the core experience. The average player should never need to interact with these systems.

**HOWEVER:**
Mojang acknowledges the community creates experiences for other players. Editor tools may eventually exist as a separate mode, distinct from the core game experience.

**EXAMPLE FOR MODDERS:**
If your mod requires extensive configuration to enjoy, reconsider the design. The best mods work well with default settings and enhance gameplay without requiring players to become system administrators.

---

## PART 7: CREATING VANILLA-STYLE MODS

Based on Mojang developer comments and community analysis, here are the key requirements for creating content that feels authentically Minecraft.

---

### THE FUNDAMENTAL QUESTION

Before adding any feature, ask: **"WOULD MOJANG PLAUSIBLY ADD THIS?"**

If you have to explain why something is Minecraft, it probably is not.

---

### CORE REQUIREMENTS FOR VANILLA FEEL

**1. SIMPLICITY**
Features should be intuitive without explanation.

**2. LOOSE FANTASY VIBES**
Not too realistic, not too outlandish. Minecraft occupies a middle ground.

**3. AVOID REAL-LIFE ASSOCIATIONS**
Especially controversial topics, cultural references, or political content.

**4. NO STEREOTYPES OR MISREPRESENTATION**
Village textures are inspired by Minecraft-based fashion rather than real-life cultures.

---

### THE CAMPFIRE TEST

A good vanilla feature works like the campfire from the Buzzy Bees update:

1. **FUNCTIONALITY REMAINED SIMPLE** - Cook food, emit smoke
2. **IT BECAME ESSENTIAL** - Pacifying bees, safe honey harvesting
3. **IT INTERACTED WITH EXISTING SYSTEMS** - Did not replace anything
4. **IT CREATED EMERGENT GAMEPLAY** - Smoke signals, decoration, bee farming strategies

Apply this test to any feature you design.

---

### FUNCTIONALITY GUIDELINES

**DO:**
- Create simple components that enable complex emergent gameplay
- Design features Mojang might add
- Make blocks breakable by default
- Interact meaningfully with existing vanilla mechanics
- Allow players to prevent or stop destruction

**DO NOT:**
- Add features that duplicate vanilla functions
- Copy existing vanilla mechanics directly
- Add unbreakable blocks without specific purpose
- Create isolated systems that ignore vanilla content
- Create unavoidable negative events

---

### DEVELOPER QUOTES TO REMEMBER

**Jens Bergensten on development philosophy:**
"When we add things to Minecraft, we're trying to go slowly so that people can adjust to the changes, but we're also trying to remember that we need to grow slowly in all directions."

**Jens Bergensten on competing with modders:**
"We are in a sense competing with modders. We have an expression that every new vanilla feature kills a would-be community mod."

**Agnes Larsson on design goals:**
"Creating as much player happiness as possible, encompassing various gameplay styles, whereas keeping the principles of the game's inclusivity and simplicity, which include the dynamic of one block at a time."

---

### STRUCTURE DESIGN FROM DEVELOPER FEEDBACK

**ON UNBREAKABLE BLOCKS:**
Developer feedback states unbreakable blocks are too common and make it difficult for players to shape the environment. Use them sparingly with specific purpose.

**ON ENCOURAGING EXPLORATION:**
Create creative ways for players to conquer structures. The Guardian's Mining Fatigue effect encourages exploring ocean monuments as intended without making mining completely impossible.

**ALTERNATIVE DEFENSE IDEAS:**
- Traps that detect wall-breaking and release mobs
- Environmental hazards hidden in walls
- Structural elements that collapse if mined incorrectly

---

## PART 8: DEVELOPMENT ANECDOTES AND LESSONS

---

### THE BEACON STORY

**LESSON: DESIGN CHALLENGES BEFORE REWARDS**

Jeb joined a Survival mode server where players had achieved everything. They had all items, all monster grinders, all amazing buildings. He felt these accomplished players needed something requiring more grinding and teamwork.

This led to the beacon block and its expensive pyramid of precious metals.

**WHAT HAPPENED:**
Players outsmarted the system. They exploited the fact that iron golems automatically generate in large villages. They got all materials through automation instead of grinding.

**THE TAKEAWAY:**
Jeb invented the reward (beacon) before designing the challenge (wither boss). It is easier to design challenges than rewards because rewards impact overall game balance. When players find shortcuts, the balance breaks.

---

### THE REDSTONE REPEATER

**LESSON: SIMPLICITY OVER OPTIMIZATION**

The redstone repeater was the first specific-purpose block for redstone contraptions. Its original purpose was to act as a diode, allowing signals to pass in only one direction.

The repeater also acted as a delay. Jeb made the delay configurable with four settings.

**THE MATHEMATICAL APPROACH:**
Initial values were 1, 2, 5, and 7 redstone ticks because these require the fewest repeaters to create all delays from 1 through 10.

The community suggested 1, 2, 5, and 6 would be better for delays up to 12.

**THE FINAL DECISION:**
Jeb chose 1, 2, 3, and 4 because it was more straightforward and easier to understand.

**THE TAKEAWAY:**
Intuitive design beats mathematical optimization. Players should not need to calculate optimal combinations. Simple, predictable values are better than clever ones.

---

### THE ROSE TO POPPY CHANGE

**LESSON: COMMUNITY FEEDBACK CAN DRIVE CHANGE**

During the "Update that Changed the World," Mojang added new flowers including the oxeye daisy, allium, and tulip. Jeb also wanted to add a peony flower.

A community member pointed out that peonies are flowering shrubs, not flowers.

Jeb mentioned that the same was true for roses.

Another community member sarcastically responded: "Gee, wonder who could fix that?"

**WHAT HAPPENED:**
The challenge was accepted. The rose flower became a poppy. Roses returned later as double-block bush plants.

The community still has a "remember the rose" day, half joking and half serious.

**THE TAKEAWAY:**
Accuracy matters to the community. When feedback is valid, act on it. Find compromises that respect both accuracy and player sentiment.

---

### THE EVIL VILLAGERS (ILLAGERS)

**LESSON: CREATE FLEXIBLE ANTAGONIST TYPES**

Minecraft: Dungeons needed significantly more hostile monsters than the vanilla game. The team explored different concepts.

**THE SOLUTION:**
Create "evil villagers" as a generic antagonist type. Names considered included "ill-willed villagers," "illvillagers," "evillagers," and "illagers." The shortest option won.

In Swedish, the equivalent would be "fybo" (bad villager), punning on "bybo" (villager).

**WHY ILLAGERS WORK:**
They can have any agenda or skills necessary to fuel a plot. They are flexible antagonists that fit the Minecraft aesthetic while providing endless enemy variety. Their skills lean toward "magic" rather than engineering, distinguishing them from player capabilities.

---

### THE CHARCOAL ADDITION

**LESSON: NEW FEATURES CAN RESHAPE PROGRESSION**

In early Survival mode, finding coal was crucial. Coal unlocked torches, which enabled mining and surviving the night.

When charcoal was added, players could get light sources wherever there were trees.

**THE IMPACT:**
Charcoal had significant impact on the game's technology tree and meta gameplay. It provided an alternative path that did not require finding coal deposits.

**THE TAKEAWAY:**
Mojang is usually careful not to upset progression with new features. They prefer to expand or deepen the technology tree rather than bypass it. Charcoal was acceptable because it solved a problem (no coal nearby) and allowed different play styles rather than invalidating existing progression.

---

## PART 9: QUICK REFERENCE CHECKLIST

---

### BEFORE ADDING ANY FEATURE

- Does it interact with one block at a time?
- Does it respect existing features without power creep?
- Is it different rather than better?
- Can players prevent or control any negative effects?
- Does it work in Hardcore mode without requiring respawn?
- Is it intuitive without HUD elements?
- Does it add gameplay value not present in vanilla?
- Would Mojang plausibly add this?

---

### FOR MOBS

- Is it a unique design, not traditional fantasy?
- Does it have eyes that track players?
- Does it have at least one standout behavior?
- Is it genderless or of unknown gender?
- If it is a real animal, is it friendly or neutral only?
- Does it avoid blood and gore?

---

### FOR ITEMS

- Is it multi-purpose or does it have significant single use?
- Is it a type rather than a named unique item?
- If it is a block, is it breakable?
- Does it justify its inventory slot?

---

### FOR STRUCTURES

- Can players find it through reasonable discovery mechanics?
- Are defensive measures creative rather than just unbreakable walls?
- Does exploring it feel rewarding rather than tedious?

---

## PART 10: INSPIRATIONAL GAMES

Understanding Minecraft's influences helps create authentic content.

**DWARF FORTRESS (2006)**
Developer: Tarn Adams
Influence: Procedural generation, emergent gameplay, simulation depth, caring for citizens' mental health, incredible complexity from simple rules

**ROLLERCOASTER TYCOON (1999)**
Developer: Chris Sawyer
Influence: Creativity combined with problem solving, AI systems that players manage rather than control directly, visitors acting autonomously while players handle urgent problems

**DUNGEON MASTER (1987)**
Developers: Doug Bell, Andy Jaros, Wayne Holder
Influence: Real-time spelunking, traditional fantasy RPG elements, first-person dungeon exploration

**INFINIMINER (2009)**
Developer: Zach Barth
Influence: Block-based interaction model, first-person building and mining. This was the breakthrough inspiration for Minecraft's entire interaction system.

**BLOOD BOWL THIRD EDITION**
Publisher: Games Workshop
Influence: Clear game rules with specific purposes. In Third Edition, a Lineman and Catcher have the same base statistics, but the Catcher has a "Catch" skill that adds new behavior. This clean design taught Jeb to value when game rules have clear and specific purposes, calling out effects and differences in mechanics rather than hiding them in numbers.

---

## CONCLUSION

Creating vanilla-style Minecraft content requires understanding that **MINECRAFT IS NOT JUST A GAME. IT IS A DESIGN PHILOSOPHY.**

**THE FIVE CORE TENETS:**

1. **PLAYER AGENCY ABOVE ALL** - Players control their experience completely
2. **SIMPLICITY ENABLES COMPLEXITY** - Simple components create emergent gameplay
3. **MYSTERY OVER EXPLANATION** - Let players create their own stories
4. **FAIRNESS AND RESPONSIBILITY** - Bad things happen, but players can prevent them
5. **ONE BLOCK AT A TIME** - The fundamental interaction that defines everything

When designing content, always return to these principles. If a feature conflicts with any of them, reconsider the design.

**THE FINAL TEST:**
If you have to explain why something feels like Minecraft, it probably does not.

---

*Sources: Minecraft Game Design by Jens Bergensten (Mojang, 2019), Mojang developer interviews and social media posts, community analysis, and official Minecraft documentation.*
