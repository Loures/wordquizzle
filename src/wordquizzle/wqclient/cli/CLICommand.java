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

public abstract class CLICommand {
	/**
	 * Process the given command.
	 * @param scanner scanner containing the command.
	 */
	public abstract void handle(Scanner scanner);
}

class RegisterCommand extends CLICommand {
	public void handle(Scanner scanner) {
		if (WQClient.state != UserState.OFFLINE) {
			System.out.println("You are already logged in");
			return;
		}
		try {
			String username = scanner.next();
			String password = scanner.next();
				try {
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

class LoginCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			String password = scanner.next();
			CLIReactor.getReactor().write("login:" + username + ":" + password +
			                                  ":" + CLIReactor.getReactor().getUDPPort());
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username and a password\n> ");}
	}	
}

class FriendListCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("lista_amici");
	}
}

class LeaderboardCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("mostra_classifica");
	}
}

class ScoreCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("mostra_punteggio");
	}
}

class AddFriendCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			CLIReactor.getReactor().write("aggiungi_amico:" + username);
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username\n> ");}
	}
}

class IssueChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			CLIReactor.getReactor().write("sfida:" + username);
		} catch (NoSuchElementException e) {System.err.print("You need to supply a username\n> ");}
	}
}

class AcceptChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("yes");
	}
}

class RejectChallengeCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("no");
		System.out.print("You have rejected the invitation\n> ");
	}
}

class SendWordCommand extends CLICommand {
	public void handle(Scanner scanner) {
		try {
			String word = scanner.nextLine().trim();
			switch(word) {
				case "!quit":
					System.exit(0);
					return;
				case "!logout":
					new LogoutCommand().handle(scanner);
					return;
			}
			CLIReactor.getReactor().write("word:" + word);
		} catch (NoSuchElementException e) {}
	}
}

class LogoutCommand extends CLICommand {
	public void handle(Scanner scanner) {
		CLIReactor.getReactor().write("logout");
	}
}

