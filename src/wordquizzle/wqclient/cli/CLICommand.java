package wordquizzle.wqclient.cli;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.NoSuchElementException;
import java.util.Scanner;

import wordquizzle.UserState;
import wordquizzle.WQRegisterInterface;
import wordquizzle.WQRegisterInterface.PasswordNotValid;
import wordquizzle.WQRegisterInterface.UserAlreadyExists;
import wordquizzle.WQRegisterInterface.UsernameNotValid;


/**
 * The {@code CLICommand} abstract class describes a the handling of a command issued from the command line.
 */
public abstract class CLICommand {
	/**
	 * Process the given command.
	 * @param scanner scanner containing the command.
	 */
	public abstract void handle(Scanner scanner);
}

/**
 * The {@code RegisterCommand} class implements user registration through RMI.
 */
class RegisterCommand extends CLICommand {
	public void handle(Scanner scanner) {
		
		//Don't do anything if we're already logged in
		if (WQClient.state != UserState.OFFLINE) {
			System.out.println("You are already logged in");
			return;
		}

		try {
			String username = scanner.next();
			String password = scanner.next();
				try {
					//Get the RMI registry and the stub and issue the registration request
					Registry registry = LocateRegistry.getRegistry(WQRegisterInterface.port);
					WQRegisterInterface stub = (WQRegisterInterface) registry.lookup("WQ-REGISTER");
					stub.register(username, password);
					System.out.print("You have been succesfully registered\n> ");
				} catch (UserAlreadyExists e) {System.err.print("A user with name " + username + " already exists\n> ");}
				catch (UsernameNotValid e) {System.err.print("The username you supplied is not valid\n> ");}
				catch (PasswordNotValid e) {System.err.print("The password you supplied is not valid\n> ");}
				catch (Exception e) {e.printStackTrace();}
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username and a password\n> ");}
	}
}

/**
 * The {@code LoginCommand} class implements user login.
 */
class LoginCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			String password = scanner.next();
			//Send the login information along with the UDP port we're listening on for challenge requests
			CLIReactor.getReactor().write("login:" + username + ":" + password +
			                                  ":" + CLIReactor.getReactor().getUDPPort());
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username and a password\n> ");}
	}	
}

/**
 * The {@code FriendList} class implements friendlist fetching.
 */
class FriendListCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("lista_amici");
	}
}

/**
 * The {@code LeaderboardCommand} class implements leaderboard fetching
 */
class LeaderboardCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("mostra_classifica");
	}
}


/**
 * The {@code ScoreCommand} class implements leaderboard fetching.
 */
class ScoreCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("mostra_punteggio");
	}
}

/**
 * The {@code AddFriendCommand} class implements friend adding.
 */
class AddFriendCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			CLIReactor.getReactor().write("aggiungi_amico:" + username);
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username\n> ");}
	}
}

/**
 * The {@code IssueChallengeCommand} class implements the challenge request.
 */
class IssueChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			CLIReactor.getReactor().write("sfida:" + username);
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username\n> ");}
	}
}

/**
 * The {@code AcceptChallengeCommand} class implements accepting the challenge.
 */
class AcceptChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("yes");
	}
}

/**
 * The {@code RejectChallengeCommand} class implements rejecting the challenge.
 */
class RejectChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("no");
		System.out.print("You have rejected the invitation\n> ");
	}
}

/**
 * The {@code SendWordCommand} class implements sending a translation for the current word.
 */
class SendWordCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String word = scanner.nextLine().trim();
			switch(word) {
				//Handle quitting the game
				case "!quit":
					System.exit(0);
					break;
				case "!logout":
					new LogoutCommand().handle(scanner);
					break;
				default:
					CLIReactor.getReactor().write("word:" + word);
					break;
			}
		} catch (NoSuchElementException e) {}
	}
}

/**
 * The {@code LogoutCommand} class implements logging out.
 */
class LogoutCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("logout");
	}
}

