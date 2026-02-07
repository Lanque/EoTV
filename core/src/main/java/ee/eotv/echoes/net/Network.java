package ee.eotv.echoes.net;

import com.esotericsoftware.kryo.Kryo;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;

public class Network {
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    public static void register(Kryo kryo) {
        kryo.register(NetMessages.JoinRequest.class);
        kryo.register(NetMessages.JoinResponse.class);
        kryo.register(NetMessages.StartGame.class);
        kryo.register(NetMessages.InputState.class);
        kryo.register(NetMessages.ThrowStone.class);
        kryo.register(NetMessages.WorldState.class);
        kryo.register(NetMessages.PlayerState.class);
        kryo.register(NetMessages.PlayerState[].class);
        kryo.register(NetMessages.EnemyState.class);
        kryo.register(NetMessages.EnemyState[].class);
        kryo.register(NetMessages.ItemState.class);
        kryo.register(NetMessages.ItemState[].class);
        kryo.register(NetMessages.DoorState.class);
        kryo.register(NetMessages.DoorState[].class);
        kryo.register(NetMessages.GeneratorState.class);
        kryo.register(NetMessages.GeneratorState[].class);
        kryo.register(NetMessages.EchoEvent.class);
        kryo.register(NetMessages.SoundEvent.class);
        kryo.register(NetMessages.SoundType.class);
        kryo.register(Player.Role.class);
        kryo.register(Item.Type.class);
        kryo.register(String.class);
    }
}
