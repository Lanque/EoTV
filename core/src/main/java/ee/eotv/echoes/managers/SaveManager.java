package ee.eotv.echoes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import ee.eotv.echoes.entities.Player;

public class SaveManager {
    // See nimi "EchoesSave" on faili nimi, mis tekib arvutisse
    private static final String PREFS_NAME = "EchoesSave";

    public static void saveGame(Player player) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Salvestame andmed
        prefs.putFloat("posX", player.getPosition().x);
        prefs.putFloat("posY", player.getPosition().y);
        prefs.putInteger("ammo", player.ammo);
        prefs.putBoolean("hasKeycard", player.hasKeycard);

        // TÄHTIS: flush() kirjutab andmed päriselt faili!
        prefs.flush();

        System.out.println("MÄNG SALVESTATUD: X=" + player.getPosition().x + ", Y=" + player.getPosition().y);
    }

    public static void loadGame(Player player) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

        // Kontrollime, kas salvestus on olemas (kas X koordinaat on salvestatud)
        if (!prefs.contains("posX")) {
            System.out.println("Salvestust ei leitud!");
            return;
        }

        float x = prefs.getFloat("posX");
        float y = prefs.getFloat("posY");
        int ammo = prefs.getInteger("ammo");
        boolean hasKeycard = prefs.getBoolean("hasKeycard");

        // Rakendame andmed mängijale
        player.setPosition(x, y);
        player.ammo = ammo;
        player.hasKeycard = hasKeycard;

        System.out.println("MÄNG LAADITUD!");
    }

    // Kas meil on üldse salvestatud mängu?
    public static boolean hasSave() {
        return Gdx.app.getPreferences(PREFS_NAME).contains("posX");
    }
}
