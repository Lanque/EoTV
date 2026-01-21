package ee.eotv.echoes.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    // --- ANIMATSIOONI MUUTUJAD ---
    private Texture texture;
    private Animation<TextureRegion> runAnimation;
    private TextureRegion idleFrame;
    private float stateTime;
    private boolean facingRight = true;

    // Hoiame nurka meeles, et uuendada valgust render tsüklis
    private float currentAngle = 0f;

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

        // VALGUS: Ei kinnita enam kehale (attachToBody puudub), et saaksime ise suunda ja asukohta määrata
        flashlight = new ConeLight(rayHandler, 200, new Color(1f, 1f, 0.9f, 1f), 45f, x, y, 0, 35f);
        flashlight.setSoft(true);

        // --- TEKSTUURID JA ANIMATSIOON (KÄSITSI LÕIKAMINE) ---
        texture = new Texture(Gdx.files.internal("images/characters.png"));

        // SIIN SAAD MUUTA KOORDINAATE, KUI PILT ON NIHES:
        int frameWidth = 20;  // Ühe kaadri laius
        int frameHeight = 35; // Ühe kaadri kõrgus

        // Rüütel on 2. reas. (1. rida on 0-15px, 2. rida algab 16px pealt)
        int startX = 9;
        int startY = 35;
        int padding = 12; // Kui piltide vahel on tühimik, suurenda seda (nt 1)

        TextureRegion[] walkFrames = new TextureRegion[4];

        for (int i = 0; i < 4; i++) {
            walkFrames[i] = new TextureRegion(
                texture,
                startX + (i * (frameWidth + padding)),
                startY,
                frameWidth,
                frameHeight
            );
        }

        runAnimation = new Animation<>(0.1f, walkFrames);
        // Seismise pilt on esimene kaader
        idleFrame = new TextureRegion(texture, startX, startY, frameWidth, frameHeight);

        stateTime = 0f;
    }

    public void render(SpriteBatch batch) {
        stateTime += Gdx.graphics.getDeltaTime();

        // 1. Uuendame taskulambi asukohta ja suunda käsitsi
        // See tagab, et valgus püsib täpselt mängija peal ja pöörleb sujuvalt
        flashlight.setPosition(body.getPosition().x, body.getPosition().y);
        flashlight.setDirection(currentAngle);

        // 2. Valime õige kaadri (animatsioon või seismine)
        TextureRegion currentFrame;
        if (isMoving) {
            currentFrame = runAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = idleFrame;
        }

        // 3. Pöörame pilti vastavalt liikumise suunale
        float velocityX = body.getLinearVelocity().x;
        if (velocityX < -0.1f) {
            facingRight = false;
        } else if (velocityX > 0.1f) {
            facingRight = true;
        }

        if (!facingRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (facingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        // 4. Joonistame (tsentreerime pildi keha asukohale, suurus 1x1 meetrit)
        batch.draw(currentFrame,
            body.getPosition().x - 0.5f,
            body.getPosition().y - 0.5f,
            1f, 1f);
    }

    public void collectItem(Item item) {
        if (item.getType() == Item.Type.STONE) {
            ammo++;
        } else if (item.getType() == Item.Type.KEYCARD) {
            hasKeycard = true;
        }
    }

    public void update(float delta, LevelManager lm, Camera camera, SoundManager sm) {
        handleInput(camera, delta, sm);

        isMoving = body.getLinearVelocity().len() > 0.1f;

        if (isMoving) {
            stepTimer += delta;
            float currentInterval = isRunning ? 0.3f : 0.6f;

            // Animatsioon kiiremaks, kui jookseb
            runAnimation.setFrameDuration(isRunning ? 0.07f : 0.15f);

            if (stepTimer >= currentInterval) {
                float echoRadius = isRunning ? 32f : 18f;

                // PARANDUS: Jooksmise ajal on kaja läbipaistvam (0.25f), et ei oleks liiga ere
                Color echoColor = isRunning ?
                    new Color(0.5f, 0.7f, 1f, 0.5f) :
                    new Color(0.4f, 0.5f, 0.8f, 0.5f);

                lm.addEcho(body.getPosition().x, body.getPosition().y, echoRadius, echoColor);
                if (sm != null) sm.playStep();
                stepTimer = 0;
            }
        }
    }

    private void handleInput(Camera camera, float delta, SoundManager sm) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            isLightOn = !isLightOn;
            flashlight.setActive(isLightOn);
            if (sm != null) {
                if (isLightOn) sm.playLightOn(); else sm.playLightOff();
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

        // Hiire loogika: arvutame nurga ja salvestame selle
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angleRad = MathUtils.atan2(mouse.y - body.getPosition().y, mouse.x - body.getPosition().x);

        // Konverteerime kraadideks ja salvestame klassimuutujasse
        currentAngle = angleRad * MathUtils.radDeg;
    }

    public Vector2 getPosition() { return body.getPosition(); }

    public void setPosition(float x, float y) {
        body.setTransform(x, y, body.getAngle());
        flashlight.setPosition(x, y); // Uuendame ka valgust koheselt
    }

    public void dispose() {
        texture.dispose();
    }
}
