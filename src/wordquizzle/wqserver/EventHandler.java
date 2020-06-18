package wordquizzle.wqserver;

import wordquizzle.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


/**
 * The {@code EventHandler} abstract class describes how TCP reading/wriging is handled inside a reactor.
 */
public abstract class EventHandler {
	private ByteBuffer rbuff;
	private ByteBuffer wbuff;
	private Reactor reactor;
	protected SelectionKey key;
	protected SocketChannel channel;
	
	/**
	 * Constructs the event handler.
	 * @param key the selection key to which the event handler is assigned.
	 */
	public EventHandler(SelectionKey key) {
		this.rbuff = ByteBuffer.allocate(4096);
		this.wbuff = ByteBuffer.allocate(4096);
		this.key = key;
		this.channel = (SocketChannel)key.channel();
	}

	/**
	 * Constructs the event handler whilst carrying over the previous event handler's write buffer.
	 * @param key      the selection key to which the event handler is assigned.
	 * @param prevbuff the previous event handler's write buffer.
	 */
	public EventHandler(SelectionKey key, ByteBuffer prevbuff) {
		this.rbuff = ByteBuffer.allocate(4096);
		this.wbuff = prevbuff;
		this.key = key;
		this.channel = (SocketChannel)key.channel();
	}

	/**
	 * Compute the received message
	 * @param msg the received message
	 */
	protected abstract void compute(String msg);

	/**
	 * Write data to the buffer.
	 * @param data the data to write.
	 */
	public synchronized void write(byte[] data) {
		key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		wbuff.put(data);

		//Wake up the selector
		reactor.getSelector().wakeup();
	}
	
	/**
	 * Write data to the buffer.
	 * @param data the data to write.
	 */
	public void write(String data) {
		write(new String(data + "\n").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * returns the handler's write buffer.
	 * @return the handler's write buffer.
	 */
	public ByteBuffer getWBuffer() {
		return wbuff.duplicate();
	}

	/**
	 * Assign the handler to the specified reactor.
	 * @param reactor the reactor to assign the handler to.
	 */
	public void registerHandler(Reactor reactor) {
		this.reactor = reactor;
		key.attach(this);
	}

	/**
	 * Returns the reactor assigned to the handler.
	 * @return the reactor assigned to the handler.
	 */
	public Reactor getReactor() {
		return this.reactor;
	}

	/**
	 * Empties the write buffer inside the socket.
	 * @throws IOException
	 */
	public void send() throws IOException {
		if (wbuff.position() > 0) {
			wbuff.flip();
			channel.write(wbuff);
			wbuff.compact();
		} else key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * Handle the received TCP segment and parse it accordingly.
	 * @throws IOException
	 */
	public void handle() throws IOException {
		//shutdown the connection
		if(channel.read(rbuff) < 0) {
			//If the user is logged in then log him out.
			if (this instanceof LoggedInEventHandler) {
				LoggedInEventHandler LoggedInEventHandler = (LoggedInEventHandler)this;
				LoggedInEventHandler.getUser().logout();
			}

			Logger.logInfo("Bye ", channel.getRemoteAddress());
			reactor.removeChannel(channel);
			channel.close();
			key.cancel();
			return;
		}
		//check if the received data ends with '\n' (0x0A)
		else if (rbuff.position() > 0 && rbuff.get(rbuff.position() - 1) == (byte)0x0A) {
			rbuff.flip();
			byte[] data = new byte[rbuff.limit()];
			rbuff.get(data, 0, rbuff.limit());
			rbuff.clear();
			String str = new String(data);
			
			//parse (split) the segment's content
			while (!str.isEmpty()) {
				int sep = str.indexOf("\n");

				//the key's EventHandler could've changed in the meantime
				EventHandler evh = (EventHandler)key.attachment();
				if (sep > 0) evh.compute(str.substring(0, sep));
				
				str = str.substring(sep + 1, str.length());
			}
		};
	}
}
