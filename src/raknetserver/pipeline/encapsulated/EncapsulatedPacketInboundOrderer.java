package raknetserver.pipeline.encapsulated;

import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import raknetserver.packet.EncapsulatedPacket;
import raknetserver.utils.Constants;
import raknetserver.utils.UINT;

public class EncapsulatedPacketInboundOrderer extends MessageToMessageDecoder<EncapsulatedPacket> {

	private final OrderedChannelPacketQueue[] channels = new OrderedChannelPacketQueue[8];
	{
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new OrderedChannelPacketQueue();
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, EncapsulatedPacket packet, List<Object> list) {
		if (packet.getReliability() == 3) {
			channels[packet.getOrderChannel()].getOrdered(packet, list);
		} else {
			list.add(Unpooled.wrappedBuffer(packet.getData()));
		}
	}

	protected static class OrderedChannelPacketQueue {

		protected final Int2ObjectOpenHashMap<EncapsulatedPacket> queue = new Int2ObjectOpenHashMap<>();
		protected int lastReceivedIndex = -1;
		{
			queue.defaultReturnValue(null);
		}

		protected void getOrdered(EncapsulatedPacket packet, List<Object> list) {
			final int orderIndex = packet.getOrderIndex();
			final int indexDiff = UINT.B3.minusWrap(orderIndex, lastReceivedIndex);
			if (indexDiff == 1) { //got next packet in line
				lastReceivedIndex = orderIndex;
				list.add(Unpooled.wrappedBuffer(packet.getData()));
				int nextIndex = UINT.B3.plus(orderIndex, 1);
				EncapsulatedPacket nextPacket = queue.get(nextIndex);
				while (nextPacket != null) { //process next packets in line if queued
					list.add(Unpooled.wrappedBuffer(nextPacket.getData()));
					lastReceivedIndex = nextIndex;
					queue.remove(nextIndex);
					nextIndex = UINT.B3.plus(nextIndex, 1);
					nextPacket = queue.get(nextIndex);
				}
			} else if (indexDiff > 1) { // future data goes in the queue
				queue.put(orderIndex, packet);
			}
			if (queue.size() > Constants.MAX_PACKET_LOSS) {
				throw new DecoderException("Too big packet loss (missed ordered packets)");
			}
		}

	}

}
