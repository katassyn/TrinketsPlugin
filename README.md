# TrinketsPlugin

A Minecraft plugin that adds an advanced accessories system with boss souls, equipment slots, and special effects.

## Features

### Accessory System
- Multiple equipment slots (rings, necklace, adornment, cloak, shield, belt, gloves, boss soul)
- Level requirements for equipment slots
- Custom attribute modifiers
- Block chance and block strength stats
- GUI-based equipment management

### Boss Souls
The plugin includes 10 unique boss souls with special effects:

1. **Grimmag's Burning Soul**
   - On mob hit: Ignite for 5s, dealing 1000 dmg/s (15s cooldown)
   - On player hit: Ignite for 3s, dealing 50 dmg/s (15s cooldown)

2. **Arachna's Venomous Soul**
   - 10% chance to duplicate unique/mythic items on pickup
   - Gain $10,000 for each mob kill

3. **King Heredur's Frostbound Soul**
   - 10% chance to block 50% incoming damage
   - After block, all entities within 10 blocks get 75% slow

4. **Bearach's Wildheart Soul**
   - Every 15s, your attack roots target for 3s
   - 15% chance to rotate player target 180 degrees

5. **Khalys's Shadowbound Soul**
   - Every 30s, fully evade next damage
   - After evade, next attack deals +300% damage

6. **Mortis's Unchained Soul**
   - Apply 50% weakness for 5s on hit (15s cooldown)
   - Kill mobs to stack +2% damage (max 20 stacks)
   - Taking damage removes 5 stacks

7. **Herald's Molten Soul**
   - 20% damage reduction
   - Reflect 20% damage back to attacker

8. **Sigrismar's Blizzard Soul**
   - Every attack slows target by 30%
   - After 5 hits, freeze target for 1s and deal +300 damage (15s cooldown)

9. **Medusa's Petrifying Soul**
   - On player hit: Levitation for 2s (20s cooldown)
   - After 3 hits on a mob: turn it to stone for 1s (10s cooldown)

10. **Gorga's Abyssal Soul**
    - On player hit: Blindness for 3s (5s cooldown)
    - When HP < 30%: heal to full and gain +30% damage for 5s (60s cooldown)

### Jewel System
The plugin features a powerful jewel system that allows players to enhance their abilities through equippable jewels:

#### Jewel Features
- 15+ unique jewel types with different effects
- Tiered jewel system (Tier 1-3) with increasing power
- GUI-based jewel management
- Level 50 requirement to equip jewels
- Special Focus Jewels that can be stacked (up to 3)

#### Available Jewels
1. **Combat Jewels**
   - Emberfang Jewel: Increases damage (2/4/8%)
   - Windrunner Jewel: Increases movement speed (2/4/8%)
   - Quicksilver Jewel: Increases attack speed (2/4/8%)
   - Lifeblood Jewel: Grants additional health (10/20/30 HP)
   - Ironhide Jewel: Grants armor toughness (1/2/4)

2. **Utility Jewels**
   - Rejuvenation Jewel: Heals after killing enemies (5/10/15 HP for 5s)
   - Lifeforce Jewel: Increases damage after healing (25/50/100% for 5s)
   - Timewarper Jewel: Reduces cooldowns (2/3/5% per jewel, stack up to 3)
   - Berserker Jewel: Grants critical chance (15/20/30% for 5s)
   - Phoenix Jewel: Prevents death (60/45/30 minute cooldown)

3. **Economy Jewels**
   - Merchant Jewel: Better selling prices and crafting discounts
   - Andermant Jewel: Chance to duplicate Andermant (10/20/30%)
   - Sunspire Amber: Get additional Gilded Sunflowers (1/2/3)
   - DrakenMelon Jewel: Get additional drakenmelons (1/2/3)
   - Ingredient Jewel: Get additional ingredients (1/2/3)
   - Collector Jewel: Executes low health enemies (2/3/5% threshold)

#### Commands
- `/jewels` - Opens a link to detailed jewel information
- `/trinkets` - Opens the trinkets menu where you can manage jewels

#### Configuration
Jewels can be configured in the `jewels.yml` file:

```yaml
# Example jewel configuration
DAMAGE:
  display_name: "Emberfang Jewel"
  material: BROWN_DYE
  description: "Grants increased damage"
  max_tier: 3
  max_equipped: 1
  effects:
    tier1:
      damage_percent: 2
    tier2:
      damage_percent: 4
    tier3:
      damage_percent: 8
```

## Requirements

- Java 16 or higher
- Spigot/Paper 1.20.1
- Vault
- MySQL database

## Installation

1. Download the latest release
2. Place the .jar file in your plugins folder
3. Start the server (plugin will generate config files)
4. Configure the database settings in config.yml
5. Restart the server

## Configuration

### config.yml
```yaml
database:
  host: localhost
  port: 3306
  name: your_database
  user: your_username
  password: your_password
```

### blokady.yml
Configure level requirements and restricted items.
```yaml
message: "You must be at least level %level% to use this item!"
items:
  example_item:
    id: DIAMOND
    display_name: "&6Legendary Ring"
    lv: 50
```

## Commands

- `/trinkets` - Open the trinkets menu
- `/jewels` - View information about jewels
- `/soul <q1-q10>` - View information about a specific boss soul
- `/resetattributes [player]` - Reset all attribute modifiers (requires permission: trinkets.resetattributes)

## Permissions

- `trinkets.resetattributes` - Allows use of /resetattributes command

## API for Developers

The plugin provides access to its features through the DatabaseManager class:

```
// Get the database manager
TrinketsPlugin.getInstance().getDatabaseManager()
```

Key methods:
- `getPlayerData(UUID)` - Get player's accessory data
- `loadPlayerData(UUID, Consumer<PlayerData>)` - Load player data asynchronously
- `savePlayerData(UUID, PlayerData)` - Save player data

## Building from Source

1. Clone the repository
2. Install Maven
3. Run `mvn clean package`

## Contributing

1. Fork the repository
2. Create a new branch for your feature
3. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
