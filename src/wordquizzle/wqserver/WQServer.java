
package wordquizzle.wqserver;

import wordquizzle.Logger;
import wordquizzle.wqserver.Reactor;

import java.util.Arrays;
import java.util.Random;
import java.util.Comparator;
import java.util.NoSuchElementException;

public class WQServer {
	private static int port;
	public static Reactor[] reactors;

	public static Reactor getReactor() {

		return Arrays.stream(WQServer.reactors)
		             .min(Comparator.comparing(Reactor::getNumOfChannels))
		             .orElseThrow(NoSuchElementException::new);
	}

	public static void main(final String[] args) {

		class Shutdown extends Thread {
			@Override
			public void run() {
				Acceptor acceptor;
				if ((acceptor = Acceptor.getAcceptor()) != null) acceptor.close();
				Random rand = new Random(System.nanoTime());
				switch(rand.nextInt(3)) {
					case 0:
					System.err.println("\nGoodbye!");
					break;
					case 1:
					System.err.println("\nBye!");
					break;
					case 2:
					System.err.println("\nMorituri te salutant.");
					break;
				}
			}
		}

		Runtime.getRuntime().addShutdownHook(new Shutdown());

		if (args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				Logger.logErr("Invalid port supplied");
				System.exit(1);
				return;
			}
		} else throw new IllegalArgumentException();

		(new Thread() {
			@Override
			public void run() {
				while (!Thread.interrupted()) {
					String line = System.console().readLine();
					if (line.trim().toLowerCase().equals("quit")) System.exit(0);
				}
			}
		}).start();
		
		WQServer.reactors = new Reactor[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			WQServer.reactors[i] = new Reactor();
			WQServer.reactors[i].start();
		}

		Acceptor acceptor = Acceptor.getAcceptor();
		acceptor.start(port);

	}
}