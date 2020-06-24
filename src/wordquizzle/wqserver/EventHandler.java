package wordquizzle.wqserver;

import wordquizzle.Logger;
import wordquizzle.Response;
import wordquizzle.UserState;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


/**
 * The {@code EventHandler} abstract class describes how TCP reading/wriging is handled inside a reactor.
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

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return this.user;
	}

	public InetSocketAddress getLocalAddress() {
		try {

			return (InetSocketAddress)this.channel.getLocalAddress();
		} catch (IOException e) {};
		return null;
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
			
			//If the user was logged in log him out
			if (this.user != null) {
				if (this.user.getState() == UserState.CHALLENGE_ISSUED)
					this.user.getChallenge().abortChallenge(this.user);
				this.user.logout();
			}

			Logger.logInfo(channel.getRemoteAddress(), " disconnected");
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
				MessageHandler msgHandler = MessageHandler.getHandler(this.user);
				if (sep > 0) msgHandler.startCompute(str.substring(0, sep), this);
				
				str = str.substring(sep + 1, str.length());
			}
		};
	}
}
