package wordquizzle.wqserver;

import wordquizzle.Logger;
import java.util.Random;

public class WQServer {
	
	private static int port;

	public static void main(final String[] args) {

		class Shutdown extends Thread {
			@Override
			public void run() {
				Acceptor acceptor;
				if ((acceptor = Acceptor.getAcceptor()) != null) acceptor.close();
				for (Reactor reactor : Reactor.reactors) reactor.close();
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
					try {

						Logger.logInfo(Database.getDatabase().getUser(line).checkPassword(line+"password"));
					} catch (Exception e) {e.printStackTrace();}
				}
			}
		}).start();
		
		Reactor.reactors = new Reactor[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			Reactor.reactors[i] = new Reactor();
			Reactor.reactors[i].start();
		}

		Acceptor acceptor = Acceptor.getAcceptor();
		acceptor.start(port);

	}
}