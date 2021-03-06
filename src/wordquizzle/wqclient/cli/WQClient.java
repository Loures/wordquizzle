package wordquizzle.wqclient.cli;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

import wordquizzle.Logger;
import wordquizzle.UserState;

public class WQClient {
	private static int port;
	private static String addr;
	public static UserState state = UserState.OFFLINE;

	//Taken from https://www.techiedelight.com/validate-ip-address-java/
	public static boolean isValidInet4Address(String ip)
	{
		String[] groups = ip.split("\\.");

		if (groups.length != 4)
			return false;

		try {
			return Arrays.stream(groups)
						.map(Integer::parseInt)
						.filter(i -> (i >= 0 && i <= 255))
						.count() == 4;

		} catch(NumberFormatException e) {
			return false;
		}
	}

	public static void printHelp() {
		System.out.print("usage: wordquizzle.wqclient.cli.WQClient <ip> <port>\n\n"
		+ "commands:\n"
		+ "    register <username> <password>: registra utente\n"
		+ "    login <username> <password>: login utente\n"
		+ "    aggingi_amico <username>: aggiungi un amico\n"
		+ "    sfida <username>: sfida un amico\n"
		+ "    lista_amici: stampa lista amici utente\n"
		+ "    mostra_punteggio: mostra punteggio dell'utente\n"
		+ "    mostra_classifica: mostra classifica amici dell'utente\n");
	}

	public static void main(final String[] args) {

		class Shutdown extends Thread {
			@Override
			public void run() {
				//Log the user out and close the reactor
				if (state != UserState.OFFLINE) new LogoutCommand().handle(null);
				if (CLIReactor.getReactor() != null) CLIReactor.getReactor().close();

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

		//Check for correct port argument
		if (args.length > 0) {
			if (args[0].equals("--help") || args[0].equals("-h")) {
				printHelp();
				return;
			}
			try {
				addr = args[0];
				if (!isValidInet4Address(addr)) {
					Logger.logErr("Invalid address supplied");
					System.exit(1);
				}
				port = Integer.valueOf(args[1]);
			} catch (NumberFormatException e) {
				Logger.logErr("Invalid port supplied");
				System.exit(1);
				return;
			}
		} else {
			printHelp();
			return;
		}

		//Create the reactor
		CLIReactor.getReactor(new InetSocketAddress(addr, port));
		
		//Start the CLI prompt
		System.out.print("> ");
		while (!Thread.interrupted()) {
			String line = System.console().readLine();
			CommandHandler.getHandler(state).startCompute(line);
		}
	}
}