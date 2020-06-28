package wordquizzle.wqserver;

import java.util.NoSuchElementException;
import java.util.Scanner;

import wordquizzle.Response;

/**
 * The {@code MessageHandler} abstract class describes how to handle the messages received by the user given
 * the state the user is in.
 */
public abstract class MessageHandler {

	/**
	 * Factory method for obtaining the appropriate MessageHandler given the user's state.
	 * @param user The user
	 * @return The appropriate MessageHandler
	 */
	public static MessageHandler getHandler(User user) {
		if (user == null) return new DefaultMessageHandler();
		switch(user.getState()) {
			case IDLE:
				return new LoggedInMessageHandler();
			case CHALLENGED:
				return new ChallengedMessageHandler();
			case CHALLENGE_ISSUED:
				return new ChallengeIssuedMessageHandler();
			case IN_GAME:
				return new InGameMessageHandler();
			default:
				return new DefaultMessageHandler();
		}
	}

	/**
	 * Handle the received message.
	 * @param msg The received message.
	 * @param evh The EventHandler that received the message.
	 */
	public void startCompute(String msg, EventHandler evh) {
		Scanner scannerstub = new Scanner(msg);
		Scanner scanner = scannerstub.useDelimiter(":");
		compute(scanner, evh);
		scanner.close();
	}

	public abstract void compute(Scanner scanner, EventHandler evh);
}

/**
 * The {@code DefaultMessageHandler} handles the messages a client sends before logging in.
 */
class DefaultMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "login":
					new LoginHandler(evh, evh.getUser()).handle(scanner);
					break;
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}

/**
 * The {@code LoggedInMessageHandler} handles the messages a user sends after logging in.
 */
class LoggedInMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "logout":
					new LogoutHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "lista_amici":
					new FriendListHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "mostra_classifica":
					new LeaderboardHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "mostra_punteggio":
					new ScoreHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "aggiungi_amico":
					new AddFriendHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "sfida":
					new IssueChallengeHandler(evh, evh.getUser()).handle(scanner);
					break;
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}

/**
 * The {@code ChallengeIssuedMessageHandler} handles the messages a user sends while waiting for a challenge request
 * to be accepted.
 */
class ChallengeIssuedMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "logout":
					//Handle the challenge request abort too
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					new LogoutHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "close":
					//For GUI only, handle the window being closed.
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					break;
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}

/**
 * The {@code ChallengedMessageHandler} handles the messages a user sends after receiving a challenge request 
 */
class ChallengedMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "no":
					//Handle the challenge request abort too
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					break;
				case "yes":
					//This is done in order to avoid blocking the Reactor
					new Thread(evh.getUser().getChallenge()).start();
					break;
				case "logout":
					//Handle the challenge request abort too
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					new LogoutHandler(evh, evh.getUser()).handle(scanner);
					break;
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}


/**
 * The {@code InGameMessageHandler} handles the message a client sends during a WordQuizzle game.
 */
class InGameMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "word":
					new SendWordCommandHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "logout":
					//Handle the challenge request abort too
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					new LogoutHandler(evh, evh.getUser()).handle(scanner);
					break;
				case "close":
					//For GUI only, handle the window being closed.
					evh.getUser().getChallenge().abortChallenge(evh.getUser());
					break;
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}