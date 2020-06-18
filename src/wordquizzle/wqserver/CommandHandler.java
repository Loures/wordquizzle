package wordquizzle.wqserver;

import java.util.Scanner;

/**
 * The {@code CommandHandler} abstract class describes how to
 * handle commands sent after a user has logged in
 */
public abstract class CommandHandler {
	protected EventHandler evh;
	protected User user;

	public CommandHandler(EventHandler evh, User user) {
		this.evh = evh;
		this.user = user;
	}

	/**
	 * Process the given command.
	 * @param scanner scanner containing the command.
	 */
	public abstract void handle(Scanner scanner);
}