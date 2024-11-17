# TrinketsPlugin

A Minecraft plugin that introduces a customizable trinket system with the `/trinkets` command. Players can manage accessories that grant special attributes and level-based restrictions. The plugin integrates with a MySQL database and offers extensive configuration options.

---

## Features

### **/trinkets Command**
- Opens a GUI with three menu options:
  1. **Accessories** (currently functional).
  2. **Jewels** (coming soon).
  3. **Runes** (coming soon).

### **Accessories**
- Equip accessories by right-clicking (RMB) while holding the item in hand.
- Accessories grant attributes (e.g., strength, speed) based on their properties as Minecraft items.
- To unequip an accessory:
  - Open the `/trinkets` GUI.
  - Click on the equipped accessory.

### **Level-Based Restrictions**
- Restrict accessories by player level using the `blocks.yml` configuration file.
- Configure custom messages sent to players when they fail to meet level requirements:
  - Use the placeholder `%level%` in messages to dynamically display the required level.
  - Example configuration:
    ```yaml
    message: "You must be at least level %level% to use this item!"
    items:
      fyrgons_ring_of_fire:
        id: TNT_MINECART
        display_name: '&6[ III ] &l&cFYRGON`S RING OF FIRE'
        lv: 100
    ```
- Supported accessory types:
  - **RING_1**: `TNT_MINECART`
  - **RING_2**: `HOPPER_MINECART`
  - **NECKLACE**: `CHEST_MINECART`
  - **ADORNMENT**: `FURNACE_MINECART`
  - **CLOAK**: `WHITE_BANNER`

### **Database Integration**
- Accessories are stored in a MySQL database for each player.
- Configure database connection details in `config.yml`.

---

## Configuration

### **config.yml**
- Set your MySQL database connection:
  ```yaml
  database:
    host: host
    port: port
    name: database name
    username: username
    password: password
