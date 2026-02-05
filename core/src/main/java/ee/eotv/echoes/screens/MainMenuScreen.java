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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.managers.SaveManager;
import ee.eotv.echoes.managers.SoundManager; // UUS IMPORT
import ee.eotv.echoes.screens.MultiplayerMenuScreen;

public class MainMenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private SoundManager soundManager; // UUS: Helihaldur

    public MainMenuScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Initsialiseerime helid ja paneme muusika käima
        soundManager = new SoundManager();
        soundManager.playMenuMusic();

        createMenu();
    }

    private void createMenu() {
        skin = createBasicSkin();
        Table table = new Table();
        table.setFillParent(true);

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("default"), Color.CYAN);
        Label titleLabel = new Label("E C H O E S", titleStyle);
        titleLabel.setFontScale(3.0f);

        TextButton newGameBtn = new TextButton("NEW GAME", skin);
        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick(); // Mängi klikki
                soundManager.stopMenuMusic(); // Peata muusika enne mängu minekut
                game.setScreen(new GameScreen(game, false));
            }
        });

        TextButton loadGameBtn = new TextButton("LOAD GAME", skin);
        loadGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (SaveManager.hasSave()) {
                    soundManager.playClick();
                    soundManager.stopMenuMusic();
                    game.setScreen(new GameScreen(game, true));
                }
            }
        });

        TextButton exitBtn = new TextButton("EXIT", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                // Siin võiks panna väikse viivituse (Timer), et heli jõuaks kõlada, aga see on keerulisem
                Gdx.app.exit();
            }
        });

        TextButton multiplayerBtn = new TextButton("MULTIPLAYER", skin);
        multiplayerBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                soundManager.stopMenuMusic();
                game.setScreen(new MultiplayerMenuScreen(game));
            }
        });

        table.add(titleLabel).padBottom(50).row();
        table.add(newGameBtn).width(200).height(50).pad(10).row();
        table.add(loadGameBtn).width(200).height(50).pad(10).row();
        table.add(multiplayerBtn).width(200).height(50).pad(10).row();
        table.add(exitBtn).width(200).height(50).pad(10);

        stage.addActor(table);
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
        return skin;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        soundManager.dispose(); // Ära unusta helisid vabastada!
    }
}
