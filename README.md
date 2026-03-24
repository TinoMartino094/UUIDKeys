# UUIDKeys

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**UUIDKeys** is a server-side Fabric mod that revolutionizes vault looting by signing keys together! It incentivizes multiplayer cooperation by allowing groups of players to share Trial and Ominous Trial Keys, granting each signer their own unique rewards from Vaults without consuming the vanilla loot slot.

## Current Features

- **Combinatorial Signatures**: Trial Keys and Ominous Trial Keys can now hold multiple player signatures.
- **Key Splitting**: Extract your own signature from a group key using a simple crafting recipe.
- **Key Merging**: Combine your signed key with a friend's key to create a shared access key.
- **Vault Redemption Bypass**: Vaults recognize unique signatures, allowing each player who signed a key to get their rewards individually.
- **Vanilla Compatibility**: Using a signed key does not block your standard vanilla trial key redemption slot!

## How to Use

### 1. Signing a Key
Simply use a Trial Key or Ominous Trial Key in a crafting grid. If the key has no signatures, it will be initialized with yours (if the mod is configured to track the crafter). 
*Note: In the current version, you split and merge signatures using the crafting table.*

### 2. Splitting a Key
If you have a key with multiple signatures, put it in the crafting grid alone. The result will be a key with ONLY your signature. The "remaining item" left in the crafting grid will be the original key with your signature removed.

### 3. Merging Keys
Put two signed keys of the same type in the crafting grid. They will merge into a single key containing the signatures from both, provided the total doesn't exceed the limit.

### 4. Opening Vaults
When you approach a Vault or Ominous Vault with a signed key, the Vault will check if your UUID is in the key's signature. If it is, and you haven't redeemed *this specific signature* at *this vault* yet, you can use the key to get loot!

---

## Icon Generation Prompt
*Since I couldn't generate the icon directly, here is the suggested prompt for use with your favorite AI image generator:*

> "A high-quality, stylized Minecraft-themed icon featuring a glowing, ornate Trial Key at the center. Around the key, three or four distinct, minimalist player head silhouettes are arranged in a circular formation, connected to the key by thin, ethereal lines of light. The background is a dark, atmospheric Trial Chamber vault texture with a subtle depth-of-field effect. Vibrant gold and copper tones dominate the key, while the players and light trails glow with a soft cyan or lime-green energy, symbolizing cooperation and unique signatures. Professional digital art style, 1024x1024, clean composition."

---

## AI-Assisted Development
This mod was developed with the assistance of Antigravity, an AI coding assistant. Transparency is important to us, and we acknowledge the role of AI in streamlining the implementation of complex mixins and recipe logic.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
