A simple Spigot/Paper/Bukkit plugin to add essential home commands to your minecraft server.

Use https://modrinth.com/mod/cardboard to get this working on fabric.

![Server Homes Banner](https://cdn.modrinth.com/data/cached_images/b6c0311bbec7b62e3850210e89b1e4714607542b.png)

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
```

![A screenshot of the plugin's command output](https://cdn.modrinth.com/data/cached_images/13946241bdb680f7b70ae0459e8c6d0ca0ad7687.png)

### Configuration
The configuration file is pretty self explanatory.

> config.yml
```
home:
  max_homes: 5
  enable_sounds: true

  confirmation:
    timeout_seconds: 30

  teleport:
    delay_seconds: 5
    cooldown_seconds: 10
    cancel_events:
      movement: true
      damage: true
```

report issues to the github or [mail@tallenpeli.dev](mailto:mail@tallenpeli.dev).
