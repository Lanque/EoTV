# ITI0301-2026

## Echoes of the Void

### Game description:

**Echoes of the Void** is a 2D top-down cooperative survival horror game for two players. Players find themselves trapped in a dark, abandoned facility infested with light-sensitive monsters.

The objective is to escape the facility by finding and repairing broken electrical generators. Repairing them restores power to the security doors, unlocking the **Exit Zone**. Survival depends on teamwork—players must choose between roles (Flashlight or Stones), manage their stamina, and cover each other while performing tasks.

The game features an asymmetric information system: lighting is limited, and enemies can hear footsteps and impacts.

### Keybinds:

| Key | Action |
| --- | --- |
| **W, A, S, D** | Move the character |
| **Shift** (Hold) | Run (consumes Stamina) |
| **Mouse Cursor** | Aim / Look direction |
| **F** or **Space** | Toggle Flashlight (Flashlight role only) |
| **E** (Hold) | Repair Generator / Revive Teammate |
| **Left Mouse Button** | Throw Stone (Stones role only) |
| **ESC** | Open Pause Menu |

### How to Play:

1. **Launch the Game:** Open the application.
2. **Multiplayer Menu:** Select "Multiplayer" from the main menu.
3. **Choose Roles:**
* **Flashlight:** Has a cone of light to see in the dark and reveal enemies.
* **Stones:** Can throw stones to distract enemies or trigger echoes to see surroundings.


4. **Create/Join Lobby:** Start the dedicated server first. One player clicks **HOST** to create a lobby (gets a Lobby ID), the other player connects with the server IP and Lobby ID.
5. **Objective:** Locate red generators on the map. Hold **E** to repair them. The other player must defend the repairer.
6. **Win Condition:** Once all generators are fixed, the **Exit Zone** (Green area) unlocks. Both players must stand in the zone to win.
7. **Revive:** If a player is attacked, they enter a "Downed" state. The other player must revive them by holding **E**.

### Installation and Starting the Game:

#### 1. Build the Project

* Clone this repository.
* Open a terminal in the project root directory.
* Run the Gradle build command:
* **Client (Windows):** `.\gradlew lwjgl3:dist`
* **Client (Mac/Linux):** `./gradlew lwjgl3:dist`
* **Server (Windows):** `.\gradlew server:jar`
* **Server (Mac/Linux):** `./gradlew server:jar`



#### 2. Run the Dedicated Server

* Navigate to: `server/build/libs/`
* Run the server JAR:
```bash
java -jar EchoesOfTheVoid-server.jar
```

*(Note: Ensure the `assets` folder is accessible relative to the JAR file if needed.)*

#### 3. Run the Client

* Navigate to: `lwjgl3/build/libs/`
* Run the client JAR:
```bash
java -jar lwjgl3-1.0.0.jar

```


*(Note: Ensure the `assets` folder is accessible relative to the JAR file).*

### Technologies Used:

* **Java 21** - Programming language
* **LibGDX 1.13.1** - Game development framework
* **KryoNet 2.22** - Network communication (TCP/UDP)
* **Box2D** - Physics simulation and collision detection
* **Box2DLights 1.5** - Dynamic lighting and shadows
* **Gradle 8.x** - Build automation tool

### Features:

| Feature | Description |
| --- | --- |
| **Co-op Multiplayer** | Real-time 2-player gameplay over LAN/Internet. |
| **AI Enemies** | Zombies patrol the map, investigate noises, and chase players upon visual contact. |
| **Dynamic Lighting** | Raycasting light system. Shadows hide enemies and objects. |
| **Sound Propagation** | Sounds (steps, throws) travel based on distance and attract AI. |
| **Generator System** | Synchronized repair mechanics required to unlock the exit. |
| **Revive System** | Players are not eliminated instantly but can be revived by teammates. |
| **Physics Interaction** | Stone throwing mechanics with trajectories and wall bouncing. |
| **Save/Load System** | Local game state saving for single-player practice. |

### Authors:

* Gregor Aab
* Alex-Christian Jõgiste
* Egert Kelder

### Connecting the Client to TalTech Server:

This game operates on a **Dedicated Server** architecture.

1. Run the server JAR on the TalTech server.
2. Clients connect to the server IP on TCP port **54555** and UDP port **54777**.
3. One client creates a lobby (**HOST** button), the other joins with the same server IP and Lobby ID.
