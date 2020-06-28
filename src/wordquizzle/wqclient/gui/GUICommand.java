package wordquizzle.wqclient.gui;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import wordquizzle.WQRegisterInterface;
import wordquizzle.WQRegisterInterface.*;

public abstract class GUICommand {
	public abstract void handle(String... args);
}

class RegisterCommand extends GUICommand {
	public void handle(String... args) {
		try {
			String username = args[0];
			String password = args[1];
			try {
				//Get the RMI registry and the stub and issue the registration request
				Registry registry = LocateRegistry.getRegistry(WQRegisterInterface.port);
				WQRegisterInterface stub = (WQRegisterInterface) registry.lookup("WQ-REGISTER");
				stub.register(username, password);
				LoginFrame.showOk("You have been succesfully registered");
			} catch (UserAlreadyExists e) {LoginFrame.showError("A user with name " + username + " already exists");}
			catch (UsernameNotValid e) {LoginFrame.showError("The username you supplied is not valid");}
			catch (PasswordNotValid e) {LoginFrame.showError("The password you supplied is not valid");}
			catch (Exception e) {e.printStackTrace();}
		} catch (IndexOutOfBoundsException e) {e.printStackTrace();}
	}
}

class LoginCommand extends GUICommand {
	public void handle(String... args) {
		String username = args[0];
		String password = args[1];
		//Send the login information along with the UDP port we're listening on for challenge requests
		GUIReactor.getReactor().write("login:" + username + ":" + password +
										  ":" + GUIReactor.getReactor().getUDPPort());
	}	
}

class FriendListCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("lista_amici");
	}
}

/**
 * The {@code LeaderboardCommand} class implements leaderboard fetching
 */
class LeaderboardCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("mostra_classifica");
	}
}

/**
 * The {@code AddFriendCommand} class implements friend adding.
 */
class AddFriendCommand extends GUICommand {
	public void handle(String... args) {
		String username = args[0];
		GUIReactor.getReactor().write("aggiungi_amico:" + username);
	}
}


/**
 * The {@code ScoreCommand} class implements leaderboard fetching.
 */
class ScoreCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("mostra_punteggio");
	}
}

class CloseChallengeCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("close");
	}
}

/**
 * The {@code IssueChallengeCommand} class implements the challenge request.
 */
class IssueChallengeCommand extends GUICommand {
	public void handle(String... args) {
		String username = args[0];
		GUIReactor.getReactor().write("sfida:" + username);
	}
}

/**
 * The {@code AcceptChallengeCommand} class implements accepting the challenge.
 */
class AcceptChallengeCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("yes");
	}
}

/**
 * The {@code RejectChallengeCommand} class implements rejecting the challenge.
 */
class RejectChallengeCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("no");
	}
}

/**
 * The {@code SendWordCommand} class implements sending a translation for the current word.
 */
class SendWordCommand extends GUICommand {
	public void handle(String... args) {
		String word = args[0];
		GUIReactor.getReactor().write("word:" + word);
	}
}

/**
 * The {@code LogoutCommand} class implements logging out.
 */
class LogoutCommand extends GUICommand {
	public void handle(String... args) {
		GUIReactor.getReactor().write("logout");
	}
}

