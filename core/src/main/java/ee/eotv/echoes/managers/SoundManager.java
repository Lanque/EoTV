package ee.eotv.echoes.managers;

import com.badlogic.gdx.Gdx;
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

    public SoundManager() {
        // Üritame laadida helisid. Kui faile pole, püüame vea kinni, et mäng kokku ei jookseks.
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

        } catch (Exception e) {
            System.out.println("HOIATUS: Helifailide laadimine ebaõnnestus (failid puudu?).");
        }
    }

    public void playStep() {
        if (stepSound != null) {
            long id = stepSound.play(0.3f);
            stepSound.setPitch(id, MathUtils.random(0.9f, 1.1f));
        }
    }

    public void playThrow() {
        if (throwSound != null) throwSound.play(1.0f);
    }

    public void playHit() {
        if (hitSound != null) hitSound.play(0.8f);
    }

    public void playCollect() {
        if (collectSound != null) collectSound.play(0.6f);
    }

    public void playDoor() {
        if (doorSound != null) doorSound.play(1.0f);
    }

    public void playLightOn() {
        if (lightOnSound != null) lightOnSound.play(0.5f);
    }

    public void playLightOff() {
        // DEBUG PRINT
        if (lightOffSound != null) {
            lightOffSound.play(0.5f);
            System.out.println("DEBUG: Mängin light_off heli.");
        } else {
            System.out.println("VIGA: playLightOff kutsuti, aga heli on NULL (faili ei laetud).");
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
    }
}
