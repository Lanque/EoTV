package ee.eotv.echoes;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.screens.MainMenuScreen; // UUS IMPORT

public class Main extends Game {
    public SpriteBatch batch;
    public static Enemy zombiInstance;

    @Override
    public void create() {
        batch = new SpriteBatch();
        // Enne oli: this.setScreen(new GameScreen(this));
        // NÜÜD:
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
