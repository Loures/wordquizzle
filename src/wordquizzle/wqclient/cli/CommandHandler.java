package wordquizzle.wqclient.cli;

import java.util.NoSuchElementException;
import java.util.Scanner;

import wordquizzle.Response;
import wordquizzle.UserState;

public abstract class CommandHandler {
	public static CommandHandler getHandler(UserState state) {
		switch(state) {
			case OFFLINE:
				return new DefaultCommandHandler();
			case IDLE:
				return new LoggedInCommandHandler();
			case CHALLENGED:
				return new ChallengedCommandHandler();
			case CHALLENGE_ISSUED:
				return new ChallengeIssuedCommandHandler();
			case IN_GAME:
				return new InGameCommandHandler();
			default:
				return new DefaultCommandHandler();
		}
	}

	public void startCompute(String msg) {
		Scanner scanner = new Scanner(msg);
		compute(scanner);
		scanner.close();
	}

	public abstract void compute(Scanner scanner);
}

class DefaultCommandHandler extends CommandHandler {
	public void compute(Scanner scanner) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "register":
					new RegisterCommand().handle(scanner);
					break;
				case "quit":
					System.exit(0);
					break;
				case "login":
					new LoginCommand().handle(scanner);
					break;
				default:
					System.err.println(Response.INVALID_COMMAND.getResponse());
					System.out.print("> ");
					break;
			}
		} catch (NoSuchElementException e) {};
	}
}

class LoggedInCommandHandler extends CommandHandler {
	public void compute(Scanner scanner) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "quit":
					System.exit(0);
					break;
				case "lista_amici":
					new FriendListCommand().handle(scanner);
					break;
				case "mostra_classifica":
					new LeaderboardCommand().handle(scanner);
					break;
				case "mostra_punteggio":
					new ScoreCommand().handle(scanner);
					break;
				case "aggiungi_amico":
					new AddFriendCommand().handle(scanner);
					break;
				case "logout":
					new LogoutCommand().handle(scanner);
					break;
				case "sfida":
					new IssueChallengeCommand().handle(scanner);
					break;
				default:
					System.err.println(Response.INVALID_COMMAND.getResponse());
					System.out.print("> ");
					break;
			}
		} catch (NoSuchElementException e) {}
	}
}

class ChallengeIssuedCommandHandler extends CommandHandler {
	public void compute(Scanner scanner) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "quit":
					System.exit(0);
					break;
				case "logout":
					new LogoutCommand().handle(scanner);
					break;
				default:
					System.err.println(Response.INVALID_COMMAND.getResponse());
					System.out.print("> ");
					break;
			}
		} catch (NoSuchElementException e) {}
	}
} 

class ChallengedCommandHandler extends CommandHandler {
	public void compute(Scanner scanner) {
		try {
			String cmd = scanner.next();
			switch(cmd) {
				case "yes":
					new AcceptChallengeCommand().handle(scanner);
					break;
				case "no":
					new RejectChallengeCommand().handle(scanner);
					break;
				case "quit":
					System.exit(0);
					break;
				case "logout":
					new LogoutCommand().handle(scanner);
					break;
				default:
					System.err.println(Response.INVALID_COMMAND.getResponse());
					System.out.print("> ");
					break;
			}
		} catch (NoSuchElementException e) {}
	}
} 

class InGameCommandHandler extends CommandHandler {
	public void compute(Scanner scanner) {
		new SendWordCommand().handle(scanner);
	}
} 