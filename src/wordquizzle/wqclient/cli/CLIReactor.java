package wordquizzle.wqclient.cli;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.gson.*;

import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqclient.Reactor;

/**
 * Handles all UDP and TCP traffic on behalf of the client
 */
public class CLIReactor extends Reactor {

	private CLIReactor(InetSocketAddress addr) {
		super(addr);
	}

	/**
	 * Create a new {@code CLIReactor} instance as a Singleton.
	 * @param addr the address to bind to.
	 * @return The newly created {@code CLIReactor}.
	 */
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
			//Handle all the "special" cases, i.e when we don't want to show the prompt arrow or we have more than
			//one argument.
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
			case "FRIENDLIST":
				String friendlist = scanner.nextLine().substring(1).trim();
				Gson gson = new Gson();
				JsonArray list = gson.fromJson(friendlist, JsonArray.class);
				StringBuilder builder = new StringBuilder();
				list.forEach((JsonElement elem) -> builder.append(elem.getAsString() + " "));
				System.out.print(builder.toString() + "\n> ");
				break;
			case "LEADERBOARD":
				String leaderboard = scanner.nextLine().substring(1).trim();
				gson = new Gson();
				JsonObject obj = gson.fromJson(leaderboard, JsonObject.class);
				obj.keySet().forEach((String key) -> System.out.println(key + " " + obj.getAsJsonPrimitive(key).getAsString()));
				System.out.print("> ");
				break;
			case "SCORE":
				int score = scanner.nextInt();
				System.out.print(Response.SCORE.getResponse(score) + "\n> ");
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
					//Handle the responses with no arguments
					System.out.print(response.getResponse() + "\n> ");
					break;
				} catch (IllegalArgumentException e) {}
				//Print everything else
				System.out.print(msg + "\n> ");
				break;
		}
		scanner.close();
	}
}