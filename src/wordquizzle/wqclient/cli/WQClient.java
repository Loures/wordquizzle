package wordquizzle.wqclient.cli;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import wordquizzle.Logger;
import wordquizzle.Response;
import wordquizzle.UserState;

public class WQClient {
	private static int port;
	public static UserState state = UserState.OFFLINE;

	public static void main(final String[] args) {

		class Shutdown extends Thread {
			@Override
			public void run() {
				new LogoutCommand().handle(null);
				CLIReactor.getReactor().close();
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

		CLIReactor.getReactor(new InetSocketAddress("127.0.0.1", port));

		System.out.print("> ");
		while (!Thread.interrupted()) {
			String line = System.console().readLine();
			CommandHandler.getHandler(state).startCompute(line);
		}
	}
}