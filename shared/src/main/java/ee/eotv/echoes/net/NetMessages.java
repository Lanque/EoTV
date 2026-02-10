package ee.eotv.echoes.net;

public class NetMessages {
    public enum SoundType {
        STEP,
        THROW,
        HIT,
        COLLECT,
        DOOR,
        LIGHT_ON,
        LIGHT_OFF
    }

    public enum Role {
        FLASHLIGHT,
        STONES
    }

    public enum ItemType {
        STONE,
        KEYCARD
    }

    public static class JoinRequest {
        public Role role;
    }

    public static class JoinResponse {
        public boolean accepted;
        public String message;
        public int playerId;
        public Role role;
    }

    public static class CreateLobbyRequest {
        public Role preferredRole;
    }

    public static class JoinLobbyRequest {
        public int lobbyId;
        public Role preferredRole;
    }

    public static class LobbyJoinedResponse {
        public boolean success;
        public int lobbyId;
        public boolean isHost;
        public String message;
    }

    public static class StartGame {
        public int playerId;
        public Role role;
    }

    public static class InputState {
        public int playerId;
        public boolean up;
        public boolean down;
        public boolean left;
        public boolean right;
        public boolean run;
        public boolean toggleLight;
        public boolean revive;
        public float aimAngle;
    }

    public static class ThrowStone {
        public int playerId;
        public float targetX;
        public float targetY;
        public float power;
    }

    public static class WorldState {
        public long tick;
        public PlayerState[] players;
        public EnemyState[] enemies;
        public ItemState[] items;
        public DoorState[] doors;
        public GeneratorState[] generators;
        public boolean exitUnlocked;
        public boolean gameOver;
        public boolean victory;
    }

    public static class PlayerState {
        public int id;
        public float x;
        public float y;
        public float vx;
        public float vy;
        public boolean lightOn;
        public boolean running;
        public boolean moving;
        public float stamina;
        public int ammo;
        public boolean hasKeycard;
        public float aimAngle;
        public Role role;
        public boolean downed;
    }

    public static class EnemyState {
        public int index;
        public float x;
        public float y;
        public float vx;
        public float vy;
        public boolean facingRight;
        public boolean active;
    }

    public static class ItemState {
        public float x;
        public float y;
        public ItemType type;
    }

    public static class DoorState {
        public float x;
        public float y;
        public boolean open;
    }

    public static class GeneratorState {
        public int index;
        public float x;
        public float y;
        public boolean repaired;
        public float progress;
    }

    public static class EchoEvent {
        public float x;
        public float y;
        public float radius;
        public float r;
        public float g;
        public float b;
        public float a;
    }

    public static class SoundEvent {
        public SoundType type;
        public float x;
        public float y;
    }
}
