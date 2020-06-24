package wordquizzle.wqclient.cli;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Scanner;

import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqclient.Reactor;

/**
 * Handles all UDP and TCP traffic on behalf of the client
 */
public class CLIReactor extends Reactor {

	protected CLIReactor(InetSocketAddress addr) {
		super(addr);
	}

	protected static Reactor getReactor(InetSocketAddress addr) {
		if (reactor == null) {
			reactor = new CLIReactor(addr);
			reactor.start();
		}
		return reactor;
	}

	public void handleRead(String msg) {
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
			case "BEGIN_CHALLENGE":
				System.out.println(Response.BEGIN_CHALLENGE.getResponse());
				break;
			case "ADDFRIEND_SUCCESS":
				String name1 = scanner.next();
				String name2 = scanner.next();
				System.out.print(Response.ADDFRIEND_SUCCESS.getResponse(name1, name2) + "\n> ");
				break;
			case "GAME_FINISHED":
				System.out.println(Response.GAME_FINISHED.getResponse());
				break;
			case "GAME_RESULT":
				int correct = scanner.nextInt();
				int wrong = scanner.nextInt();
				int points = scanner.nextInt();
				System.out.print(Response.GAME_RESULT.getResponse(correct, wrong, points) + "\n> ");
				break;
			case "SEND_WORD":
				int current = scanner.nextInt();
				int maxWords = scanner.nextInt();
				String word = scanner.next();
				System.out.print(Response.SEND_WORD.getResponse(current, maxWords, word) + "\n> ");
				break;
			case "WINNER":
				int bonusPoints = scanner.nextInt();
				int totalPoints = scanner.nextInt();
				System.out.print(Response.WINNER.getResponse(bonusPoints, totalPoints) + "\n> ");
				break;
			default:
				try {
					Response response = Response.valueOf(code);
					try {
						//Handle responses with 1 arg only
						System.out.print(response.getResponse(scanner.next()) + "\n> ");
						break;
					} catch (NoSuchElementException e) {}
					System.out.print(response.getResponse() + "\n> ");
					break;
				} catch (IllegalArgumentException e) {}
				System.out.print(msg + "\n> ");
				break;
		}
		scanner.close();
	}
}