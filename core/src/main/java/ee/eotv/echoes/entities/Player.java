package ee.eotv.echoes.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.ConeLight;
import box2dLight.RayHandler;
import ee.eotv.echoes.world.LevelManager;
import ee.eotv.echoes.managers.SoundManager;

public class Player {
    private Body body;
    private ConeLight flashlight;

    private float walkSpeed = 4.0f;
    private float runSpeed = 8.0f;
    public boolean isRunning = false;
    public boolean isMoving = false;
    public boolean isLightOn = true;

    // --- STAMINA ---
    public float maxStamina = 100f;
    public float stamina = 100f;
    private boolean isExhausted = false;

    // --- INVENTAR ---
    public int ammo = 3;
    public boolean hasKeycard = false;

    private float stepTimer = 0;

    public Player(World world, RayHandler rayHandler, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.fixedRotation = true;
        body = world.createBody(bodyDef);
        body.setUserData("PLAYER");

        CircleShape shape = new CircleShape();
        shape.setRadius(0.4f);
        body.createFixture(shape, 1.0f);
        shape.dispose();

        flashlight = new ConeLight(rayHandler, 200, new Color(1f, 1f, 0.9f, 1f), 45f, x, y, 0, 35f);
        flashlight.attachToBody(body);
        flashlight.setSoft(true);
    }

    public void collectItem(Item item) {
        if (item.getType() == Item.Type.STONE) {
            ammo++;
        } else if (item.getType() == Item.Type.KEYCARD) {
            hasKeycard = true;
        }
    }

    public void update(float delta, LevelManager lm, Camera camera, SoundManager sm) {
        // Saadame nüüd SoundManageri edasi ka handleInput meetodisse!
        handleInput(camera, delta, sm);

        isMoving = body.getLinearVelocity().len() > 0.1f;

        if (isMoving) {
            stepTimer += delta;
            float currentInterval = isRunning ? 0.3f : 0.6f;

            if (stepTimer >= currentInterval) {
                float echoRadius = isRunning ? 32f : 18f;
                Color echoColor = isRunning ?
                    new Color(0.5f, 0.7f, 1f, 0.7f) :
                    new Color(0.4f, 0.5f, 0.8f, 0.5f);

                lm.addEcho(body.getPosition().x, body.getPosition().y, echoRadius, echoColor);

                if (sm != null) sm.playStep();

                stepTimer = 0;
            }
        }
    }

    // --- UUENDATUD: handleInput võtab nüüd SoundManageri vastu ---
    private void handleInput(Camera camera, float delta, SoundManager sm) {
        // Taskulamp
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            isLightOn = !isLightOn;
            flashlight.setActive(isLightOn);

            // --- UUS: Mängime klõpsu heli ---
            if (sm != null) {
                if (isLightOn) {
                    sm.playLightOn();
                } else {
                    sm.playLightOff();
                }
            }
        }

        boolean wantsToRun = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        if (isExhausted) {
            if (stamina > 25f) isExhausted = false;
            wantsToRun = false;
        }

        if (wantsToRun && stamina > 0 && isMoving) {
            isRunning = true;
            stamina -= delta * 30f;
            if (stamina <= 0) {
                stamina = 0;
                isExhausted = true;
            }
        } else {
            isRunning = false;
            if (stamina < maxStamina) {
                stamina += delta * 15f;
                if (stamina > maxStamina) stamina = maxStamina;
            }
        }

        float currentSpeed = isRunning ? runSpeed : walkSpeed;
        float vx = 0, vy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) vy = currentSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vy = -currentSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) vx = -currentSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) vx = currentSpeed;
        body.setLinearVelocity(vx, vy);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angle = MathUtils.atan2(mouse.y - body.getPosition().y, mouse.x - body.getPosition().x);
        body.setTransform(body.getPosition(), angle);
    }

    public Vector2 getPosition() { return body.getPosition(); }

    // See meetod tõstab mängija jõuga uude kohta (laadimisel)
    public void setPosition(float x, float y) {
        body.setTransform(x, y, body.getAngle());
    }
}


