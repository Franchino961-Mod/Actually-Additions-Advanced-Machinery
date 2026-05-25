![Actually Additions: Advanced Machinery - Banner](https://raw.githubusercontent.com/Franchino961-Mod/Actually-Additions-Advanced-Machinery/main/Docs/assets/image/Actually%20Additions%20-%20Advanced%20Machinery.png)

# ⚙️ Actually Additions: Advanced Machinery

A powerful addon for **Actually Additions** that introduces a fully functional, multi-block advanced version of the Empowerer — supporting all 4 modifier slots, upgrades, and a complete custom GUI.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://www.minecraft.net/)
[![Version](https://img.shields.io/badge/version-0.7.1-blue.svg)]()
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.223-orange.svg)](https://neoforged.net/)
[![Actually Additions](https://img.shields.io/badge/Actually%20Additions-1.3.1-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/actually-additions)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![en](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![it](https://img.shields.io/badge/lang-it-green.svg)](Docs/README.it.md)

> 📝 **Changelog**: See [CHANGELOG.en.md](Docs/CHANGELOG.en.md) for version history.

---

## 🌟 Why Advanced Machinery?
- **Full Recipe Support**: Supports all Actually Additions Empowerer recipes, including those requiring all 4 modifier items.
- **Upgrade System**: Install Speed and Energy Upgrades (up to 8 each) to optimize your production.
- **Custom GUI**: A clean, intuitive interface showing all input slots in a cross layout, an energy bar, and a progress arrow.
- **JEI Integration**: Browse all compatible Empowerer recipes directly in JEI.

---

## 🚀 Quick Start
1. Craft the **Advanced Empowerer** block.
2. Place it in your base and right-click to open the GUI.
3. Insert the **base item** in the center slot and up to **4 modifier items** in the surrounding cross slots.
4. Power the machine with Forge Energy (FE).
5. Wait for the progress bar to complete — the result will appear in the output slot!

---

## ✨ Main Features
- **5-Slot Input Layout**: One base input slot + 4 modifier slots arranged in a cross pattern, matching the full Actually Additions Empowerer recipe format.
- **Output Slot**: A dedicated read-only output slot — items are placed here automatically upon recipe completion.
- **Energy System**: The machine consumes Forge Energy (FE). A visual energy bar is displayed in the GUI.
- **Speed Upgrade**: Reduces the ticks required to complete a recipe (up to 8 slots).
- **Energy Upgrade**: Reduces energy consumption per operation and increases internal energy buffer capacity (up to 8 slots).
- **Directional Placement**: The block rotates to face the player when placed.
- **Inventory Drop**: All items are safely dropped into the world when the block is broken.
- **JEI Plugin**: All compatible Empowerer recipes are browsable and transferable via JEI.

---

## 📦 Requirements
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.223 or higher
- **Actually Additions**: 1.3.1
- **JEI** *(optional)*: 19.21.0.247 or higher

---

## 📥 Installation
1. Install NeoForge and the required dependencies.
2. Download the `advanced-machinery` `.jar` file.
3. Place the file in your Minecraft installation's `mods` folder.
4. Launch the game!

---

## 🖥️ Client/Server Behavior
- **Server**: Required. The mod handles block entities, energy, recipe matching, and item processing on the server side.
- **Client**: Required. Includes the custom GUI screen and JEI recipe integration.

---

## ⚠️ Known Limitations
- The Advanced Empowerer requires all 4 modifier slots to be filled for recipes that need them — partial fills will not trigger processing.
- The machine does not currently support automation via hoppers for item insertion (manual insertion required).

---

## 🛠️ Troubleshooting
### The machine is not processing!
- Check that the base item is in the center slot (slot 2) and modifiers are in the surrounding slots.
- Ensure the machine has enough Forge Energy (FE) stored.
- Verify the recipe is a valid Actually Additions Empowerer recipe by checking JEI.
- Make sure the output slot is not full.

---

## ❓ FAQ
**Q: Is this compatible with other Actually Additions addons?**
A: Yes, as long as they use the standard `EmpowererRecipe` format from Actually Additions, all recipes should be recognized automatically.

**Q: Can I automate item insertion?**
A: Direct hopper automation into the machine is not supported in the current version. You can, however, extract items from the output slot using hoppers placed on the block.

---

## 💬 Support & Feedback
If you encounter issues or bugs, please report them with:
- Mod version
- Minecraft / NeoForge / Actually Additions versions
- Detailed description of the problem
- Crash logs (if applicable)

---

## 📄 License
This mod is licensed under the [MIT License](LICENSE). Feel free to include it in your modpacks!

## 👤 Author
**Franchino961** — [GitHub](https://github.com/Franchino961-Mod)
