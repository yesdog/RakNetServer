package network.ycc.raknet.packet;

import io.netty.buffer.ByteBuf;

public abstract class SimplePacket implements Packet {

    abstract void encode(ByteBuf buf);
    abstract void decode(ByteBuf buf);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "()";
    }

    public void write(ByteBuf out) {
        out.writeByte(getPacketId());
        encode(out);
    }

    public int getPacketId() {
        return Packets.packetIdFor(getClass());
    }

}
