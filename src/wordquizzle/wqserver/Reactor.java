package wordquizzle.wqserver;

import java.io.IOException;
import java.nio.channels.*;
import java.util.concurrent.BlockingQueue;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * The {@code Reactor} class implements a sort of Reactor pattern for handling reads and writes
 * on a stream socket.
 */
public class Reactor extends Thread {

	public static int reactornum = 0;
	public static Reactor[] reactors;
	private int id;
	private int numOfChannels ;
	private Selector selector;
	private BlockingQueue<SocketChannel> queue;
	private List<SocketChannel> channels;


	/**
	 * Returns the "least busy" reactor.
	 * @return the "least busy" reactor.
	 */
	public static Reactor getReactor() {
		return Arrays.stream(reactors)
		             .min(Comparator.comparing(Reactor::getNumOfChannels))
		             .orElseThrow(NoSuchElementException::new);
	}

	/**
	 * Constructor method
	 */
	public Reactor() {
		try {
			this.selector = Selector.open();
			this.queue = new LinkedBlockingQueue<>();
			this.channels = new LinkedList<>();
			this.id = Reactor.reactornum++;
			this.numOfChannels = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Registers a channel to the reactor channel set
	 * @param channel the channel to register
	 */
	public void registerChannel(SocketChannel channel) {
		queue.add(channel);
		this.numOfChannels++;
		selector.wakeup();
	}

	/**
	 * Removes a channel from the reactor channel set
	 * @param channel the channel to remove
	 */
	public void removeChannel(SocketChannel channel) {
		channels.remove(channel);
		this.numOfChannels--;
		try {
			channel.close();
		} catch (IOException e) {e.printStackTrace();}
	}
	/**
	 * Returns the number of channels registered to a Reactor
	 * @return number of channels registered to a Reactor as an integer
	 */
	public int getNumOfChannels() {
		return this.numOfChannels;
	}

	/**
	 * Returns the Reactor's selector.
	 * @return the reactor's selector.
	 */
	public Selector getSelector() {
		return selector;
	}

	/**
	 * Handles the channel's registration.
	 * This has to be done inside the thread loop otherwise deadlocks will ensue.
	 */
	private void handleChannelRegistration() {
		SocketChannel channel;
		try {
			if ((channel = queue.poll(25, TimeUnit.MILLISECONDS)) != null) {
				SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
				channels.add(channel);
				EventHandler evh = new EventHandler(key);
				evh.registerHandler(this);
				key.attach(evh);
			}
		} catch (ClosedChannelException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				handleChannelRegistration();
				if (!channels.isEmpty()) {
						selector.select();
						for (SelectionKey key : selector.selectedKeys()) {
							EventHandler evh = (EventHandler)key.attachment();
							//Handle writes
							if (key.isWritable()) evh.send();
							
							//Handle reads
							if (key.isReadable()) evh.handle();
							
						}
						selector.selectedKeys().clear();
					}
				}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
				return;
			} catch (ClosedSelectorException | CancelledKeyException e) {/*discard*/}
		}
	}
	
	/**
	 * Closes the Reactor and all its registered channels
	 */
	public void close() {
		try {
			for (SocketChannel channel : channels) channel.close();
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Reactor " + new Integer(this.id).toString();
	}
}
