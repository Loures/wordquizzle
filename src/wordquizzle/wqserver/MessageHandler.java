package wordquizzle.wqserver;

import java.util.NoSuchElementException;
import java.util.Scanner;

import wordquizzle.Response;
import wordquizzle.UserState;

public abstract class MessageHandler {
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

	public void startCompute(String msg, EventHandler evh) {
		Scanner scannerstub = new Scanner(msg);
		Scanner scanner = scannerstub.useDelimiter(":");
		compute(scanner, evh);
		scanner.close();
	}

	public abstract void compute(Scanner scanner, EventHandler evh);
}

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
				default:
					evh.write(Response.INVALID_COMMAND.getCode());
					break;
			}
		} catch (NoSuchElementException e) {e.printStackTrace(); return;}
	}
}

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
					evh.getUser().getChallenge().startChallenge(evh.getUser());
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

class InGameMessageHandler extends MessageHandler {
	public void compute(Scanner scanner, EventHandler evh) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "word":
					new HasWordCommandHandler(evh, evh.getUser()).handle(scanner);
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