package wordquizzle.wqclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * The {@code Reactor} abstract class handles all UDP and TCP traffic on behalf of the client
 */
public abstract class Reactor extends Thread {

	//What to do with received messages
	public abstract void handleRead(String msg);
	
	private static ByteBuffer rbuff;
	private static ByteBuffer udp_rbuff;
	private static ByteBuffer wbuff;
	private static SelectionKey tcpkey;
	private static SelectionKey udpkey;
	private static Selector sel;
	private static SocketChannel tcpchannel;
	private static DatagramChannel udpchannel;
	protected static Reactor reactor = null;

	protected Reactor(InetSocketAddress addr) {
		try {
			//Create the selector, the TCP and UDP sockets and the relevant buffers
			sel = Selector.open();
			tcpchannel = SocketChannel.open();
			tcpchannel.connect(addr);
			if (tcpchannel.isConnected()) System.out.println("Connected to WordQuizzle server.");
			tcpchannel.configureBlocking(false);
			tcpkey = tcpchannel.register(sel, SelectionKey.OP_READ);
			udpchannel = DatagramChannel.open();
			udpchannel.bind(new InetSocketAddress("0.0.0.0", 0));
			udpchannel.configureBlocking(false);
			udpkey = udpchannel.register(sel, SelectionKey.OP_READ);
			rbuff = ByteBuffer.allocate(4096);
			wbuff = ByteBuffer.allocate(4096);
			udp_rbuff = ByteBuffer.allocate(4096);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}
	}

	public static Reactor getReactor() {
		return reactor;
	}

	/**
	 * Write a byte array to the TCP socket.
	 * @param data the byte array to write.
	 */
	public synchronized void write(byte[] data) {
		try {
			tcpkey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			wbuff.put(data);
		} catch (CancelledKeyException e) {/*silently fail*/}

		//Wake up the selector
		sel.wakeup();
	}
	
	/**
	 * Write a string to the buffer.
	 * @param data the string to write.
	 */
	public void write(String data) {
		write(new String(data + "\n").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Get the UDP port picked randomply by the bind method.
	 * @return the UDP port.
	 */
	public int getUDPPort() {
		try {
			return ((InetSocketAddress)udpchannel.getLocalAddress()).getPort();
		} catch (IOException e) {e.printStackTrace();}
		return -1;
	}

	private void send() throws IOException {
		if (wbuff.position() > 0) {
			wbuff.flip();
			tcpchannel.write(wbuff);
			wbuff.compact();
		} else tcpkey.interestOps(SelectionKey.OP_READ);
	}

	private void handle(SelectableChannel channel, ByteBuffer rbuff) throws IOException {
		int read = 0;
		if (channel instanceof DatagramChannel) {
			((DatagramChannel)channel).receive(rbuff);
		}
		else read = ((SocketChannel)channel).read(rbuff);
		if (read < 0) {
			//Logger.logInfo("Bye ", channel.getRemoteAddress());
			channel.close();
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

				handleRead(str.substring(0, sep));
				
				str = str.substring(sep + 1, str.length());
			}
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				sel.select();
				if (tcpkey.isWritable()) send();
				if (tcpkey.isReadable()) handle(tcpchannel, rbuff);
				if (udpkey.isReadable()) handle(udpchannel, udp_rbuff);
				sel.selectedKeys().clear();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
				return;
			} catch (ClosedSelectorException | CancelledKeyException e) {/*discard*/}
		}
	}

	/**
	* Closes the handler and all its registered channels
	*/
	public void close() {
		try {
			for (SelectionKey key : sel.keys()) key.cancel();
			sel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}