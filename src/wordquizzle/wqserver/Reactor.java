package wordquizzle.wqserver;

import java.io.IOException;
import java.nio.ByteBuffer;
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
	
	//Wakeup stuff
	private Pipe wakeupPipe;
	private SelectionKey wakeupPipeKey;
	private ByteBuffer wakeupPipeBuffer;
	
	//Channel stuff
	private BlockingQueue<SocketChannel> registrationQueue;
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
			this.registrationQueue = new LinkedBlockingQueue<>();
			this.channels = new LinkedList<>();
			this.wakeupPipe = Pipe.open();
			wakeupPipe.source().configureBlocking(false);
			this.wakeupPipeKey = wakeupPipe.source().register(selector, SelectionKey.OP_READ);
			this.wakeupPipeBuffer = ByteBuffer.allocate(512);
			this.id = Reactor.reactornum++;
			this.numOfChannels = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wakes up the reactor.
	 */
	public void wakeup() {
		//Write to the pipe to wakeup the selector
		ByteBuffer one = ByteBuffer.allocate(1);
		try {
			while (wakeupPipe.sink().write(one) == 0);
		} catch (IOException e) {e.printStackTrace();}
	}

	/**
	 * Registers a channel to the reactor channel set
	 * @param channel the channel to register
	 */
	public void registerChannel(SocketChannel channel) {
		//Add channel to the queue
		registrationQueue.add(channel);
		wakeup();
		this.numOfChannels++;
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
			//poll the queue
			while ((channel = registrationQueue.poll(25, TimeUnit.MILLISECONDS)) != null) {
				//Register the channel with the reactor.
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
				//Check if there's channels awaiting registration with the reactor.
				handleChannelRegistration();

				//Select and do work (eventually)
				selector.select();
				if (wakeupPipeKey.isReadable()) {
					//Handle wake ups
					wakeupPipe.source().read(wakeupPipeBuffer);
					wakeupPipeBuffer.clear();
				}
				selector.selectedKeys().remove(wakeupPipeKey);
				for (SelectionKey key : selector.selectedKeys()) {
					EventHandler evh = (EventHandler)key.attachment();
					//Handle writes
					if (key.isWritable())
						try {evh.send();} catch (IOException e) {
							removeChannel((SocketChannel)key.channel());
							key.cancel();
						}
					
					//Handle reads
					if (key.isReadable()) 
						try {evh.handle();} catch (IOException e) {
							removeChannel((SocketChannel)key.channel());
							key.cancel();
						}
					
				}
				selector.selectedKeys().clear();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (ClosedSelectorException | CancelledKeyException e) {/*discard*/}
		}
	}
	
	/**
	 * Closes the Reactor and all its registered channels
	 */
	public void close() {
		selector.wakeup();
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
