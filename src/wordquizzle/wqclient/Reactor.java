package wordquizzle.wqclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.xml.crypto.Data;

import wordquizzle.Response;
import wordquizzle.UserState;

/**
 * Handles all UDP and TCP traffic on behalf of the client
 */
public class Reactor extends Thread {

	public static void handleRead(String msg) {
		Scanner scannerstub = new Scanner(msg);
		Scanner scanner = scannerstub.useDelimiter(":");
		String code = scanner.next();
		switch (code) {
			// SET_STATE handling
			case "SET_STATE":
				String newState = scanner.next();
				switch (newState) {
					case "OFFLINE":
						WQClient.state = UserState.OFFLINE;
						break;
					case "IDLE":
						WQClient.state = UserState.IDLE;
						break;
					case "CHALLENGE_ISSUED":
						WQClient.state = UserState.CHALLENGE_ISSUED;
						break;
					case "CHALLENGED":
						WQClient.state = UserState.CHALLENGED;
						break;
					case "IN_GAME":
						WQClient.state = UserState.IN_GAME;
						break;
				}
				break;
			case "WAITING_RESPONSE":
				System.out.println(Response.WAITINGRESPONSE.getResponse());
				break;
			case "ADDFRIEND_SUCCESS":
				String name1 = scanner.next();
				String name2 = scanner.next();
				System.out.print(Response.ADDFRIEND_SUCCESS.getResponse(name1, name2) + "\n> ");
				break;
			case "GAME_RESULT":
				int correct = scanner.nextInt();
				int wrong = scanner.nextInt();
				int points = scanner.nextInt();
				System.out.print(Response.GAME_RESULT.getResponse(correct, wrong, points) + "\n> ");
				break;
			default:
				try {
					//Parse response that requires only one argument
					String arg = scanner.next();
					System.out.print(Response.valueOf(code).getResponse(arg) + "\n> ");
				} catch (NoSuchElementException e) {
					try {
						//Response that requires no argument
						System.out.print(Response.valueOf(code).getResponse() + "\n> ");
					} catch (IllegalArgumentException e1) {
						//Everything else
						System.out.print(msg + "\n> ");
					}
				}
				break;
		}
	}
	
	private static ByteBuffer rbuff;
	private static ByteBuffer udp_rbuff;
	private static ByteBuffer wbuff;
	private static SelectionKey tcpkey;
	private static SelectionKey udpkey;
	private static Selector sel;
	private static SocketChannel tcpchannel;
	private static DatagramChannel udpchannel;
	private static Reactor reactor = null;

	private Reactor(InetSocketAddress addr) {
		try {
			sel = Selector.open();
			tcpchannel = SocketChannel.open(addr);
			tcpchannel.configureBlocking(false);
			tcpkey = tcpchannel.register(sel, SelectionKey.OP_READ);
			udpchannel = DatagramChannel.open();
			udpchannel.bind(new InetSocketAddress(addr.getAddress(), 0));
			udpchannel.configureBlocking(false);
			udpkey = udpchannel.register(sel, SelectionKey.OP_READ);
			rbuff = ByteBuffer.allocate(4096);
			wbuff = ByteBuffer.allocate(4096);
			udp_rbuff = ByteBuffer.allocate(4096);
		} catch (Exception e) {e.printStackTrace();}
	}

	public static Reactor getReactor(InetSocketAddress addr) {
		if (reactor == null) {
			reactor = new Reactor(addr);
			reactor.start();
		}
		return reactor;
	}

	public static Reactor getReactor() {
		return reactor;
	}

	public synchronized void write(byte[] data) {
		tcpkey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		wbuff.put(data);

		//Wake up the selector
		sel.wakeup();
	}
	
	/**
	 * Write data to the buffer.
	 * @param data the data to write.
	 */
	public void write(String data) {
		write(new String(data + "\n").getBytes(StandardCharsets.UTF_8));
	}

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