package wordquizzle.wqserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.gson.*;

import wordquizzle.Logger;
import wordquizzle.wqserver.Database.UserNotFoundException;
import wordquizzle.wqserver.User.AlreadyFriendsException;

class LogoutHandler extends CommandHandler {
	public LogoutHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		user.logout();
		Logger.logInfo("User ", user.getName(), " disconnected");
		evh.write("Bye " + user.getName());
	}
}

class FriendListHandler extends CommandHandler {	
	public FriendListHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		List<String> userlist = new LinkedList<>();
		user.getFriendList().forEach((User friend) -> userlist.add(friend.getName()));
		evh.write(new Gson().toJson(userlist));
	}
}

class LeaderboardHandler extends CommandHandler {
	public LeaderboardHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		Map<String, Integer> userlist = new LinkedHashMap<>();
		userlist.put(user.getName(), user.getScore());
		user.getFriendList().forEach((User friend) -> userlist.put(friend.getName(), friend.getScore()));
		Map<String, Integer> userlist_sorted= new LinkedHashMap<>();
		userlist.entrySet().stream()
				.sorted(Map.Entry.comparingByValue((v1, v2) -> v1 >= v2 ? -1 : 0))
		        .forEach(entry -> userlist_sorted.put(entry.getKey(), entry.getValue()));
		evh.write(new Gson().toJson(userlist_sorted));
	}
}

class ScoreHandler extends CommandHandler {
	public ScoreHandler(EventHandler evh, User user) {
		super(evh, user);

	};

	public void handle(Scanner scanner) {
		evh.write(new Integer(user.getScore()).toString());
	}
}

class AddFriendHandler extends CommandHandler {
	public AddFriendHandler(EventHandler evh, User user) {
		super(evh, user);
	}

	public void handle(Scanner scanner) {
		try {
			String name = scanner.next();
			try {
				user.addFriend(name);
				Database.getDatabase().getUser(name).addFriend(user.getName());
			} catch (UserNotFoundException e) {
				evh.write("The user " + name + " doesn't exist");
			} catch (AlreadyFriendsException e) {
				evh.write("You're already friends with " + name);
			}
		} catch (NoSuchElementException e) {
			evh.write("You must specify a username");
		}
	}
}

class SendMessageHandler extends CommandHandler {

	public SendMessageHandler(EventHandler evh, User user) {
		super(evh, user);
	}

	public void handle(Scanner scanner) {
		try {
			String name = scanner.next();
			String msg = scanner.next();
			try {
				User recipient = Database.getDatabase().getUser(name);
				if (!recipient.isOnline()) {
					evh.write(name + " isn't online");
					return;
				}
				recipient.getHandler().write(user.getName() + " sent you " + msg);
			} catch (UserNotFoundException e) {
				evh.write("The user " + name + " doesn't exist");
			}
		} catch (NoSuchElementException e) {
			evh.write("You must specify a username and a message");
		}
	}
}

/**
 * The {@code LoggedInEventHandler} class handles all the communications
 * that happens <b>AFTER</b> a user has logged in.
 */
public class LoggedInEventHandler extends EventHandler {

	private User user;

	public LoggedInEventHandler(SelectionKey key, User user, ByteBuffer wbuffer) {
		super(key, wbuffer);
		this.user = user;
	} 

	/**
	 * Returns the user bound to this event handler.
	 * @return the user bound to this event handler.
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * Accepts the commands that can be sent <b>AFTER</b> a user has logged in
	 * @param msg the full command string (command + arguments) 
	 */
	@Override
	protected void compute(String msg) {
		Scanner scanner = new Scanner(msg);
		try {
			String cmd = scanner.next().toLowerCase();
			switch(cmd) {
				case "logout":
					new LogoutHandler(this, user).handle(scanner);
					EventHandler evh = new DefaultEventHandler(key, getWBuffer());
					evh.registerHandler(getReactor());
					break;
				case "lista_amici":
					new FriendListHandler(this, user).handle(scanner);
					break;
				case "mostra_classifica":
					new LeaderboardHandler(this, user).handle(scanner);
					break;
				case "mostra_punteggio":
					new ScoreHandler(this, user).handle(scanner);
					break;
				case "aggiungi_amico":
					new AddFriendHandler(this, user).handle(scanner);
					break;
				case "invia":
					new SendMessageHandler(this, user).handle(scanner);
					break;
				default:
					write("Invalid command");
					break;
			}
		} catch (NoSuchElementException e) {/*this should not happen*/}
		scanner.close();
	}

	
	
}