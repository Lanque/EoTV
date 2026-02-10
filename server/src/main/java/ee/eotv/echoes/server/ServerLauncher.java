package ee.eotv.echoes.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class ServerLauncher {
    public static void main(String[] args) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Serveri "FPS" - kui tihti update() jookseb (60 korda sekundis on hea füüsika jaoks)
        config.updatesPerSecond = 60;

        // Käivitame DedicatedServeri
        new HeadlessApplication(new DedicatedServer(), config);
    }
}
