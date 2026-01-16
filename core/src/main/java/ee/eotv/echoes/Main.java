package ee.eotv.echoes;

import com.badlogic.gdx.Game;
// --- PARANDATUD IMPORDID ---
import ee.eotv.echoes.screens.GameScreen; // Täispikk tee!
import ee.eotv.echoes.entities.Enemy;     // Täispikk tee!

public class Main extends Game {

    // Staatiline viide, et teised klassid saaksid zombile sündmusi saata
    public static Enemy zombiInstance;

    @Override
    public void create() {
        // Käivitamisel paneme kohe mängu ekraani ette
        setScreen(new GameScreen(this));
    }

    @Override
    public void render() {
        // "super.render()" on ÜLIOLULINE Game klassi puhul!
        // See delegeerib joonistamise aktiivsele ekraanile (GameScreen).
        super.render();
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
