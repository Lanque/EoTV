package ee.eotv.echoes.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class Enemy {
    public Body body;
    private Vector2 targetPos;
    private Vector2 spawnPos;

    private enum State { PATROL, INVESTIGATE, CHASE }
    private State state = State.PATROL;

    private Array<Vector2> patrolPoints = new Array<>();
    private int patrolIndex = 0;
    private float patrolWaitTimer = 0f;

    private Vector2 investigateTarget = new Vector2();
    private float investigateTimer = 0f;

    private Vector2 lastSeenPos = new Vector2();
    private float chaseMemoryTimer = 0f;

    private float patrolSpeed = 2.0f;
    private float investigateSpeed = 2.6f;
    private float chaseSpeed = 3.6f;
    private float viewRange = 12f;
    private float arriveDistance = 0.45f;
    private float patrolWaitDuration = 1.1f;
    private float investigateDuration = 4.0f;
    private float chaseMemoryDuration = 1.8f;

    // --- GRAAFIKA MUUTUJAD ---
    private Texture texture;
    private Animation<TextureRegion> walkAnimation;
    private TextureRegion idleFrame;
    private float stateTime;
    private boolean facingRight = true;

    // Suurus maailmas (meetrites)
    private float width = 1.0f;
    private float height = 1.0f;

    // Nihutamine (kui pilt ei ole tÃ¤pselt kasti keskel)
    private float drawOffsetX = 0f;
    private float drawOffsetY = 0.1f; // TÃµstame natuke Ã¼les

    public Enemy(World world, float x, float y) {
        // 1. FÃœÃœSIKA (Sama mis enne)
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        def.fixedRotation = true;
        body = world.createBody(def);
        body.setUserData("ENEMY");

        CircleShape s = new CircleShape();
        s.setRadius(0.35f);
        body.createFixture(s, 1.0f);
        s.dispose();

        targetPos = new Vector2(x, y);
        spawnPos = new Vector2(x, y);

        // 2. TEKSTUURID JA ANIMATSIOON (TÃ¤pselt nagu Player klassis)
        texture = new Texture(Gdx.files.internal("images/characters.png"));

        // --- SIIN SAAD PIKSLEID MUUTA ---
        int frameWidth = 20;  // Ãœhe kaadri laius
        int frameHeight = 35; // Ãœhe kaadri kÃµrgus

        // ORANÅ½ TEGELANE on 1. reas (kÃµige Ã¼leval).
        // Playeril oli startY = 35. Enemy jaoks paneme vÃ¤iksema numbri.
        int startX = 9;
        int startY = 3;  // <--- MUUDA SEDA: 3 peaks olema Ã¼lemine rida
        int padding = 12; // Vahe piltide vahel

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

        walkAnimation = new Animation<>(0.15f, walkFrames);
        // Seismise pilt on esimene kaader
        idleFrame = new TextureRegion(texture, startX, startY, frameWidth, frameHeight);

        stateTime = 0f;

        if (patrolPoints.size > 0) {
            targetPos.set(patrolPoints.get(0));
        }
    }

    public void investigate(float x, float y) {
        investigateTarget.set(x, y);
        investigateTimer = investigateDuration;
        state = State.INVESTIGATE;
    }

    public void update(Player player, float delta) {
        updateState(player, delta);

        Vector2 myPos = body.getPosition();
        float distToTarget = myPos.dst(targetPos);

        if (distToTarget > arriveDistance) {
            Vector2 dir = new Vector2(targetPos.x - myPos.x, targetPos.y - myPos.y);
            float speed = getSpeedForState();
            body.setLinearVelocity(dir.nor().scl(speed));

            // Suuna mÃ¤Ã¤ramine
            if (dir.x > 0) facingRight = true;
            if (dir.x < 0) facingRight = false;
        } else {
            body.setLinearVelocity(0, 0);
            onArrived(delta);
        }
    }

    // --- JOONISTAMINE ---
    public void render(SpriteBatch batch) {
        stateTime += Gdx.graphics.getDeltaTime();

        TextureRegion currentFrame;

        // Kui liigub, kasuta animatsiooni, muidu idleFrame
        if (body.getLinearVelocity().len() > 0.1f) {
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = idleFrame;
        }

        // PÃ¶Ã¶ramine (Flip)
        if (!facingRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (facingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        // Joonistame
        float drawX = body.getPosition().x - (width / 2) + drawOffsetX;
        float drawY = body.getPosition().y - (height / 2) + drawOffsetY;

        batch.draw(currentFrame, drawX, drawY, width, height);
    }

    public Vector2 getPosition() { return body.getPosition(); }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    public void setNetworkState(float x, float y, float vx, float vy, boolean facingRightValue) {
        body.setTransform(x, y, body.getAngle());
        body.setLinearVelocity(vx, vy);
        facingRight = facingRightValue;
    }

    public void setPatrolPoints(Array<Vector2> points) {
        patrolPoints.clear();
        if (points != null && points.size > 0) {
            for (Vector2 point : points) {
                patrolPoints.add(new Vector2(point));
            }
            patrolIndex = 0;
            targetPos.set(patrolPoints.get(0));
        } else {
            patrolPoints.add(new Vector2(spawnPos));
            patrolIndex = 0;
            targetPos.set(spawnPos);
        }
    }

    private void updateState(Player player, float delta) {
        Vector2 myPos = body.getPosition();
        Vector2 playerPos = player.getPosition();
        float distToPlayer = myPos.dst(playerPos);

        boolean playerEmittingLight = player.isLightOn || (player.isRunning && player.isMoving);
        boolean canSeePlayer = playerEmittingLight && distToPlayer <= viewRange && hasLineOfSight(playerPos);

        if (canSeePlayer) {
            state = State.CHASE;
            lastSeenPos.set(playerPos);
            chaseMemoryTimer = chaseMemoryDuration;
            targetPos.set(playerPos);
            return;
        }

        if (state == State.CHASE) {
            chaseMemoryTimer -= delta;
            if (chaseMemoryTimer <= 0f) {
                investigateTarget.set(lastSeenPos);
                investigateTimer = investigateDuration;
                state = State.INVESTIGATE;
            } else {
                targetPos.set(lastSeenPos);
            }
            return;
        }

        if (state == State.INVESTIGATE) {
            investigateTimer -= delta;
            if (investigateTimer <= 0f) {
                state = State.PATROL;
                patrolWaitTimer = 0f;
            } else {
                targetPos.set(investigateTarget);
            }
            return;
        }

        if (state == State.PATROL) {
            if (patrolPoints.size > 0) {
                targetPos.set(patrolPoints.get(patrolIndex));
            } else {
                targetPos.set(spawnPos);
            }
        }
    }

    private void onArrived(float delta) {
        if (state == State.PATROL) {
            patrolWaitTimer += delta;
            if (patrolWaitTimer >= patrolWaitDuration) {
                patrolWaitTimer = 0f;
                if (patrolPoints.size > 0) {
                    patrolIndex = (patrolIndex + 1) % patrolPoints.size;
                    targetPos.set(patrolPoints.get(patrolIndex));
                }
            }
            return;
        }

        if (state == State.INVESTIGATE) {
            investigateTimer -= delta;
            if (investigateTimer <= 0f) {
                state = State.PATROL;
                patrolWaitTimer = 0f;
            }
        }
    }

    private float getSpeedForState() {
        switch (state) {
            case CHASE:
                return chaseSpeed;
            case INVESTIGATE:
                return investigateSpeed;
            case PATROL:
            default:
                return patrolSpeed;
        }
    }

    private boolean hasLineOfSight(Vector2 target) {
        final float[] closestPlayer = {Float.MAX_VALUE};
        final float[] closestBlocker = {Float.MAX_VALUE};
        World world = body.getWorld();
        Vector2 from = body.getPosition();

        world.rayCast((fixture, point, normal, fraction) -> {
            Body hitBody = fixture.getBody();
            if (hitBody == body) return -1f;

            Object data = hitBody.getUserData();
            if ("PLAYER".equals(data)) {
                if (fraction < closestPlayer[0]) closestPlayer[0] = fraction;
                return 1f;
            }

            if (hitBody.getType() == BodyDef.BodyType.StaticBody && hitBody.isActive()) {
                if (fraction < closestBlocker[0]) closestBlocker[0] = fraction;
            }

            return 1f;
        }, from, target);

        return closestPlayer[0] < closestBlocker[0];
    }

}
