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
    public interface ActionListener {
        void onStep(Player player);
        void onLightToggle(Player player, boolean lightOn);
    }

    public enum Role {
        ALL,
        FLASHLIGHT,
        STONES
    }

    private Body body;
    private ConeLight flashlight;

    private float walkSpeed = 4.0f;
    private float runSpeed = 8.0f;
    public boolean isRunning = false;
    public boolean isMoving = false;
    public boolean isLightOn = true;
    private Role role = Role.ALL;
    private ActionListener actionListener;

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
    private boolean isDowned = false;
    private boolean isFrozen = false;

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
        if (rayHandler != null) {
            flashlight = new ConeLight(rayHandler, 200, new Color(1f, 1f, 0.9f, 1f), 45f, x, y, 0, 35f);
            flashlight.setSoft(true);
        } else {
            flashlight = null;
        }

        // --- TEXTURE/ANIMATION (skip in headless) ---
        if (Gdx.gl != null) {
            texture = new Texture(Gdx.files.internal("images/characters.png"));

            int frameWidth = 20;
            int frameHeight = 35;

            int startX = 9;
            int startY = 35;
            int padding = 12;

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
            idleFrame = new TextureRegion(texture, startX, startY, frameWidth, frameHeight);
        }

        stateTime = 0f;
    }

    public void render(SpriteBatch batch) {
        if (texture == null || runAnimation == null || idleFrame == null) return;
        stateTime += Gdx.graphics.getDeltaTime();

        // 1. Uuendame taskulambi asukohta ja suunda käsitsi
        // See tagab, et valgus püsib täpselt mängija peal ja pöörleb sujuvalt
        if (flashlight != null) {
            flashlight.setPosition(body.getPosition().x, body.getPosition().y);
            flashlight.setDirection(currentAngle);
        }

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
        updateMovementEffects(delta, lm, sm);
    }

    private void handleInput(Camera camera, float delta, SoundManager sm) {
        boolean toggleLight = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F);
        applyInput(buildInputFromDevice(camera, toggleLight), delta, sm);
    }

    public void updateFromInput(float delta, PlayerInput input, SoundManager sm, LevelManager lm) {
        applyInput(input, delta, sm);
        isMoving = body.getLinearVelocity().len() > 0.1f;
        updateMovementEffects(delta, lm, sm);
    }

    private PlayerInput buildInputFromDevice(Camera camera, boolean toggleLight) {
        PlayerInput input = new PlayerInput();
        input.toggleLight = toggleLight;
        input.wantsToRun = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        input.up = Gdx.input.isKeyPressed(Input.Keys.W);
        input.down = Gdx.input.isKeyPressed(Input.Keys.S);
        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angleRad = MathUtils.atan2(mouse.y - body.getPosition().y, mouse.x - body.getPosition().x);
        input.aimAngle = angleRad * MathUtils.radDeg;
        return input;
    }

    private void applyInput(PlayerInput input, float delta, SoundManager sm) {
        if (input == null) return;

        if (isFrozen) {
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.wantsToRun = false;
        }

        if (input.toggleLight && canUseFlashlight() && !isDowned) {
            isLightOn = !isLightOn;
            if (flashlight != null) {
                flashlight.setActive(isLightOn);
            }
            if (actionListener != null) {
                actionListener.onLightToggle(this, isLightOn);
            } else if (sm != null) {
                if (isLightOn) sm.playLightOn(); else sm.playLightOff();
            }
        }

        boolean wantsToRun = input.wantsToRun && !isDowned;

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

        float currentSpeed = (isRunning ? runSpeed : walkSpeed) * (isDowned ? 0.25f : 1f);
        float vx = 0, vy = 0;
        if (input.up) vy = currentSpeed;
        if (input.down) vy = -currentSpeed;
        if (input.left) vx = -currentSpeed;
        if (input.right) vx = currentSpeed;
        if (isFrozen) {
            body.setLinearVelocity(0, 0);
        } else {
            body.setLinearVelocity(vx, vy);
        }

        currentAngle = input.aimAngle;
    }

    private void updateMovementEffects(float delta, LevelManager lm, SoundManager sm) {
        if (isMoving) {
            stepTimer += delta;
            float currentInterval = isRunning ? 0.3f : 0.6f;

            if (runAnimation != null) {
                runAnimation.setFrameDuration(isRunning ? 0.07f : 0.15f);
            }

            if (stepTimer >= currentInterval) {
                float echoRadius = isRunning ? 32f : 18f;
                Color echoColor = isRunning ?
                    new Color(0.5f, 0.7f, 1f, 0.5f) :
                    new Color(0.4f, 0.5f, 0.8f, 0.5f);

                if (lm != null) {
                    lm.addEcho(body.getPosition().x, body.getPosition().y, echoRadius, echoColor);
                }
                if (actionListener != null) {
                    actionListener.onStep(this);
                } else if (sm != null) {
                    sm.playStep();
                }
                stepTimer = 0;
            }
        }
    }

    public Vector2 getPosition() { return body.getPosition(); }

    public Vector2 getVelocity() { return body.getLinearVelocity(); }

    public float getAimAngle() { return currentAngle; }

    public void setPosition(float x, float y) {
        body.setTransform(x, y, body.getAngle());
        if (flashlight != null) {
            flashlight.setPosition(x, y); // Uuendame ka valgust koheselt
        }
    }

    public void setNetworkState(float x, float y, float vx, float vy, boolean lightOn, boolean running,
                                boolean moving, float staminaValue, int ammoValue, boolean hasKeycardValue,
                                float aimAngle, Role roleValue, boolean downed) {
        body.setTransform(x, y, body.getAngle());
        body.setLinearVelocity(vx, vy);
        isLightOn = lightOn && canUseFlashlight(roleValue);
        if (flashlight != null) {
            flashlight.setActive(isLightOn);
        }
        isRunning = running;
        isMoving = moving;
        stamina = staminaValue;
        ammo = ammoValue;
        hasKeycard = hasKeycardValue;
        currentAngle = aimAngle;
        isDowned = downed;
        if (roleValue != null) {
            role = roleValue;
        }
    }

    public Role getRole() { return role; }

    public void setRole(Role role) {
        this.role = role;
        if (!canUseFlashlight()) {
            isLightOn = false;
            if (flashlight != null) {
                flashlight.setActive(false);
            }
        }
    }

    public boolean isDowned() { return isDowned; }

    public void setDowned(boolean downed) {
        isDowned = downed;
        if (isDowned) {
            isRunning = false;
            if (flashlight != null) {
                flashlight.setActive(false);
            }
            isLightOn = false;
        }
    }

    public void setFrozen(boolean frozen) {
        isFrozen = frozen;
        if (isFrozen) {
            body.setLinearVelocity(0, 0);
        }
    }

    public void setActionListener(ActionListener listener) {
        this.actionListener = listener;
    }

    public boolean canUseFlashlight() { return canUseFlashlight(role); }

    public boolean canThrowStones() { return !isDowned && (role == Role.ALL || role == Role.STONES); }

    private boolean canUseFlashlight(Role roleValue) {
        return roleValue == Role.ALL || roleValue == Role.FLASHLIGHT;
    }

    public static class PlayerInput {
        public boolean up;
        public boolean down;
        public boolean left;
        public boolean right;
        public boolean wantsToRun;
        public boolean toggleLight;
        public float aimAngle;
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}




