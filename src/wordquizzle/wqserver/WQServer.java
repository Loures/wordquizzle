package wordquizzle.wqserver;

import wordquizzle.Logger;
import wordquizzle.WQRegisterInterface;

import java.io.FileNotFoundException;
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

		//load the challenge dictionary
		try {Challenge.loadDictionary();}
		catch (FileNotFoundException e) {
			Logger.logErr("Dictionary not found, exiting...");
			System.exit(1);
		}
		
		//Initialize the RMI registry
		new RegisterServerHandler().initHandler(WQRegisterInterface.port);

		//Start up all the reactors
		Reactor.reactors = new Reactor[4];
		for (int i = 0; i < Reactor.reactors.length; i++) {
			Reactor.reactors[i] = new Reactor();
			Reactor.reactors[i].start();
		}

		//Start up the connection acceptor
		Acceptor acceptor = Acceptor.getAcceptor();
		acceptor.start(port);

	}
}