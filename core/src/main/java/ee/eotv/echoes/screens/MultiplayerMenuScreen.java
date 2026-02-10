package ee.eotv.echoes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.net.NetworkClient;
import ee.eotv.echoes.net.NetMessages;

import java.io.IOException;

public class MultiplayerMenuScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private Skin skin;

    private NetworkClient client;

    private Player.Role selectedRole = Player.Role.FLASHLIGHT;
    private boolean isWaiting = false;
    private boolean lobbyReady = false;

    private Label statusLabel;
    private TextButton hostBtn;
    private TextButton joinBtn;
    private TextButton flashlightBtn;
    private TextButton stonesBtn;
    private TextButton backBtn;
    private TextButton cancelBtn;
    private TextField ipField;
    private TextField lobbyField;

    public MultiplayerMenuScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        createMenu();
    }

    private void createMenu() {
        skin = createBasicSkin();
        Table table = new Table();
        table.setFillParent(true);

        Label titleLabel = new Label("MULTIPLAYER", new Label.LabelStyle(skin.getFont("default"), Color.CYAN));
        titleLabel.setFontScale(2.0f);

        statusLabel = new Label("", new Label.LabelStyle(skin.getFont("default"), Color.LIGHT_GRAY));

        flashlightBtn = new TextButton("FLASHLIGHT", skin);
        stonesBtn = new TextButton("STONES", skin);
        updateRoleButtonColors();

        flashlightBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedRole = Player.Role.FLASHLIGHT;
                updateRoleButtonColors();
            }
        });

        stonesBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedRole = Player.Role.STONES;
                updateRoleButtonColors();
            }
        });

        hostBtn = new TextButton("HOST", skin);
        hostBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startHosting();
            }
        });

        joinBtn = new TextButton("JOIN", skin);
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startJoining();
            }
        });

        ipField = new TextField("127.0.0.1", skin);
        ipField.setMessageText("Server IP");

        lobbyField = new TextField("1", skin);
        lobbyField.setMessageText("Lobby ID");

        cancelBtn = new TextButton("CANCEL", skin);
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stopNetwork();
                isWaiting = false;
                lobbyReady = false;
                statusLabel.setText("");
            }
        });

        backBtn = new TextButton("BACK", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stopNetwork();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        table.add(titleLabel).padBottom(30).row();
        table.add(new Label("ROLE", skin)).padBottom(10).row();
        table.add(flashlightBtn).width(200).height(45).pad(6).row();
        table.add(stonesBtn).width(200).height(45).pad(6).row();
        table.add(new Label("CONNECT", skin)).padTop(20).padBottom(10).row();
        table.add(ipField).width(240).height(40).pad(6).row();
        table.add(lobbyField).width(240).height(40).pad(6).row();
        table.add(hostBtn).width(200).height(45).pad(6).row();
        table.add(joinBtn).width(200).height(45).pad(6).row();
        table.add(statusLabel).padTop(20).row();
        table.add(cancelBtn).width(200).height(45).pad(6).row();
        table.add(backBtn).width(200).height(45).pad(6);

        stage.addActor(table);
    }

    private void startHosting() {
        stopNetwork();
        isWaiting = true;
        lobbyReady = false;
        statusLabel.setText("Connecting to server...");

        client = new NetworkClient();
        try {
            client.connect(ipField.getText());
            client.createLobby(selectedRole);
        } catch (IOException e) {
            statusLabel.setText("Failed to host: " + e.getMessage());
            isWaiting = false;
        }
    }

    private void startJoining() {
        stopNetwork();
        isWaiting = true;
        lobbyReady = false;
        statusLabel.setText("Connecting...");
        client = new NetworkClient();
        try {
            int lobbyId = Integer.parseInt(lobbyField.getText().trim());
            client.connect(ipField.getText());
            client.joinLobby(lobbyId, selectedRole);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid lobby ID.");
            isWaiting = false;
        } catch (IOException e) {
            statusLabel.setText("Failed to connect: " + e.getMessage());
            isWaiting = false;
        }
    }

    private void updateRoleButtonColors() {
        Color active = Color.WHITE;
        Color inactive = Color.LIGHT_GRAY;
        flashlightBtn.getLabel().setColor(selectedRole == Player.Role.FLASHLIGHT ? active : inactive);
        stonesBtn.getLabel().setColor(selectedRole == Player.Role.STONES ? active : inactive);
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = skin.newDrawable("white", 0.2f, 0.2f, 0.2f, 1f);
        style.down = skin.newDrawable("white", 0.4f, 0.4f, 0.4f, 1f);
        style.over = skin.newDrawable("white", 0.3f, 0.3f, 0.3f, 1f);
        style.font = skin.getFont("default");
        skin.add("default", style);

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = skin.getFont("default");
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.background = skin.newDrawable("white", 0.1f, 0.1f, 0.1f, 1f);
        fieldStyle.cursor = skin.newDrawable("white", 1f, 1f, 1f, 1f);
        skin.add("default", fieldStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("default"), Color.WHITE);
        skin.add("default", labelStyle);
        return skin;
    }

    @Override
    public void render(float delta) {
        if (isWaiting) {
            if (client != null) {
                if (client.isDisconnected()) {
                    statusLabel.setText("Disconnected from server.");
                    isWaiting = false;
                    stopNetwork();
                }

                NetMessages.LobbyJoinedResponse response = client.getLobbyResponse();
                if (response != null && !response.success) {
                    statusLabel.setText(response.message == null ? "Lobby join failed." : response.message);
                    isWaiting = false;
                } else if (response != null && response.success && !lobbyReady) {
                    lobbyReady = true;
                    lobbyField.setText(String.valueOf(response.lobbyId));
                    if (response.isHost) {
                        statusLabel.setText("Lobby " + response.lobbyId + " created. Waiting for player...");
                    } else {
                        statusLabel.setText("Joined lobby " + response.lobbyId + ". Waiting to start...");
                    }
                }

                NetMessages.StartGame startGame = client.getStartGame();
                if (startGame != null) {
                    game.setScreen(new MultiplayerGameScreen(game, client, startGame));
                }
            }
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    private void stopNetwork() {
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stopNetwork();
        stage.dispose();
        skin.dispose();
    }
}
