package ee.eotv.echoes.server;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryonet.Connection;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Generator;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.net.NetMessages;
import ee.eotv.echoes.world.LevelManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameRoom {
    private static final int HOST_ID = 1;
    private static final int GUEST_ID = 2;
    private static final float REVIVE_TIME_REQUIRED = 2.5f;
    private static final float REVIVE_RANGE = 1.8f;
    private static final float GENERATOR_REPAIR_TIME = 10f;
    private static final float GENERATOR_REPAIR_RANGE = 1.2f;

    public final int id;
    private final Connection hostConnection;
    private Connection guestConnection;

    private final Map<Integer, Integer> connectionToPlayerId = new ConcurrentHashMap<>();
    private Player.Role hostRole;
    private Player.Role guestRole;

    private final LevelManager levelManager;
    private final StoneManager stoneManager;

    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private Player hostPlayerEntity;
    private Player guestPlayerEntity;

    private final Map<Integer, NetMessages.InputState> latestInputs = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> pendingToggleLight = new ConcurrentHashMap<>();
    private final Queue<NetMessages.ThrowStone> throwQueue = new ConcurrentLinkedQueue<>();

    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean victory = false;
    private long tick = 0;

    private float reviveTimerHost = 0f;
    private float reviveTimerGuest = 0f;

    public GameRoom(int id, Connection host, Player.Role role) {
        this.id = id;
        this.hostConnection = host;
        this.hostRole = role;
        this.connectionToPlayerId.put(host.getID(), HOST_ID);

        this.levelManager = new LevelManager(false);
        this.stoneManager = new StoneManager(levelManager.world, levelManager, null);
        this.stoneManager.setImpactListener(this::sendStoneImpact);
        this.stoneManager.setThrowListener((x, y) -> sendSoundEvent(NetMessages.SoundType.THROW, x, y));

        this.hostPlayerEntity = new Player(levelManager.world, levelManager.rayHandler, 2, 25);
        this.hostPlayerEntity.setActionListener(createActionListener());
        this.players.put(HOST_ID, hostPlayerEntity);
    }

    public boolean join(Connection guest, Player.Role role) {
        if (guestConnection != null) {
            NetMessages.LobbyJoinedResponse response = new NetMessages.LobbyJoinedResponse();
            response.success = false;
            response.lobbyId = id;
            response.isHost = false;
            response.message = "Lobby full.";
            guest.sendTCP(response);
            return false;
        }

        if (role != null && role == hostRole) {
            NetMessages.LobbyJoinedResponse response = new NetMessages.LobbyJoinedResponse();
            response.success = false;
            response.lobbyId = id;
            response.isHost = false;
            response.message = "Role already taken.";
            guest.sendTCP(response);
            return false;
        }

        guestConnection = guest;
        connectionToPlayerId.put(guest.getID(), GUEST_ID);

        if (role == null || role == hostRole) {
            guestRole = (hostRole == Player.Role.FLASHLIGHT) ? Player.Role.STONES : Player.Role.FLASHLIGHT;
        } else {
            guestRole = role;
        }

        guestPlayerEntity = new Player(levelManager.world, levelManager.rayHandler, 4, 25);
        guestPlayerEntity.setActionListener(createActionListener());
        players.put(GUEST_ID, guestPlayerEntity);

        NetMessages.LobbyJoinedResponse response = new NetMessages.LobbyJoinedResponse();
        response.success = true;
        response.lobbyId = id;
        response.isHost = false;
        guest.sendTCP(response);

        startGame();
        return true;
    }

    public void handleDisconnect(int connectionId) {
        Integer playerId = connectionToPlayerId.remove(connectionId);
        if (playerId == null) {
            return;
        }
        if (playerId == GUEST_ID) {
            guestConnection = null;
            guestPlayerEntity = null;
            guestRole = null;
        }
        if (playerId == HOST_ID) {
            hostPlayerEntity = null;
            hostRole = null;
        }
        players.remove(playerId);
        latestInputs.remove(playerId);
        throwQueue.clear();
        gameStarted = false;
    }

    public boolean isEmpty() {
        return connectionToPlayerId.isEmpty();
    }

    private void startGame() {
        if (hostPlayerEntity == null || guestPlayerEntity == null) {
            return;
        }
        hostPlayerEntity.setRole(hostRole);
        guestPlayerEntity.setRole(guestRole);
        gameStarted = true;

        NetMessages.StartGame msg1 = new NetMessages.StartGame();
        msg1.playerId = HOST_ID;
        msg1.role = hostRole;
        hostConnection.sendTCP(msg1);

        NetMessages.StartGame msg2 = new NetMessages.StartGame();
        msg2.playerId = GUEST_ID;
        msg2.role = guestRole;
        guestConnection.sendTCP(msg2);

        System.out.println("Room " + id + ": Game started.");
    }

    public void handleInput(int connectionId, NetMessages.InputState input) {
        Integer playerId = connectionToPlayerId.get(connectionId);
        if (playerId == null) {
            return;
        }
        if (input != null && input.toggleLight) {
            pendingToggleLight.put(playerId, true);
        }
        latestInputs.put(playerId, copyInput(input, playerId));
    }

    public void handleThrow(int connectionId, NetMessages.ThrowStone throwStone) {
        Integer playerId = connectionToPlayerId.get(connectionId);
        if (playerId == null) {
            return;
        }
        throwStone.playerId = playerId;
        throwQueue.add(throwStone);
    }

    public void update(float delta) {
        if (!gameStarted || hostPlayerEntity == null || guestPlayerEntity == null) {
            return;
        }

        NetMessages.InputState hostInput = latestInputs.get(HOST_ID);
        NetMessages.InputState guestInput = latestInputs.get(GUEST_ID);

        boolean hostHoldRepair = hostInput != null && hostInput.revive;
        boolean guestHoldRepair = guestInput != null && guestInput.revive;

        boolean hostAttemptRevive = isAttemptingRevive(hostPlayerEntity, guestPlayerEntity, hostHoldRepair);
        boolean guestAttemptRevive = isAttemptingRevive(guestPlayerEntity, hostPlayerEntity, guestHoldRepair);

        Generator hostTarget = (!hostPlayerEntity.isDowned() && hostHoldRepair && !hostAttemptRevive)
            ? findRepairTarget(hostPlayerEntity) : null;
        Generator guestTarget = (!guestPlayerEntity.isDowned() && guestHoldRepair && !guestAttemptRevive)
            ? findRepairTarget(guestPlayerEntity) : null;

        hostPlayerEntity.setFrozen(hostTarget != null);
        guestPlayerEntity.setFrozen(guestTarget != null);

        applyInput(hostPlayerEntity, hostInput, delta);
        applyInput(guestPlayerEntity, guestInput, delta);

        handleThrows(hostTarget, guestTarget);

        levelManager.update(delta);
        stoneManager.update(delta);

        for (Enemy enemy : levelManager.getEnemies()) {
            if (!enemy.isActive()) continue;
            Player closest = getClosestPlayer(enemy);
            if (closest != null) {
                enemy.update(closest, delta);
            }
        }

        handleDownedState(delta, hostInput, guestInput);
        updateGenerators(delta, hostTarget, guestTarget);
        updateItemsAndDoors();
        updateVictoryAndGameOver();

        sendWorldState();
    }

    private void applyInput(Player player, NetMessages.InputState input, float delta) {
        if (player == null) {
            return;
        }
        Player.PlayerInput converted = new Player.PlayerInput();
        boolean togglePending = pendingToggleLight.remove(getPlayerId(player)) != null;
        if (input != null) {
            converted.up = input.up;
            converted.down = input.down;
            converted.left = input.left;
            converted.right = input.right;
            converted.wantsToRun = input.run;
            converted.aimAngle = input.aimAngle;
        }
        converted.toggleLight = togglePending;
        player.updateFromInput(delta, converted, null, levelManager);
    }

    private int getPlayerId(Player player) {
        return player == hostPlayerEntity ? HOST_ID : GUEST_ID;
    }

    private void handleThrows(Generator hostTarget, Generator guestTarget) {
        NetMessages.ThrowStone throwStone;
        while ((throwStone = throwQueue.poll()) != null) {
            if (throwStone.playerId == HOST_ID && hostTarget == null) {
                stoneManager.throwStoneFromNetwork(hostPlayerEntity, throwStone.targetX, throwStone.targetY, throwStone.power);
            } else if (throwStone.playerId == GUEST_ID && guestTarget == null) {
                stoneManager.throwStoneFromNetwork(guestPlayerEntity, throwStone.targetX, throwStone.targetY, throwStone.power);
            }
        }
    }

    private void handleDownedState(float delta, NetMessages.InputState hostInput, NetMessages.InputState guestInput) {
        for (Enemy enemy : levelManager.getEnemies()) {
            if (!hostPlayerEntity.isDowned() && enemy.getPosition().dst(hostPlayerEntity.getPosition()) < 0.85f) {
                hostPlayerEntity.setDowned(true);
                enemy.forceReturnToPatrol();
            }
            if (!guestPlayerEntity.isDowned() && enemy.getPosition().dst(guestPlayerEntity.getPosition()) < 0.85f) {
                guestPlayerEntity.setDowned(true);
                enemy.forceReturnToPatrol();
            }
        }

        boolean hostRevive = hostInput != null && hostInput.revive;
        boolean guestRevive = guestInput != null && guestInput.revive;

        reviveTimerHost = updateReviveTimer(hostPlayerEntity, guestPlayerEntity, guestRevive, reviveTimerHost, delta);
        reviveTimerGuest = updateReviveTimer(guestPlayerEntity, hostPlayerEntity, hostRevive, reviveTimerGuest, delta);
    }

    private float updateReviveTimer(Player target, Player helper, boolean isReviving, float timer, float delta) {
        if (target.isDowned() && !helper.isDowned() && helper.getPosition().dst(target.getPosition()) <= REVIVE_RANGE && isReviving) {
            timer += delta;
            if (timer >= REVIVE_TIME_REQUIRED) {
                target.setDowned(false);
                timer = 0f;
            }
        } else {
            timer = 0f;
        }
        return timer;
    }

    private boolean isAttemptingRevive(Player helper, Player target, boolean isHolding) {
        return isHolding
            && !helper.isDowned()
            && target.isDowned()
            && helper.getPosition().dst(target.getPosition()) <= REVIVE_RANGE;
    }

    private Generator findRepairTarget(Player player) {
        Generator closest = null;
        float bestDist = GENERATOR_REPAIR_RANGE;
        for (Generator generator : levelManager.getGenerators()) {
            if (generator.isRepaired()) continue;
            float dist = player.getPosition().dst(generator.getPosition());
            if (dist <= bestDist) {
                bestDist = dist;
                closest = generator;
            }
        }
        return closest;
    }

    private void updateGenerators(float delta, Generator hostTarget, Generator guestTarget) {
        for (Generator generator : levelManager.getGenerators()) {
            if (generator.isRepaired()) continue;
            boolean repairing = generator == hostTarget || generator == guestTarget;
            if (repairing) {
                float next = generator.getRepairProgress() + delta;
                if (next >= GENERATOR_REPAIR_TIME) {
                    generator.setRepaired(true);
                } else {
                    generator.setRepairProgress(next);
                }
            } else {
                generator.setRepairProgress(0f);
            }
        }

        if (!levelManager.isExitUnlocked() && levelManager.areAllGeneratorsRepaired()) {
            levelManager.setExitUnlocked(true);
        }
        if (levelManager.isExitUnlocked()) {
            clearEnemiesInSafeZone();
        }
    }

    private void clearEnemiesInSafeZone() {
        if (levelManager.getExitZone() == null) return;
        for (Enemy enemy : levelManager.getEnemies()) {
            if (enemy.isActive() && levelManager.getExitZone().getBounds().contains(enemy.getPosition())) {
                enemy.setActive(false);
            }
        }
    }

    private void updateItemsAndDoors() {
        Iterator<Item> itemIter = levelManager.getItems().iterator();
        while (itemIter.hasNext()) {
            Item item = itemIter.next();
            if (item.isActive()) {
                if (hostPlayerEntity.getPosition().dst(item.getPosition()) < 0.8f) {
                    if (item.getType() != Item.Type.STONE || hostPlayerEntity.canThrowStones()) {
                        hostPlayerEntity.collectItem(item);
                        sendSoundEvent(NetMessages.SoundType.COLLECT, hostPlayerEntity.getPosition().x, hostPlayerEntity.getPosition().y);
                        item.collect();
                        itemIter.remove();
                    }
                } else if (guestPlayerEntity.getPosition().dst(item.getPosition()) < 0.8f) {
                    if (item.getType() != Item.Type.STONE || guestPlayerEntity.canThrowStones()) {
                        guestPlayerEntity.collectItem(item);
                        sendSoundEvent(NetMessages.SoundType.COLLECT, guestPlayerEntity.getPosition().x, guestPlayerEntity.getPosition().y);
                        item.collect();
                        itemIter.remove();
                    }
                }
            }
        }

        for (Door door : levelManager.getDoors()) {
            if (!door.isOpen()) {
                boolean hostNear = hostPlayerEntity.getPosition().dst(door.getCenter()) < 1.5f;
                boolean guestNear = guestPlayerEntity.getPosition().dst(door.getCenter()) < 1.5f;
                if ((hostNear && hostPlayerEntity.hasKeycard) || (guestNear && guestPlayerEntity.hasKeycard)) {
                    door.open();
                    float soundX = hostNear ? hostPlayerEntity.getPosition().x : guestPlayerEntity.getPosition().x;
                    float soundY = hostNear ? hostPlayerEntity.getPosition().y : guestPlayerEntity.getPosition().y;
                    sendSoundEvent(NetMessages.SoundType.DOOR, soundX, soundY);
                }
            }
        }
    }

    private void updateVictoryAndGameOver() {
        if (hostPlayerEntity.isDowned() && guestPlayerEntity.isDowned()) {
            gameOver = true;
        }

        if (levelManager.isExitUnlocked() && levelManager.getExitZone() != null) {
            boolean hostInExit = levelManager.getExitZone().getBounds().contains(hostPlayerEntity.getPosition());
            boolean guestInExit = levelManager.getExitZone().getBounds().contains(guestPlayerEntity.getPosition());
            if (hostInExit && guestInExit) {
                victory = true;
            }
        }
    }

    private void sendWorldState() {
        NetMessages.WorldState state = new NetMessages.WorldState();
        state.tick = tick++;

        state.players = new NetMessages.PlayerState[2];
        state.players[0] = buildPlayerState(HOST_ID, hostPlayerEntity);
        state.players[1] = buildPlayerState(GUEST_ID, guestPlayerEntity);

        ArrayList<Enemy> enemies = levelManager.getEnemies();
        state.enemies = new NetMessages.EnemyState[enemies.size()];
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            NetMessages.EnemyState es = new NetMessages.EnemyState();
            es.index = i;
            es.x = e.getPosition().x;
            es.y = e.getPosition().y;
            es.vx = e.body.getLinearVelocity().x;
            es.vy = e.body.getLinearVelocity().y;
            es.facingRight = e.body.getLinearVelocity().x >= 0;
            es.active = e.isActive();
            state.enemies[i] = es;
        }

        state.items = new NetMessages.ItemState[levelManager.getItems().size()];
        for (int i = 0; i < levelManager.getItems().size(); i++) {
            Item item = levelManager.getItems().get(i);
            NetMessages.ItemState is = new NetMessages.ItemState();
            is.x = item.getPosition().x;
            is.y = item.getPosition().y;
            is.type = item.getType();
            state.items[i] = is;
        }

        state.doors = new NetMessages.DoorState[levelManager.getDoors().size()];
        for (int i = 0; i < levelManager.getDoors().size(); i++) {
            Door door = levelManager.getDoors().get(i);
            NetMessages.DoorState ds = new NetMessages.DoorState();
            ds.x = door.getPosition().x;
            ds.y = door.getPosition().y;
            ds.open = door.isOpen();
            state.doors[i] = ds;
        }

        state.generators = new NetMessages.GeneratorState[levelManager.getGenerators().size()];
        for (int i = 0; i < levelManager.getGenerators().size(); i++) {
            Generator generator = levelManager.getGenerators().get(i);
            NetMessages.GeneratorState gs = new NetMessages.GeneratorState();
            gs.index = i;
            gs.x = generator.getPosition().x;
            gs.y = generator.getPosition().y;
            gs.repaired = generator.isRepaired();
            gs.progress = generator.getRepairProgress();
            state.generators[i] = gs;
        }

        state.exitUnlocked = levelManager.isExitUnlocked();
        state.gameOver = gameOver;
        state.victory = victory;

        broadcastUDP(state);
    }

    private NetMessages.PlayerState buildPlayerState(int id, Player player) {
        NetMessages.PlayerState ps = new NetMessages.PlayerState();
        ps.id = id;
        ps.x = player.getPosition().x;
        ps.y = player.getPosition().y;
        ps.vx = player.getVelocity().x;
        ps.vy = player.getVelocity().y;
        ps.lightOn = player.isLightOn;
        ps.running = player.isRunning;
        ps.moving = player.isMoving;
        ps.stamina = player.stamina;
        ps.ammo = player.ammo;
        ps.hasKeycard = player.hasKeycard;
        ps.aimAngle = player.getAimAngle();
        ps.role = player.getRole();
        ps.downed = player.isDowned();
        return ps;
    }

    private Player getClosestPlayer(Enemy enemy) {
        float distHost = enemy.getPosition().dst(hostPlayerEntity.getPosition());
        float distGuest = enemy.getPosition().dst(guestPlayerEntity.getPosition());
        return distHost <= distGuest ? hostPlayerEntity : guestPlayerEntity;
    }

    private NetMessages.InputState copyInput(NetMessages.InputState input, int playerId) {
        NetMessages.InputState copy = new NetMessages.InputState();
        copy.playerId = playerId;
        if (input == null) {
            return copy;
        }
        copy.up = input.up;
        copy.down = input.down;
        copy.left = input.left;
        copy.right = input.right;
        copy.run = input.run;
        copy.toggleLight = input.toggleLight;
        copy.revive = input.revive;
        copy.aimAngle = input.aimAngle;
        return copy;
    }

    private Player.ActionListener createActionListener() {
        return new Player.ActionListener() {
            @Override
            public void onStep(Player player) {
                sendSoundEvent(NetMessages.SoundType.STEP, player.getPosition().x, player.getPosition().y);
                sendEchoForStep(player);
            }

            @Override
            public void onLightToggle(Player player, boolean lightOn) {
                NetMessages.SoundType type = lightOn ? NetMessages.SoundType.LIGHT_ON : NetMessages.SoundType.LIGHT_OFF;
                sendSoundEvent(type, player.getPosition().x, player.getPosition().y);
            }
        };
    }

    private void sendEchoForStep(Player player) {
        float radius = player.isRunning ? 32f : 18f;
        Color color = player.isRunning
            ? new Color(0.5f, 0.7f, 1f, 0.5f)
            : new Color(0.4f, 0.5f, 0.8f, 0.5f);
        sendEchoEvent(player.getPosition().x, player.getPosition().y, radius, color);
    }

    private void sendStoneImpact(float x, float y, float radius, Color color) {
        sendEchoEvent(x, y, radius, color);
        sendSoundEvent(NetMessages.SoundType.HIT, x, y);
    }

    private void sendEchoEvent(float x, float y, float radius, Color color) {
        NetMessages.EchoEvent event = new NetMessages.EchoEvent();
        event.x = x;
        event.y = y;
        event.radius = radius;
        event.r = color.r;
        event.g = color.g;
        event.b = color.b;
        event.a = color.a;
        broadcastTCP(event);
    }

    private void sendSoundEvent(NetMessages.SoundType type, float x, float y) {
        NetMessages.SoundEvent event = new NetMessages.SoundEvent();
        event.type = type;
        event.x = x;
        event.y = y;
        broadcastTCP(event);
    }

    private void broadcastTCP(Object obj) {
        hostConnection.sendTCP(obj);
        if (guestConnection != null) guestConnection.sendTCP(obj);
    }

    private void broadcastUDP(Object obj) {
        hostConnection.sendUDP(obj);
        if (guestConnection != null) guestConnection.sendUDP(obj);
    }
}
