package wordquizzle.wqserver;

import wordquizzle.Logger;
import wordquizzle.wqserver.Database.UserNotFoundException;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.NoSuchElementException;
import java.util.Scanner;

class LoginHandler {

	private EventHandler evh;

	public LoginHandler(EventHandler evh) {
		this.evh = evh;
	};

	public User handle(Scanner scanner) {
		try {
			String name = scanner.next();
			String password = scanner.next();
			try {
				User user = Database.getDatabase().getUser(name);
				if (user.isOnline()) {
					evh.write("User " + user.getName() + " is already logged in");
					return null;
				}
				if (user.checkPassword(password)) {
					Logger.logInfo("Logged in as ", name);
					user.login();
					evh.write("Welcome back " + name);
					return user;
				} else {
					Logger.logErr("Failed login");
					evh.write("Failed login");
				}
			}
			catch (UserNotFoundException e) {
				Logger.logErr("Found no user with name ", name);
				evh.write("Found no user with name " + name);
			}
		} catch (NoSuchElementException e) {
			evh.write("You need to supply both a username and a password");
			e.printStackTrace();
		}
		return null;
	}
}

class RegisterHandler {

	private EventHandler evh;

	public RegisterHandler(EventHandler evh) {
		this.evh = evh;
	}

	public void handle(Scanner scanner) {
		String name = scanner.next();
		String password = scanner.next();

		if (!Database.getDatabase().hasUser(name)) {
			User user = new User();
			user.setName(name);
			user.setPassword(password);
			Database.getDatabase().updateUser(user);
			evh.write("You have been succesfully registered");
		} else evh.write("A user with username " + name + " already exists");
	}
}

/**
 * The {@code DefaultEventHandler} class handles all the communications
 * that happens before a user has logged in.
 */
public class DefaultEventHandler extends EventHandler {

	public DefaultEventHandler(SelectionKey key) {
		super(key);
	} 

	public DefaultEventHandler(SelectionKey key, ByteBuffer prevbuff) {
		super(key, prevbuff);
	} 

	@Override
	protected void compute(String msg) {
		Scanner scanner = new Scanner(msg);
		try {
			String cmd = scanner.next().toLowerCase();
			switch(cmd) {
				case "login":
					User loggedInUser = new LoginHandler(this).handle(scanner);
					if (loggedInUser != null) {
						LoggedInEventHandler evh = new LoggedInEventHandler(key, loggedInUser, getWBuffer());
						loggedInUser.setHandler(evh);
						evh.registerHandler(getReactor());
					}
					break;
				case "register":
					new RegisterHandler(this).handle(scanner);
					break;
				default:
					write("You must be logged in first");
					break;
			}
		} catch (NoSuchElementException e) {/*this should not happen*/}
		scanner.close();
	}
}