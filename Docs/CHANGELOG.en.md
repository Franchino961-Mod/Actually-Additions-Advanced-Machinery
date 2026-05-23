# Changelog - Advanced Machinery

All notable changes to the **Advanced Machinery** mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.7.1] - GUI Alignment & Energy Shader Support

### Added
- **White Energy Bar Sprite**: Converted the energy bar fill sprite to grayscale/white to support dynamic coloring via shaders (such as Actually Additions' animated rainbow shader).

### Fixed
- **Energy Bar Dimensions**: Resized the GUI background's dark energy bar rectangle from 90px to 85px to match the dimensions of Actually Additions.
- **`ENERGY_HEIGHT` Constant**: Updated `ENERGY_HEIGHT` to `83` (from `90`) in `AdvancedEmpowererScreen.java` to align the inner drawable fill area with Actually Additions, preventing visual overflow at maximum capacity.
- **Progress Arrow Constants**: Updated progress arrow rendering coordinates to match the new `22x16` sprite dimensions.

---

## [0.7.0] - Energy Upgrade System & Critical Fixes

### Added
- **Energy Upgrade Item**: Replaced the Efficiency Upgrade with a new `Energy Upgrade` item that serves a dual purpose: reduces energy consumption per tick and increases the machine's internal energy buffer capacity.
- **Dynamic Energy Buffer**: The Advanced Empowerer's energy capacity now scales exponentially with installed Energy Upgrades, from 2,000,000 FE (base) up to 20,000,000 FE (8 upgrades).

### Changed
- **Upgrade System Redesign**: The upgrade slot formerly holding Efficiency Upgrades (slot 7) now accepts Energy Upgrades, which combine energy reduction and buffer expansion in a single item.
- **Exponential Speed Formula**: Speed Upgrade now uses `S(u) = 10^(u/8)`, reaching 10x speed with 8 upgrades (time reduced from 200 to 20 ticks).
- **Exponential Energy Formula**: Energy per tick is now `usage = baseUsage * 10^((2*S - E) / 8)`, allowing Energy Upgrades to fully offset the additional cost introduced by Speed Upgrades.
- **Upgrade Slot Limit**: Both Speed and Energy Upgrade slots now accept up to 8 items (previously 4).
- **`ContainerData` expanded to 6 values**: Added two extra slots (indices 4 and 5) to synchronize the dynamic `maxEnergy` value to the client, required by the new variable-capacity energy buffer.

### Fixed
- **`MutableEnergyStorage` declared as `static` inner class**: Previously a non-static inner class, it held an implicit reference to the outer `BlockEntity`, causing potential memory leaks and hidden coupling to the inventory handler. Now static, with inventory access delegated to an `IntSupplier` passed at construction time.
- **`setStored()` now clamps correctly**: Previously assigned the raw value directly to `this.energy` without bounds checking. If the saved NBT energy exceeded the current capacity (e.g. after removing Energy Upgrades), `receiveEnergy()` and `extractEnergy()` would produce inconsistent results. Now clamps to `[0, getMaxEnergyStored()]`.
- **Atomic energy synchronization (16-bit split)**: The two half-words of `energyStored` and `maxEnergy` are now applied atomically using staging variables (`pendingEnergyLow`, `pendingMaxEnergyLow`). The full value is committed only when the HI half-word arrives, preventing the GUI from reading a hybrid value between two update frames.
- **`ClientEvents` — added `bus = Bus.MOD`**: `RegisterMenuScreensEvent` is a Mod Bus event. The missing `bus` parameter caused it to be subscribed on the Game Bus instead, meaning the GUI screen was never registered and the game would crash on first right-click.
- **`neoforge.mods.toml` — corrected NeoForge `versionRange`**: Was `[${neo_version},)` (expanding to `[21.1.223,)`), which required exactly that build or higher. Now uses `${neo_version_range}` (defined as `[21.1.0,)` in `gradle.properties`), accepting any compatible 21.1.x build.

---

## [0.6.1] - Slot Coordinate Fix
### Fixed
- **Slot Coordinates**: Corrected the GUI slot positions for all input slots and the output slot to correctly match the final texture layout.

---

## [0.6.0] - GUI, Crafting Recipe & JEI Finalization
### Added
- **Crafting Recipe for Advanced Empowerer**: Added the in-game crafting recipe to obtain the Advanced Empowerer block.
- **Final GUI Texture**: Added the definitive GUI texture for the Advanced Empowerer screen.
- **JEI Catalyst Registration**: The Advanced Empowerer block is now registered as a recipe catalyst in JEI.
- **JEI Recipe Transfer**: Support for Shift+Click recipe transfer from JEI to the Advanced Empowerer GUI.

---

## [0.5.2] - JEI Slot Range Fix
### Fixed
- **JEI Transfer Slot Ranges**: Updated the recipe transfer handler to correctly reference all 8 machine slots and 36 player inventory slots, following the 5-slot input layout introduced in 0.5.0.

---

## [0.5.1] - Code Style & Minor Fixes
### Fixed
- **Level Null Check**: Added a null check for `level` in the block entity tick method to prevent potential NullPointerException.

### Changed
- **Lightweight Client-Side Menu**: The client-side menu constructor now uses a lightweight `ItemStackHandler` dummy instead of a full `BlockEntity` instance, preventing crashes during race conditions or unloaded chunks. The `stillValid` method now returns `false` when the block entity is unavailable.

### Refactored
- Cleaned up comments and formatting in `AdvancedEmpowererScreen`.
- Updated inventory drop comment in `AdvancedEmpowererBlock` to reflect the correct slot range.
- Refined slot role comments and removed outdated notes in `AdvancedEmpowererBlockEntity`.

---

## [0.5.0] - 5-Slot Input Layout & Asset Refactor
### Added
- **5-Slot Input Support (BlockEntity)**: The Advanced Empowerer now supports 1 base input + 4 modifier inputs, enabling all standard Actually Additions Empowerer recipes.
- **5-Slot Input Support (Menu)**: Updated the container layout to reflect the new cross-shaped 5-slot input arrangement (top, left, center, right, bottom).
- **Directional Blockstate**: Added `facing` variants (north, south, east, west) to the Advanced Empowerer blockstate definition.

### Changed
- **Block Model Restructured**: Reorganized block model elements and groups for correctness and visual consistency.

### Fixed
- **quickMoveStack Fallback**: Added a fallback for Shift+Click between the player inventory and hotbar when no machine slot accepts the item.

### Removed
- **Unused GUI Textures**: Removed `advanced_empowerer_2.png` and `advanced_empowerer_3.png` (unused intermediate textures).

---

## [0.4.1] - Build & Code Cleanup
### Changed
- **Build Configuration**: Added `archives_base_name` property and reorganized `gradle.properties` sections for clarity.

### Refactored
- Removed client-side menu screen registration from the main mod class (moved to a dedicated client event handler).
- Replaced fully qualified class names with proper static imports in `AdvancedEmpowererBlock`.

---

## [0.4.0] - Logic & Content Improvements
### Added
- **Horizontal Facing Property**: The Advanced Empowerer block now rotates to face the player upon placement.
- **Inventory Drop on Break**: All inventory contents (inputs, output, upgrades) are correctly dropped into the world when the block is broken.
- **Explosion Survival Loot**: Added an explosion survival condition to the Advanced Empowerer loot table so items are not destroyed by explosions.
- **Italian Localization**: Added full Italian translation for all mod items, blocks, and GUI labels.
- **Speed Upgrade Crafting Recipe**: Added the in-game crafting recipe to obtain the Speed Upgrade item.
- **Efficiency Upgrade Crafting Recipe**: Added the in-game crafting recipe to obtain the Efficiency Upgrade item.

### Changed
- **Enhanced Energy Storage**: Improved `MutableEnergyStorage` behavior and client syncing in the block entity.
- **Refined Inventory Layout**: Adjusted slot count and slot roles in the block entity for clarity.
- **Improved Menu Constructors**: Refactored server and client constructors in `AdvancedEmpowererMenu` for better separation of concerns and more robust slot management.

---

## [0.3.1] - Docs & Repository Cleanup
### Added
- **MIT License**: Added the `LICENSE` file to the repository.
- **README**: Added the initial `README.md` file.
- **CHANGELOG**: Added the initial `CHANGELOG` file.

### Refactored
- Reorganized and updated `.gitignore` for better coverage of Java, Gradle, and NeoForge artifacts.
- Reorganized `gradle.properties` for improved clarity and consistency.

---

## [0.3.0] - Assets & Data
### Added
- **Block Textures**: Added top, side, and bottom textures for the Advanced Empowerer block.
- **GUI Texture**: Added the initial GUI background texture for the Advanced Empowerer screen.
- **Item Textures**: Added textures for Speed Upgrade and Efficiency Upgrade items.
- **Block Model**: Added the initial block model definition with correct face culling.
- **Blockstate Definition**: Added the initial blockstate model mapping.
- **Item Model Mappings**: Added item model definitions for the Advanced Empowerer, Speed Upgrade, and Efficiency Upgrade.
- **English Translations**: Added all English translation strings (`en_us.json`).
- **Mod Icon**: Added the mod icon image.
- **Loot Table**: Added the block loot table for the Advanced Empowerer.
- **Crafting Recipe (Data)**: Added the initial data-driven crafting recipe for the Advanced Empowerer.
- **Resource Pack Metadata**: Added `pack.mcmeta` for the resource pack.

---

## [0.2.0] - Core Implementation
### Added
- **`AdvancedEmpowererBlock`**: Block class with basic right-click interaction to open the GUI and a ticker for the block entity.
- **`AdvancedEmpowererBlockEntity`**: Full block entity implementation including inventory handling, energy storage, recipe matching (via `EmpowererRecipe`), and recipe processing logic.
- **`AdvancedEmpowererMenu`**: Container menu implementation with server and client constructors, slot layout, and `quickMoveStack` (Shift+Click) logic.
- **`AdvancedEmpowererScreen`**: GUI screen implementation with rendering for the background texture, progress arrow, energy bar, and energy tooltip.
- **JEI Plugin**: Added `AdvancedMachineryJEIPlugin` for initial JEI integration.
- **Registration**: Registered the Advanced Empowerer block entity, menu type, speed and efficiency upgrade items, block items, and creative mode tab.
- **Main Class**: Registered the Advanced Empowerer GUI screen from the main mod class.

---

## [0.1.0] - Project Setup
### Added
- **Gradle Build System**: Configured Gradle wrapper, project name, NeoForge ModDev plugin, and all required dependencies (NeoForge, Actually Additions, JEI).
- **`.gitignore`**: Added `.gitignore` configured for Java, Gradle, and NeoForge projects.
- **`.gitattributes`**: Added `.gitattributes` for consistent line endings across platforms.
- **`mods.toml`**: Added NeoForge mod metadata configuration (`neoforge.mods.toml`).
- **`gradle.properties`**: Added and organized all project properties (mod ID, version, Minecraft version, dependency versions).
- **Initial Commit**: Created the base project structure.