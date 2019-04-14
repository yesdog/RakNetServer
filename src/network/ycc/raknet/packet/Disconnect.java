package network.ycc.raknet.packet;

import io.netty.buffer.ByteBuf;

public class Disconnect extends SimpleFramedPacket {

    public Disconnect() {
        reliability = Reliability.RELIABLE;
    }

    @Override
    public void decode(ByteBuf buf) {
    }

    @Override
    public void encode(ByteBuf buf) {
    }

}
