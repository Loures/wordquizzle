package wordquizzle.wqserver;

import wordquizzle.Logger;
import java.nio.channels.*;
import java.io.IOException;
import java.net.*;

/**
 * This class implements the TCP connection acceptor as a Singleton.
 */
public class Acceptor {

	private static Acceptor acceptor;
	private ServerSocketChannel serverChannel;
	private Selector selector;

	private Acceptor() {}
	
	/**
	 * Return the acceptor singleton
	 * @return connection acceptor
	 */
	public synchronized static Acceptor getAcceptor() {
		if (acceptor == null) acceptor = new Acceptor();
		return acceptor;
	}

	/**
	 * Start listening for TCP connections
	 * @param port the port to bind to
	 */
	public void start(final int port) {
		
		//Open the listening socket
		try {
			serverChannel = ServerSocketChannel.open();
			final ServerSocket ss = serverChannel.socket();
			ss.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			Logger.logErr(e.toString());
			e.printStackTrace();
			System.exit(1);
			return;
		};

		//Wait for incoming connections
		while (!Thread.interrupted()) {
			try {
				selector.select();
				for (SelectionKey key : selector.selectedKeys()) {
					if (key.isAcceptable()) {
						final ServerSocketChannel server = (ServerSocketChannel) key.channel();
						final SocketChannel client = server.accept();
						client.configureBlocking(false);
						Logger.logInfo("Accepted connection from ", client.getLocalAddress());
						Reactor.getReactor().registerChannel(client);
					}
				}
				selector.selectedKeys().clear();
			} catch (final IOException e) {
				Logger.logErr(e.toString());
				e.printStackTrace();
				System.exit(1);
				return;
			} catch (final ClosedSelectorException e) {/* discard */}
		}
	}

	/**
	 * Close the acceptor channel and selector
	 */
	public void close() {
		try {
			Logger.logInfo("Closed ", serverChannel, " and ", selector);
			serverChannel.close();
			selector.close();
		} catch (final IOException e) {
			Logger.logErr(e.toString());
			e.printStackTrace();
		}
	}
}