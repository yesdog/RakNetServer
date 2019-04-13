package network.ycc.raknet.packet;

import io.netty.buffer.ByteBuf;
import network.ycc.raknet.utils.DataSerializer;

public class ConnectionRequest2 extends SimplePacket implements Packet {

    private int mtu;
    private long guid;

    public ConnectionRequest2() {}

    public ConnectionRequest2(int mtu, long guid) {
        this.mtu = mtu;
        this.guid = guid;
    }

    @Override
    public void decode(ByteBuf buf) {
        DataSerializer.readMagic(buf);
        DataSerializer.readAddress(buf);
        mtu = buf.readShort();
        guid = buf.readLong();
    }

    @Override
    public void encode(ByteBuf buf) {
        DataSerializer.writeMagic(buf);
        DataSerializer.writeAddress(buf);
        buf.writeShort(mtu);
        buf.writeLong(guid);
    }

    public int getMtu() {
        return mtu;
    }

    public long getGUID() {
        return guid;
    }

}
