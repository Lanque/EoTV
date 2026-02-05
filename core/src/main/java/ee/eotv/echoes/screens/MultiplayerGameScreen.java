package ee.eotv.echoes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.managers.SoundManager;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.net.NetMessages;
import ee.eotv.echoes.net.NetworkClient;
import ee.eotv.echoes.net.NetworkServer;
import ee.eotv.echoes.ui.Hud;
import ee.eotv.echoes.world.LevelManager;

import java.util.Iterator;
import java.util.Queue;

public class MultiplayerGameScreen implements Screen {
    private final Main game;
    private final boolean isHost;
    private final NetworkServer server;
    private final NetworkClient client;
    private final Player.Role localRole;

    private LevelManager levelManager;
    private Player localPlayer;
    private Player remotePlayer;
    private Player hostPlayer;
    private Player clientPlayer;
    private int localPlayerId;
    private int remotePlayerId;

    private StoneManager stoneManager;
    private SoundManager soundManager;
    private Hud hud;
    private OrthographicCamera camera;
    private ShapeRenderer trajectoryRenderer;
    private ShapeRenderer overlayRenderer;

    private Stage stage;
    private Skin skin;
    private Table pauseTable;
    private Table gameOverTable;
    private boolean isPaused = false;
    private BitmapFont overlayFont;

    private float reviveTimeRequired = 2.5f;
    private float reviveRange = 1.8f;
    private float reviveTimerForHost = 0f;
    private float reviveTimerForClient = 0f;
    private float reviveDisplayTimer = 0f;

    private float currentPower = 0f;
    private float maxPower = 1.2f;
    private boolean isCharging = false;

    private boolean isGameOver = false;
    private boolean isVictory = false;
    private long tick = 0;

    public MultiplayerGameScreen(Main game, NetworkServer server, Player.Role localRole, boolean isHost) {
        this.game = game;
        this.server = server;
        this.client = null;
        this.localRole = localRole;
        this.isHost = isHost;
        init();
    }

    public MultiplayerGameScreen(Main game, NetworkClient client, Player.Role localRole, boolean isHost) {
        this.game = game;
        this.client = client;
        this.server = null;
        this.localRole = localRole;
        this.isHost = isHost;
        init();
    }

    private void init() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();
        soundManager = new SoundManager();
        hud = new Hud();
        trajectoryRenderer = new ShapeRenderer();
        overlayRenderer = new ShapeRenderer();
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();
        createPauseMenu();
        overlayFont = new BitmapFont();

        Vector2 hostStart = new Vector2(2, 25);
        Vector2 clientStart = new Vector2(4, 25);

        Player.Role hostRole = isHost ? localRole : oppositeRole(localRole);
        Player.Role clientRole = isHost ? server.getClientRole() : localRole;
        if (clientRole == null) clientRole = oppositeRole(hostRole);

        hostPlayer = new Player(levelManager.world, levelManager.rayHandler, hostStart.x, hostStart.y);
        clientPlayer = new Player(levelManager.world, levelManager.rayHandler, clientStart.x, clientStart.y);
        hostPlayer.setRole(hostRole);
        clientPlayer.setRole(clientRole);

        localPlayerId = isHost ? server.getHostPlayerId() : 2;
        remotePlayerId = isHost ? server.getClientPlayerId() : 1;

        if (isHost) {
            localPlayer = hostPlayer;
            remotePlayer = clientPlayer;
        } else {
            localPlayer = clientPlayer;
            remotePlayer = hostPlayer;
        }

        if (isHost) {
            stoneManager = new StoneManager(levelManager.world, levelManager, soundManager);
            stoneManager.setImpactListener((x, y, radius, color) -> {
                NetMessages.EchoEvent event = new NetMessages.EchoEvent();
                event.x = x;
                event.y = y;
                event.radius = radius;
                event.r = color.r;
                event.g = color.g;
                event.b = color.b;
                event.a = color.a;
                server.sendEcho(event);

                sendSoundEvent(NetMessages.SoundType.HIT, x, y, true);
            });
            stoneManager.setThrowListener((x, y) -> sendSoundEvent(NetMessages.SoundType.THROW, x, y, true));
        }

        if (isHost) {
            Player.ActionListener actionListener = new Player.ActionListener() {
                @Override
                public void onStep(Player player) {
                    sendSoundEvent(NetMessages.SoundType.STEP, player.getPosition().x, player.getPosition().y, true);
                    sendEchoForStep(player);
                }

                @Override
                public void onLightToggle(Player player, boolean lightOn) {
                    NetMessages.SoundType type = lightOn ? NetMessages.SoundType.LIGHT_ON : NetMessages.SoundType.LIGHT_OFF;
                    sendSoundEvent(type, player.getPosition().x, player.getPosition().y, true);
                }
            };
            hostPlayer.setActionListener(actionListener);
            clientPlayer.setActionListener(actionListener);
        }
    }

    @Override
    public void render(float delta) {
        if (isGameOver) {
            drawScene(delta);
            renderGameOverMenu(delta);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (isPaused) {
            drawScene(delta);
            renderPauseMenu(delta);
            return;
        }

        Gdx.input.setInputProcessor(null);

        if (isHost) {
            updateHost(delta);
        } else {
            updateClient(delta);
        }

        drawScene(delta);
    }

    private void drawScene(float delta) {
        camera.position.set(localPlayer.getPosition(), 0);
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        levelManager.drawWorld(camera);
        levelManager.drawItems(camera);
        levelManager.drawDoors(camera);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        localPlayer.render(game.batch);
        remotePlayer.render(game.batch);
        for (Enemy enemy : levelManager.getEnemies()) {
            enemy.render(game.batch);
        }
        game.batch.end();

        levelManager.renderLights(camera);
        if (isHost && stoneManager != null && localPlayer.canThrowStones()) {
            stoneManager.renderTrajectory(localPlayer, camera);
        } else {
            renderTrajectory();
        }
        hud.render(localPlayer, camera);
        if (localPlayer.isDowned()) {
            renderDownedOverlay();
        }
        renderRevivePrompt();
    }

    private void updateHost(float delta) {
        if (!isGameOver && !isVictory) {
            localPlayer.update(delta, levelManager, camera, soundManager);

            NetMessages.InputState remoteInput = server.getLatestInput(remotePlayerId);
            Player.PlayerInput converted = convertInput(remoteInput);
            remotePlayer.updateFromInput(delta, converted, soundManager, levelManager);

            if (localPlayer.canThrowStones()) {
                stoneManager.handleInput(localPlayer, camera);
            }

            Queue<NetMessages.ThrowStone> throwQueue = server.getThrowQueue();
            NetMessages.ThrowStone throwStone;
            while ((throwStone = throwQueue.poll()) != null) {
                if (throwStone.playerId == remotePlayerId) {
                    stoneManager.throwStoneFromNetwork(remotePlayer, throwStone.targetX, throwStone.targetY, throwStone.power);
                }
            }

            levelManager.update(delta);
            stoneManager.update(delta);

            for (Enemy enemy : levelManager.getEnemies()) {
                Player closest = getClosestPlayer(enemy);
                enemy.update(closest, delta);
            }

            handleDownedState(delta, remoteInput);
            updateItemsAndDoors();
            updateVictoryAndGameOver();
        }

        updateReviveDisplayTimer(delta, Gdx.input.isKeyPressed(Input.Keys.E));
        sendWorldState();
    }

    private void updateClient(float delta) {
        if (client != null && client.isDisconnected()) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }
        handleClientInput();
        applyWorldState();
        levelManager.updateClient(delta);
        handleEchoEvents();
        handleSoundEvents();
        updateReviveDisplayTimer(delta, Gdx.input.isKeyPressed(Input.Keys.E));
    }

    private void updateItemsAndDoors() {
        Iterator<Item> itemIter = levelManager.getItems().iterator();
        while (itemIter.hasNext()) {
            Item item = itemIter.next();
            if (item.isActive()) {
                if (localPlayer.getPosition().dst(item.getPosition()) < 0.8f) {
                    if (item.getType() != Item.Type.STONE || localPlayer.canThrowStones()) {
                        localPlayer.collectItem(item);
                        sendSoundEvent(NetMessages.SoundType.COLLECT, localPlayer.getPosition().x, localPlayer.getPosition().y, true);
                        item.collect();
                        itemIter.remove();
                    }
                } else if (remotePlayer.getPosition().dst(item.getPosition()) < 0.8f) {
                    if (item.getType() != Item.Type.STONE || remotePlayer.canThrowStones()) {
                        remotePlayer.collectItem(item);
                        sendSoundEvent(NetMessages.SoundType.COLLECT, remotePlayer.getPosition().x, remotePlayer.getPosition().y, true);
                        item.collect();
                        itemIter.remove();
                    }
                }
            }
        }

        for (Door door : levelManager.getDoors()) {
            if (!door.isOpen()) {
                boolean localNear = localPlayer.getPosition().dst(door.getCenter()) < 1.5f;
                boolean remoteNear = remotePlayer.getPosition().dst(door.getCenter()) < 1.5f;
                if ((localNear && localPlayer.hasKeycard) || (remoteNear && remotePlayer.hasKeycard)) {
                    door.open();
                    float soundX = localNear ? localPlayer.getPosition().x : remotePlayer.getPosition().x;
                    float soundY = localNear ? localPlayer.getPosition().y : remotePlayer.getPosition().y;
                    sendSoundEvent(NetMessages.SoundType.DOOR, soundX, soundY, true);
                }
            }
        }
    }

    private void updateVictoryAndGameOver() {
        if (localPlayer.isDowned() && remotePlayer.isDowned()) {
            isGameOver = true;
        }

        if (levelManager.getExitZone() != null) {
            Rectangle localRect = new Rectangle(localPlayer.getPosition().x - 0.2f, localPlayer.getPosition().y - 0.2f, 0.4f, 0.4f);
            Rectangle remoteRect = new Rectangle(remotePlayer.getPosition().x - 0.2f, remotePlayer.getPosition().y - 0.2f, 0.4f, 0.4f);
            if (localRect.overlaps(levelManager.getExitZone().getBounds()) ||
                remoteRect.overlaps(levelManager.getExitZone().getBounds())) {
                isVictory = true;
            }
        }
    }

    private void sendWorldState() {
        NetMessages.WorldState state = new NetMessages.WorldState();
        state.tick = ++tick;
        state.players = new NetMessages.PlayerState[2];
        state.players[0] = buildPlayerState(server.getHostPlayerId(), hostPlayer);
        state.players[1] = buildPlayerState(server.getClientPlayerId(), clientPlayer);

        state.enemies = new NetMessages.EnemyState[levelManager.getEnemies().size()];
        for (int i = 0; i < levelManager.getEnemies().size(); i++) {
            Enemy enemy = levelManager.getEnemies().get(i);
            NetMessages.EnemyState es = new NetMessages.EnemyState();
            es.index = i;
            es.x = enemy.getPosition().x;
            es.y = enemy.getPosition().y;
            es.vx = enemy.body.getLinearVelocity().x;
            es.vy = enemy.body.getLinearVelocity().y;
            es.facingRight = enemy.body.getLinearVelocity().x >= 0;
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

        state.gameOver = isGameOver;
        state.victory = isVictory;
        server.sendWorldState(state);
    }

    private void handleClientInput() {
        if (client == null) return;

        NetMessages.InputState input = new NetMessages.InputState();
        input.playerId = localPlayerId;
        input.up = Gdx.input.isKeyPressed(Input.Keys.W);
        input.down = Gdx.input.isKeyPressed(Input.Keys.S);
        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        input.toggleLight = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F);
        input.revive = Gdx.input.isKeyPressed(Input.Keys.E);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angleRad = (float) Math.atan2(mouse.y - localPlayer.getPosition().y, mouse.x - localPlayer.getPosition().x);
        input.aimAngle = (float) Math.toDegrees(angleRad);
        client.sendInput(input);

        if (localRole == Player.Role.STONES) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && localPlayer.ammo > 0) {
                isCharging = true;
                currentPower = Math.min(currentPower + 1.5f * Gdx.graphics.getDeltaTime(), maxPower);
            } else if (isCharging) {
                NetMessages.ThrowStone throwStone = new NetMessages.ThrowStone();
                throwStone.playerId = localPlayerId;
                throwStone.targetX = mouse.x;
                throwStone.targetY = mouse.y;
                throwStone.power = currentPower;
                client.sendThrow(throwStone);
                currentPower = 0f;
                isCharging = false;
            }
        }
    }

    private void applyWorldState() {
        NetMessages.WorldState state = client.getLatestWorldState();
        if (state == null) return;

        for (NetMessages.PlayerState ps : state.players) {
            if (ps == null) continue;
            Player target = ps.id == localPlayerId ? localPlayer : remotePlayer;
            target.setNetworkState(ps.x, ps.y, ps.vx, ps.vy, ps.lightOn, ps.running, ps.moving,
                ps.stamina, ps.ammo, ps.hasKeycard, ps.aimAngle, ps.role, ps.downed);
        }

        for (NetMessages.EnemyState es : state.enemies) {
            if (es == null || es.index < 0 || es.index >= levelManager.getEnemies().size()) continue;
            levelManager.getEnemies().get(es.index).setNetworkState(es.x, es.y, es.vx, es.vy, es.facingRight);
        }

        levelManager.getItems().clear();
        for (NetMessages.ItemState is : state.items) {
            if (is != null) {
                levelManager.spawnItem(is.type, is.x, is.y);
            }
        }

        for (NetMessages.DoorState ds : state.doors) {
            for (Door door : levelManager.getDoors()) {
                if (Math.abs(door.getPosition().x - ds.x) < 0.01f &&
                    Math.abs(door.getPosition().y - ds.y) < 0.01f) {
                    if (ds.open && !door.isOpen()) door.open();
                }
            }
        }

        isGameOver = state.gameOver;
        isVictory = state.victory;
    }

    private void handleEchoEvents() {
        Queue<NetMessages.EchoEvent> echoes = client.getEchoEvents();
        NetMessages.EchoEvent event;
        while ((event = echoes.poll()) != null) {
            levelManager.addEcho(event.x, event.y, event.radius, new Color(event.r, event.g, event.b, event.a));
        }
    }

    private void handleSoundEvents() {
        Queue<NetMessages.SoundEvent> sounds = client.getSoundEvents();
        NetMessages.SoundEvent event;
        while ((event = sounds.poll()) != null) {
            playSoundWithDistance(event.type, event.x, event.y);
        }
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

    private Player.PlayerInput convertInput(NetMessages.InputState input) {
        Player.PlayerInput converted = new Player.PlayerInput();
        if (input == null) return converted;
        converted.up = input.up;
        converted.down = input.down;
        converted.left = input.left;
        converted.right = input.right;
        converted.wantsToRun = input.run;
        converted.toggleLight = input.toggleLight;
        converted.aimAngle = input.aimAngle;
        return converted;
    }

    private Player getClosestPlayer(Enemy enemy) {
        float distLocal = enemy.getPosition().dst(localPlayer.getPosition());
        float distRemote = enemy.getPosition().dst(remotePlayer.getPosition());
        return distLocal <= distRemote ? localPlayer : remotePlayer;
    }

    private Player.Role oppositeRole(Player.Role role) {
        return role == Player.Role.FLASHLIGHT ? Player.Role.STONES : Player.Role.FLASHLIGHT;
    }

    private void renderTrajectory() {
        if (!isCharging || localRole != Player.Role.STONES) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Vector2 startPos = localPlayer.getPosition();
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        Vector2 dir = new Vector2(mouse.x - startPos.x, mouse.y - startPos.y).nor();

        float distance = (currentPower / 0.0628f) * 0.6f;

        trajectoryRenderer.setProjectionMatrix(camera.combined);
        trajectoryRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int dots = 12;
        for (int i = 1; i <= dots; i++) {
            float step = (distance / dots) * i;
            float dotX = startPos.x + dir.x * (step + 0.7f);
            float dotY = startPos.y + dir.y * (step + 0.7f);

            float alpha = 1.0f - ((float)i / dots) * 0.5f;
            trajectoryRenderer.setColor(0.4f, 0.8f, 1f, alpha);
            float size = 0.12f - (i * 0.005f);
            trajectoryRenderer.circle(dotX, dotY, size, 8);
        }

        trajectoryRenderer.setColor(1, 1, 1, 0.8f);
        trajectoryRenderer.circle(startPos.x + dir.x * (distance + 0.7f), startPos.y + dir.y * (distance + 0.7f), 0.15f, 10);

        trajectoryRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void handleDownedState(float delta, NetMessages.InputState remoteInput) {
        for (Enemy enemy : levelManager.getEnemies()) {
            if (!localPlayer.isDowned() && enemy.getPosition().dst(localPlayer.getPosition()) < 0.85f) {
                localPlayer.setDowned(true);
                enemy.forceReturnToPatrol();
            }
            if (!remotePlayer.isDowned() && enemy.getPosition().dst(remotePlayer.getPosition()) < 0.85f) {
                remotePlayer.setDowned(true);
                enemy.forceReturnToPatrol();
            }
        }

        boolean localRevive = Gdx.input.isKeyPressed(Input.Keys.E);
        boolean remoteRevive = remoteInput != null && remoteInput.revive;

        reviveTimerForHost = updateReviveTimer(localPlayer, remotePlayer, remoteRevive, reviveTimerForHost, delta);
        reviveTimerForClient = updateReviveTimer(remotePlayer, localPlayer, localRevive, reviveTimerForClient, delta);
    }

    private float updateReviveTimer(Player target, Player helper, boolean isReviving, float timer, float delta) {
        if (target.isDowned() && !helper.isDowned() && helper.getPosition().dst(target.getPosition()) <= reviveRange && isReviving) {
            timer += delta;
            if (timer >= reviveTimeRequired) {
                target.setDowned(false);
                timer = 0f;
            }
        } else {
            timer = 0f;
        }
        return timer;
    }

    private void renderDownedOverlay() {
        float viewWidth = camera.viewportWidth * camera.zoom;
        float viewHeight = camera.viewportHeight * camera.zoom;
        float startX = camera.position.x - viewWidth / 2f;
        float startY = camera.position.y - viewHeight / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        overlayRenderer.setProjectionMatrix(camera.combined);
        overlayRenderer.begin(ShapeRenderer.ShapeType.Filled);
        overlayRenderer.setColor(0.6f, 0.0f, 0.0f, 0.35f);
        overlayRenderer.rect(startX, startY, viewWidth, viewHeight);
        overlayRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderRevivePrompt() {
        boolean canRevive = !localPlayer.isDowned()
            && remotePlayer.isDowned()
            && localPlayer.getPosition().dst(remotePlayer.getPosition()) <= reviveRange;

        if (!canRevive) return;

        String text = "Hold E to revive";
        float textX = stage.getViewport().getWorldWidth() * 0.5f - 70f;
        float textY = 40f;

        overlayFont.getData().setScale(1.0f);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        overlayFont.setColor(1f, 1f, 1f, 1f);
        overlayFont.draw(game.batch, text, textX, textY);
        game.batch.end();

        float progress = Math.min(reviveDisplayTimer / reviveTimeRequired, 1f);
        float barWidth = 140f;
        float barHeight = 6f;
        float barX = stage.getViewport().getWorldWidth() * 0.5f - barWidth * 0.5f;
        float barY = 22f;

        overlayRenderer.setProjectionMatrix(stage.getCamera().combined);
        overlayRenderer.begin(ShapeRenderer.ShapeType.Filled);
        overlayRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        overlayRenderer.rect(barX, barY, barWidth, barHeight);
        overlayRenderer.setColor(0.8f, 0.2f, 0.2f, 0.9f);
        overlayRenderer.rect(barX, barY, barWidth * progress, barHeight);
        overlayRenderer.end();
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    private void renderPauseMenu(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        pauseTable.setVisible(true);
        gameOverTable.setVisible(false);
        stage.act(delta);
        stage.draw();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void createPauseMenu() {
        pauseTable = new Table();
        pauseTable.setFillParent(true);

        TextButton resumeBtn = new TextButton("RESUME", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (server != null) server.stop();
                if (client != null) client.stop();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        pauseTable.add(resumeBtn).width(200).height(50).pad(10).row();
        pauseTable.add(exitBtn).width(200).height(50).pad(10);
        stage.addActor(pauseTable);

        gameOverTable = new Table();
        gameOverTable.setFillParent(true);

        TextButton restartBtn = new TextButton("RESTART", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (server != null) server.stop();
                if (client != null) client.stop();
                game.setScreen(new MultiplayerMenuScreen(game));
            }
        });

        TextButton gameOverExitBtn = new TextButton("EXIT TO MENU", skin);
        gameOverExitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (server != null) server.stop();
                if (client != null) client.stop();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        com.badlogic.gdx.scenes.scene2d.ui.Label gameOverLabel =
            new com.badlogic.gdx.scenes.scene2d.ui.Label("GAME OVER", skin);

        gameOverTable.add(gameOverLabel).padBottom(30).row();
        gameOverTable.add(restartBtn).width(200).height(50).pad(10).row();
        gameOverTable.add(gameOverExitBtn).width(200).height(50).pad(10);
        gameOverTable.setVisible(false);
        stage.addActor(gameOverTable);
    }

    private void renderGameOverMenu(float delta) {
        gameOverTable.setVisible(true);
        pauseTable.setVisible(false);
        Gdx.input.setInputProcessor(stage);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        stage.act(delta);
        stage.draw();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        skin.add("default", new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(skin.getFont("default"), Color.WHITE));

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = skin.newDrawable("white", 0.2f, 0.2f, 0.2f, 0.9f);
        style.down = skin.newDrawable("white", 0.4f, 0.4f, 0.4f, 0.9f);
        style.over = skin.newDrawable("white", 0.3f, 0.3f, 0.3f, 0.9f);
        style.font = skin.getFont("default");
        skin.add("default", style);
        return skin;
    }

    private void updateReviveDisplayTimer(float delta, boolean isHoldingRevive) {
        boolean canRevive = !localPlayer.isDowned()
            && remotePlayer.isDowned()
            && localPlayer.getPosition().dst(remotePlayer.getPosition()) <= reviveRange;
        if (canRevive && isHoldingRevive) {
            reviveDisplayTimer = Math.min(reviveDisplayTimer + delta, reviveTimeRequired);
        } else {
            reviveDisplayTimer = 0f;
        }
    }

    private void sendEchoForStep(Player player) {
        float radius = player.isRunning ? 32f : 18f;
        Color color = player.isRunning
            ? new Color(0.5f, 0.7f, 1f, 0.5f)
            : new Color(0.4f, 0.5f, 0.8f, 0.5f);

        NetMessages.EchoEvent event = new NetMessages.EchoEvent();
        event.x = player.getPosition().x;
        event.y = player.getPosition().y;
        event.radius = radius;
        event.r = color.r;
        event.g = color.g;
        event.b = color.b;
        event.a = color.a;
        if (server != null) {
            server.sendEcho(event);
        }
    }

    private void sendSoundEvent(NetMessages.SoundType type, float x, float y, boolean playLocal) {
        if (playLocal) {
            playSoundWithDistance(type, x, y);
        }
        if (server != null) {
            NetMessages.SoundEvent event = new NetMessages.SoundEvent();
            event.type = type;
            event.x = x;
            event.y = y;
            server.sendSound(event);
        }
    }

    private void playSoundWithDistance(NetMessages.SoundType type, float x, float y) {
        float dist = localPlayer.getPosition().dst(x, y);
        float maxDist = getMaxDistance(type);
        if (dist > maxDist) return;
        float volume = Math.max(0.05f, 1f - (dist / maxDist));

        switch (type) {
            case STEP:
                soundManager.playStep(volume * 0.4f);
                break;
            case THROW:
                soundManager.playThrow(volume * 0.9f);
                break;
            case HIT:
                soundManager.playHit(volume * 0.8f);
                break;
            case COLLECT:
                soundManager.playCollect(volume * 0.7f);
                break;
            case DOOR:
                soundManager.playDoor(volume);
                break;
            case LIGHT_ON:
                soundManager.playLightOn(volume * 0.7f);
                break;
            case LIGHT_OFF:
                soundManager.playLightOff(volume * 0.7f);
                break;
        }
    }

    private float getMaxDistance(NetMessages.SoundType type) {
        switch (type) {
            case STEP:
                return 10f;
            case LIGHT_ON:
            case LIGHT_OFF:
                return 12f;
            case COLLECT:
                return 9f;
            case DOOR:
                return 16f;
            case THROW:
                return 18f;
            case HIT:
                return 20f;
            default:
                return 10f;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (trajectoryRenderer != null) trajectoryRenderer.dispose();
        if (overlayRenderer != null) overlayRenderer.dispose();
        levelManager.dispose();
        hud.dispose();
        soundManager.dispose();
        localPlayer.dispose();
        remotePlayer.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (overlayFont != null) overlayFont.dispose();
        if (server != null) server.stop();
        if (client != null) client.stop();
    }
}
