A simple Spigot/Paper/Bukkit plugin to add essential home commands to your minecraft server.

Use https://modrinth.com/mod/cardboard to get this working on fabric.

![Server Homes Banner](https://cdn.modrinth.com/data/cached_images/c4ba370356e8c6fd7e26a2523a69d01e6733d7b2.png)

### Features
- Configuration file for server admins to tweak settings
- Colorful text to be more interactive I guess
- Independently saves player homes in seperate configuration files
- Lightweight

### Commands
```
- /home <home>        // Teleport to a specified home
- /homes              // List all homes
- /sethome <home>     // Sets a new home
- /delhome <home>     // Delete a specified home
- /confirm            // Confirm a delhome or a sethome overwrite.
- /cancel             // Cancels action
```

![A screenshot of the plugin's command output](https://cdn.modrinth.com/data/cached_images/13946241bdb680f7b70ae0459e8c6d0ca0ad7687.png)

### Configuration
The configuration file is pretty self explanatory.

#### config.yml
```yaml
home:
  max_homes: 3
  max_homes_vip: 5
  enable_sounds: true
  enable_message: true # enables the countdown message

  confirmation:
    timeout_seconds: 30 # players with the vip permission skip this
    enable_delete_confirm: true
    enable_overwrite_confirm: true

  teleport:
    safe_landing_check: false # checks the home teleport location to make sure the player doesn't take damage on teleport
    delay_seconds: 5
    cooldown_seconds: 10
    cancel_events:
      movement: true
      damage: true
      attack: true
      inventory: false
      interact: true

  admin:
    allow_cross_world_teleportation: true
    blocked_worlds:
      # - world_nether
      # - world_end
      # - world
```

### Permissions
Permissions are set using a permissions plugin such as luckyperms or you can set default permissions in `plugin.yml`

#### plugin.yml
```yaml
main: dev.tallenpeli.serverHomes.ServerHomes
name: ServerHomes
version: 1.0.0
api-version: 1.21

commands:
  home:
    description: Teleport to your set home
    usage: /home <name>
    permission: tallenpeli.serverHomes.home
    permission-message: §cYou don't have permission to use the home command!
  homes:
    description: List all your homes
    usage: /homes
    permission: tallenpeli.serverHomes.homes
    permission-message: §cYou don't have permission to use the homes command!
  sethome:
    description: Set your home
    usage: /sethome <name>
    permission: tallenpeli.serverHomes.sethome
    permission-message: §cYou don't have permission to use the sethome command!
  delhome:
    description: Set your home
    usage: /delhome <name>
    permission: tallenpeli.serverHomes.delhome
    permission-message: §cYou don't have permission to use the delhome command!
  confirm:
    description: Confirm a home deletion
    usage: /confirm
    permission: tallenpeli.serverHomes.confirm
    permission-message: §cYou don't have permission to use the confirm command!
  cancel:
    description: Cancels a confirmation request
    usage: /cancel
    permission: tallenpeli.serverHomes.cancel
    permission-message: §cYou don't have permission to use the cancel command!

permissions:
  tallenpeli.serverHomes.home:
    description: Teleports you to your home.
    default: true
  tallenpeli.serverHomes.sethome:
    description: Sets the player's home.
    default: true
  tallenpeli.serverHomes.delhome:
    description: Delete a specified home.
    default: true
  tallenpeli.serverHomes.confirm:
    description: Confirm a home deletion.
    default: true
  tallenpeli.serverHomes.cancel:
    description: Cancel a pending confirmation.
    default: true
  tallenpeli.serverHomes.homes:
    description: Lists all your homes.
    default: true

  tallenpeli.serverHomes.cooldown.bypass:
    description: Allows certain players to bypass the teleport cooldown.
    default: op
  tallenpeli.serverHomes.delay.bypass:
    description: Allows certain players to bypass the teleport delay (timer).
    default: op
  tallenpeli.serverHomes.confirm.bypass:
    description: Allows certain players to skip all confirmation prompts.
    default: op
  tallenpeli.serverHomes.limit.vip:
    description: Grants the player the higher home limit (max_homes_vip).
    default: false
```

### Reporting issues/Feature request?
Report issues or request features to the Github issue tracker or directly to my inbox at [mail@tallenpeli.dev](mailto:mail@tallenpeli.dev).
