package network.ycc.raknet.pipeline;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import network.ycc.raknet.client.pipeline.ConnectionInitializer;
import network.ycc.raknet.packet.Packet;
import network.ycc.raknet.packet.Ping;

import java.util.concurrent.TimeUnit;

public abstract class AbstractConnectionInitializer extends SimpleChannelInboundHandler<Packet> {
    public static final String NAME = "rn-init-connect";

    protected final ChannelPromise connectPromise;
    protected ConnectionInitializer.State state = ConnectionInitializer.State.CR1;
    protected ScheduledFuture<?> sendTimer = null;
    protected ScheduledFuture<?> connectTimer = null;

    public AbstractConnectionInitializer(ChannelPromise connectPromise) {
        this.connectPromise = connectPromise;
    }

    abstract public void sendRequest(ChannelHandlerContext ctx);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        sendTimer = ctx.channel().eventLoop().scheduleAtFixedRate(() -> sendRequest(ctx),
                250, 250, TimeUnit.MILLISECONDS);
        connectTimer = ctx.channel().eventLoop().schedule(this::doTimeout,
                ctx.channel().config().getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
        sendRequest(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        sendTimer.cancel(false);
        connectTimer.cancel(false);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        fail(cause);
    }

    protected void finish(ChannelHandlerContext ctx) {
        connectPromise.trySuccess();
        ctx.pipeline().remove(this);
        ctx.pipeline().fireChannelActive();
        final ScheduledFuture<?> pingTask = ctx.channel().eventLoop().scheduleAtFixedRate(
                () -> ctx.channel().writeAndFlush(new Ping()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE),
                100, 250, TimeUnit.MILLISECONDS
        );
        ctx.channel().closeFuture().addListener(x -> pingTask.cancel(false));
    }

    protected void fail(Throwable cause) {
        connectPromise.tryFailure(cause);
    }

    protected void doTimeout() {
        fail(new ConnectTimeoutException());
    }

    protected enum State {
        CR1, //UDP: ConnectionRequest1 -> ConnectionReply1, InvalidVersion
        CR2, //UDP: ConnectionRequest2 -> ConnectionReply2, ConnectionFailed
        CR3, //Framed: ConnectionRequest -> Handshake -> ClientHandshake
    }
}
