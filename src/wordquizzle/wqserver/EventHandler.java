package wordquizzle.wqserver;

import wordquizzle.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class EventHandler {
    private ByteBuffer rbuff;
    private ByteBuffer wbuff;
    private Reactor reactor;
    protected SelectionKey key;
    protected SocketChannel channel;

    public EventHandler(SelectionKey key) {
        this.rbuff = ByteBuffer.allocate(2048);
        this.wbuff = ByteBuffer.allocate(2048);
        this.key = key;
        this.channel = (SocketChannel)key.channel();
    }
    
    protected abstract void compute(String msg);

    protected void write(byte[] data) {
        wbuff.put(data);
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void registerHandler(Reactor reactor) {
        this.reactor = reactor;
    }

    public Reactor getReactor() {
        return this.reactor;
    }

    public void send() throws IOException {
        if (wbuff.position() > 0) {
            wbuff.flip();
            channel.write(wbuff);
            wbuff.compact();
            if (wbuff.position() == 0) key.interestOps(SelectionKey.OP_READ);
        } 
    }

    public void handle() throws IOException {
        if(channel.read(rbuff) < 0) {
            Logger.logInfo("Bye ", channel.getRemoteAddress());
            reactor.removeChannel(channel);
            channel.close();
            key.cancel();
            return;
        } else if (rbuff.position() > 0 && rbuff.get(rbuff.position() - 1) == (byte)0x0A) {
            byte[] data = new byte[2048];
            rbuff.flip();
            rbuff.get(data, 0, rbuff.limit());
            rbuff.clear();
            compute(new String(data));
        };
    }
}