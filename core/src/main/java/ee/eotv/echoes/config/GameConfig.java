package ee.eotv.echoes.config;

public final class GameConfig {
    private GameConfig() {}

    public static final class Camera {
        public static final float WIDTH = 40f;
        public static final float HEIGHT = 30f;
        private Camera() {}
    }

    public static final class Player {
        public static final float WALK_SPEED = 4.0f;
        public static final float RUN_SPEED = 8.0f;
        public static final float DOWNED_SPEED_MULTIPLIER = 0.25f;

        public static final float BODY_RADIUS = 0.4f;

        public static final int FLASHLIGHT_RAYS = 200;
        public static final float FLASHLIGHT_DISTANCE = 45f;
        public static final float FLASHLIGHT_CONE_DEGREES = 35f;

        public static final float MAX_STAMINA = 100f;
        public static final float EXHAUSTED_THRESHOLD = 25f;
        public static final float RUN_STAMINA_DRAIN = 30f;
        public static final float STAMINA_REGEN = 15f;

        public static final float RUN_STEP_INTERVAL = 0.3f;
        public static final float WALK_STEP_INTERVAL = 0.6f;
        public static final float RUN_ANIM_FRAME = 0.07f;
        public static final float WALK_ANIM_FRAME = 0.15f;
        public static final float RUN_ECHO_RADIUS = 32f;
        public static final float WALK_ECHO_RADIUS = 18f;

        public static final float MOVE_EPSILON = 0.1f;
        private Player() {}
    }

    public static final class Enemy {
        public static final float BODY_RADIUS = 0.35f;
        public static final float DRAW_WIDTH = 1.0f;
        public static final float DRAW_HEIGHT = 1.0f;
        public static final float DRAW_OFFSET_Y = 0.1f;

        public static final float PATROL_SPEED = 2.0f;
        public static final float INVESTIGATE_SPEED = 2.6f;
        public static final float CHASE_SPEED = 3.6f;
        public static final float VIEW_RANGE = 12f;
        public static final float ARRIVE_DISTANCE = 0.45f;
        public static final float PATROL_WAIT = 1.1f;
        public static final float INVESTIGATE_DURATION = 4.0f;
        public static final float CHASE_MEMORY_DURATION = 1.8f;
        private Enemy() {}
    }

    public static final class Revive {
        public static final float TIME_REQUIRED = 2.5f;
        public static final float RANGE = 1.8f;
        private Revive() {}
    }

    public static final class Generator {
        public static final float REPAIR_TIME = 10f;
        public static final float REPAIR_RANGE = 1.2f;
        private Generator() {}
    }

    public static final class Stone {
        public static final float MAX_POWER = 1.2f;
        public static final float CHARGE_RATE = 1.5f;
        public static final float TRAVEL_TIME = 0.6f;
        public static final float BODY_RADIUS = 0.1f;
        public static final float DENSITY = 2.0f;
        public static final float ECHO_RADIUS = 15f;
        private Stone() {}
    }

    public static final class Interaction {
        public static final float ITEM_PICKUP_RANGE = 0.8f;
        public static final float DOOR_OPEN_RANGE = 1.5f;
        public static final float CONSOLE_RANGE = 1.1f;
        public static final float SHADE_HIT_RANGE = 0.6f;
        private Interaction() {}
    }

    public static final class Level {
        public static final float TILE_SIZE = 1.0f;
        public static final float ECHO_FADE_SPEED = 15f;
        public static final float EXIT_LIGHT_RADIUS = 7f;
        private Level() {}
    }

    public static final class Ui {
        public static final float CCTV_MESSAGE_TIME = 2.5f;

        public static final float HUD_X_OFFSET = 18f;
        public static final float HUD_Y_OFFSET = 13f;
        public static final float HUD_STAMINA_WIDTH = 10f;
        public static final float HUD_STAMINA_HEIGHT = 0.8f;
        public static final float HUD_STONE_RADIUS = 0.3f;
        public static final float HUD_STONE_SPACING = 0.8f;

        public static final float REVIVE_TEXT_OFFSET_X = 70f;
        public static final float REVIVE_TEXT_Y = 40f;
        public static final float REVIVE_BAR_WIDTH = 140f;
        public static final float REVIVE_BAR_HEIGHT = 6f;
        public static final float REVIVE_BAR_Y = 22f;

        public static final float GENERATOR_TEXT_OFFSET_X = 90f;
        public static final float GENERATOR_TEXT_Y = 60f;
        public static final float GENERATOR_BAR_WIDTH = 160f;
        public static final float GENERATOR_BAR_HEIGHT = 6f;
        public static final float GENERATOR_BAR_Y = 42f;

        public static final float CCTV_TEXT_OFFSET_X = 70f;
        public static final float CCTV_TEXT_Y = 70f;
        private Ui() {}
    }
}
