package ee.eotv.echoes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music; // Lisatud Music import
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class SoundManager implements Disposable {
    private Sound stepSound;
    private Sound throwSound;
    private Sound hitSound;
    private Sound collectSound;
    private Sound doorSound;
    private Sound lightOnSound;
    private Sound lightOffSound;
    private Sound clickSound; // UUS: Click heli

    private Music menuMusic;  // UUS: Menüü muusika (Music tüüpi)

    public SoundManager() {
        // Üritame laadida helisid. Kui faile pole, püüame vea kinni.
        try {
            if (Gdx.files.internal("sounds/step.wav").exists())
                stepSound = Gdx.audio.newSound(Gdx.files.internal("sounds/step.wav"));

            if (Gdx.files.internal("sounds/throw.wav").exists())
                throwSound = Gdx.audio.newSound(Gdx.files.internal("sounds/throw.wav"));

            if (Gdx.files.internal("sounds/hit.wav").exists())
                hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.wav"));

            if (Gdx.files.internal("sounds/collect.wav").exists())
                collectSound = Gdx.audio.newSound(Gdx.files.internal("sounds/collect.wav"));

            if (Gdx.files.internal("sounds/door.wav").exists())
                doorSound = Gdx.audio.newSound(Gdx.files.internal("sounds/door.wav"));

            // --- Valguse helid ---
            if (Gdx.files.internal("sounds/light_on.wav").exists()) {
                lightOnSound = Gdx.audio.newSound(Gdx.files.internal("sounds/light_on.wav"));
            } else {
                System.out.println("VIGA: light_on.wav faili ei leitud!");
            }

            if (Gdx.files.internal("sounds/light_off.wav").exists()) {
                lightOffSound = Gdx.audio.newSound(Gdx.files.internal("sounds/light_off.wav"));
                System.out.println("OK: light_off.wav laeti edukalt.");
            } else {
                System.out.println("VIGA: light_off.wav faili ei leitud assets/sounds kaustast!");
            }

            // --- UUS: Click heli ---
            if (Gdx.files.internal("sounds/click.wav").exists()) {
                clickSound = Gdx.audio.newSound(Gdx.files.internal("sounds/click.wav"));
            }

            // --- UUS: Menüü muusika ---
            // Muusika laetakse newMusic käsuga
            if (Gdx.files.internal("sounds/menu_music.wav").exists()) {
                menuMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/menu_music.wav"));
            }

        } catch (Exception e) {
            System.out.println("HOIATUS: Helifailide laadimine ebaõnnestus (failid puudu?). " + e.getMessage());
        }
    }

    public void playStep() {
        playStep(0.3f);
    }

    public void playStep(float volume) {
        if (stepSound != null) {
            long id = stepSound.play(volume);
            stepSound.setPitch(id, MathUtils.random(0.9f, 1.1f));
        }
    }

    public void playThrow() {
        playThrow(1.0f);
    }

    public void playThrow(float volume) {
        if (throwSound != null) throwSound.play(volume);
    }

    public void playHit() {
        playHit(0.8f);
    }

    public void playHit(float volume) {
        if (hitSound != null) hitSound.play(volume);
    }

    public void playCollect() {
        playCollect(0.6f);
    }

    public void playCollect(float volume) {
        if (collectSound != null) collectSound.play(volume);
    }

    public void playDoor() {
        playDoor(1.0f);
    }

    public void playDoor(float volume) {
        if (doorSound != null) doorSound.play(volume);
    }

    public void playLightOn() {
        playLightOn(0.5f);
    }

    public void playLightOn(float volume) {
        if (lightOnSound != null) lightOnSound.play(volume);
    }

    public void playLightOff() {
        playLightOff(0.5f);
    }

    public void playLightOff(float volume) {
        if (lightOffSound != null) lightOffSound.play(volume);
    }

    // --- UUS: Click meetod ---
    public void playClick() {
        if (clickSound != null) clickSound.play(1.0f);
    }

    // --- UUS: Menüü muusika meetodid ---
    public void playMenuMusic() {
        if (menuMusic != null) {
            // Kontrollime, et muusika juba ei käiks, et vältida topeltkäivitamist
            if (!menuMusic.isPlaying()) {
                menuMusic.setLooping(true); // Paneb kordama
                menuMusic.setVolume(1f);  // Helitugevus (soovi korral muuda)
                menuMusic.play();
            }
        }
    }

    public void stopMenuMusic() {
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.stop();
        }
    }

    @Override
    public void dispose() {
        if (stepSound != null) stepSound.dispose();
        if (throwSound != null) throwSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (collectSound != null) collectSound.dispose();
        if (doorSound != null) doorSound.dispose();
        if (lightOnSound != null) lightOnSound.dispose();
        if (lightOffSound != null) lightOffSound.dispose();

        // UUS: Vabastame uued ressursid
        if (clickSound != null) clickSound.dispose();
        if (menuMusic != null) menuMusic.dispose();
    }
}
