package ee.eotv.echoes.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.world.LevelManager;
import java.util.ArrayList;
import java.util.Iterator;

public class StoneManager {
    public interface StoneImpactListener {
        void onStoneImpact(float x, float y, float radius, Color color);
    }

    public interface StoneThrowListener {
        void onStoneThrow(float x, float y);
    }

    private class Stone {
        Body body;
        float timeAlive = 0;
        // boolean echoed = false; // Seda pole enam vaja, sest kivi kaob kohe
        public Stone(Body b) { this.body = b; }
    }

    private World world;
    private LevelManager levelManager;
    private SoundManager soundManager;
    private ArrayList<Stone> stones = new ArrayList<>();
    private ShapeRenderer shapeRenderer;

    public float currentPower = 0f;
    public float maxPower = 1.2f;
    public boolean isCharging = false;
    private StoneImpactListener impactListener;
    private StoneThrowListener throwListener;

    public StoneManager(World world, LevelManager lm, SoundManager sm) {
        this.world = world;
        this.levelManager = lm;
        this.soundManager = sm;
    }

    public void setImpactListener(StoneImpactListener listener) {
        this.impactListener = listener;
    }

    public void setThrowListener(StoneThrowListener listener) {
        this.throwListener = listener;
    }

    public void handleInput(Player p, Camera cam) {
        if (!p.canThrowStones()) return;
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && p.ammo > 0) {
            isCharging = true;
            currentPower = Math.min(currentPower + 1.5f * Gdx.graphics.getDeltaTime(), maxPower);
        } else if (isCharging) {
            throwStone(p, cam, currentPower);
            if (throwListener != null) {
                throwListener.onStoneThrow(p.getPosition().x, p.getPosition().y);
            } else if (soundManager != null) {
                soundManager.playThrow();
            }
            p.ammo--;
            currentPower = 0;
            isCharging = false;
        }
    }

    public void renderTrajectory(Player player, Camera camera) {
        if (!isCharging) return;
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Vector2 startPos = player.getPosition();
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        Vector2 dir = new Vector2(mouse.x - startPos.x, mouse.y - startPos.y).nor();

        float distance = (currentPower / 0.0628f) * 0.6f;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int dots = 12;
        for (int i = 1; i <= dots; i++) {
            float step = (distance / dots) * i;
            float dotX = startPos.x + dir.x * (step + 0.7f);
            float dotY = startPos.y + dir.y * (step + 0.7f);

            float alpha = 1.0f - ((float)i / dots) * 0.5f;
            shapeRenderer.setColor(0.4f, 0.8f, 1f, alpha);
            float size = 0.12f - (i * 0.005f);
            shapeRenderer.circle(dotX, dotY, size, 8);
        }

        shapeRenderer.setColor(1, 1, 1, 0.8f);
        shapeRenderer.circle(startPos.x + dir.x * (distance + 0.7f), startPos.y + dir.y * (distance + 0.7f), 0.15f, 10);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void throwStone(Player p, Camera cam, float power) {
        Vector2 pos = p.getPosition();
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cam.unproject(mouse);
        Vector2 dir = new Vector2(mouse.x - pos.x, mouse.y - pos.y).nor();
        throwStoneAt(pos, dir, power);
    }

    public void throwStoneFromNetwork(Player p, float targetX, float targetY, float power) {
        if (!p.canThrowStones() || p.ammo <= 0) return;
        Vector2 pos = p.getPosition();
        Vector2 dir = new Vector2(targetX - pos.x, targetY - pos.y).nor();
        throwStoneAt(pos, dir, Math.min(power, maxPower));
        p.ammo--;
        if (throwListener != null) {
            throwListener.onStoneThrow(pos.x, pos.y);
        } else if (soundManager != null) {
            soundManager.playThrow();
        }
    }

    private void throwStoneAt(Vector2 pos, Vector2 dir, float power) {

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(pos.x + dir.x * 0.7f, pos.y + dir.y * 0.7f);
        def.bullet = true;

        Body b = world.createBody(def);
        b.setUserData("STONE");
        CircleShape s = new CircleShape(); s.setRadius(0.1f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = s;
        fdef.density = 2.0f;
        b.createFixture(fdef);
        s.dispose();

        b.applyLinearImpulse(dir.scl(power), b.getWorldCenter(), true);
        stones.add(new Stone(b));
    }

    public void update(float delta) {
        Iterator<Stone> iter = stones.iterator();
        while (iter.hasNext()) {
            Stone s = iter.next();
            s.timeAlive += delta;

            // Kui kivi on lennanud 0.6 sekundit (maandub)
            if (s.timeAlive > 0.6f) {
                // 1. Tee Echo (Heli + Valgus + Zombi tähelepanu)
                float echoX = s.body.getPosition().x;
                float echoY = s.body.getPosition().y;
                Color echoColor = new Color(0.4f, 0.7f, 1f, 1f);
                levelManager.addEcho(echoX, echoY, 15f, echoColor);
                if (impactListener == null && soundManager != null) soundManager.playHit();

                if (levelManager != null) {
                    for (ee.eotv.echoes.entities.Enemy enemy : levelManager.getEnemies()) {
                        if (enemy.isActive()) {
                            enemy.investigate(echoX, echoY);
                        }
                    }
                }
                if (impactListener != null) {
                    impactListener.onStoneImpact(echoX, echoY, 15f, echoColor);
                }

                // 2. MUUDA KOHE KORJATAVAKS ESEMEKS
                // Tekitame Item.Type.STONE täpselt sinna, kus kivi on
                levelManager.spawnItem(Item.Type.STONE, echoX, echoY);

                // 3. Kustutame lendava kivi füüsikakeha ja eemaldame nimekirjast
                world.destroyBody(s.body);
                iter.remove();
            }
        }
    }
}
