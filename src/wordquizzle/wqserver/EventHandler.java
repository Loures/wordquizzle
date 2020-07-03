package wordquizzle.wqserver;

import wordquizzle.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


/**
 * The {@code EventHandler} class implements the parsing and dispatching of the various messages to the appropriate
 * {@code MessageHandler}.
 */
public class EventHandler {
	private ByteBuffer rbuff;
	private ByteBuffer wbuff;
	private Reactor reactor;
	private User user;
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
	 * Write data to the buffer.
	 * @param data the data to write.
	 */
	public synchronized void write(byte[] data) {
		try {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			wbuff.put(data);
		} catch (CancelledKeyException e) {/*silently fail*/}

		//Wake up the selector
		reactor.wakeup();
	}
	
	/**
	 * Write data to the buffer.
	 * @param data the data to write.
	 */
	public void write(String data) {
		write(new String(data + "\n").getBytes(StandardCharsets.UTF_8));
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
	 * Assigns a user to the EventHandler.
	 * @param user the user to assign to the EventHandler.
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Returns the user assigned to the EventHandler.
	 * @return the user assigned to the EventHandler
	 */
	public User getUser() {
		return this.user;
	}

	/**
	 * Returns the local address of the EventHandler's channel.
	 * @return the local address of the EventHandler's channel.
	 */
	public InetSocketAddress getLocalAddress() {
		try {

			return (InetSocketAddress)this.channel.getLocalAddress();
		} catch (IOException e) {};
		return null;
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
			
			//If the user was logged in log him out
			if (this.user != null) {
				this.user.logoutNoNotify();
				Logger.logInfo("User ", this.user.getName(), " disconnected");
			}

			Logger.logInfo(channel.getRemoteAddress(), " connection shutdown");
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

				MessageHandler msgHandler = MessageHandler.getHandler(this.user);
				if (sep > 0) msgHandler.startCompute(str.substring(0, sep), this);
				
				str = str.substring(sep + 1, str.length());
			}
		};
	}
}
