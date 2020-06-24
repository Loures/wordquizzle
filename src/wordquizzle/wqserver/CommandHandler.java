package wordquizzle.wqserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.gson.Gson;

import wordquizzle.Logger;
import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqserver.Database.UserNotFoundException;
import wordquizzle.wqserver.User.*;
import wordquizzle.wqserver.EventHandler;

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

class LoginHandler extends CommandHandler {

	public LoginHandler(EventHandler evh, User user) {
		super(evh, user);
	}
	
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			String password = scanner.next();
			int port = new Integer(scanner.next()).intValue();
			try {
				User user = Database.getDatabase().getUser(username);
				if (user.getState() != UserState.OFFLINE) {
					evh.write(Response.ALREADYLOGGEDIN_FAILURE.getCode(user));
					return;
				}
				if (user.checkPassword(password)) {
					user.setHandler(evh);
					evh.setUser(user);
					user.login(port);
					evh.write(Response.LOGIN_SUCCESS.getCode(username));
				} else {
					evh.write(Response.LOGIN_FAILURE.getCode());
					return;
				}
			} catch (UserNotFoundException e) {
				evh.write(Response.USERNOTEXISTS_FAILURE.getCode(username));
			}
		} catch (NoSuchElementException e) {
			evh.write(Response.NOUSERPASS_FAILURE.getCode());
		}
	}
}

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
		List<String> userlist = new ArrayList<>(user.getFriendList().size());
		user.getFriendList().forEach((User friend) -> userlist.add(friend.getName()));
		evh.write(new Gson().toJson(userlist));
	}
}

class LeaderboardHandler extends CommandHandler {
	public LeaderboardHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		LinkedHashMap<String, Integer> userlist = new LinkedHashMap<>();
		userlist.put(user.getName(), user.getScore());
		user.getFriendList().forEach((User friend) -> userlist.put(friend.getName(), friend.getScore()));
		LinkedHashMap<String, Integer> userlist_sorted= new LinkedHashMap<>();
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
				evh.write(Response.ADDFRIEND_SUCCESS.getCode(user, name));
			} catch (UserNotFoundException e) {
				evh.write(Response.USERNOTEXISTS_FAILURE.getCode(name));
			} catch (AlreadyFriendsException e) {
				evh.write(Response.ALREADYFRIENDS_FAILURE.getCode(name));
			}
		} catch (NoSuchElementException e) {
			evh.write(Response.NOUSERNAME_FAILURE.getCode());
		}
	}
}

class IssueChallengeHandler extends CommandHandler {

	public IssueChallengeHandler(EventHandler evh, User user) {
		super(evh, user);
	}

	public void handle(Scanner scanner) {
		try {
			String name = scanner.next();
			try {
				User opponent = Database.getDatabase().getUser(name);
				if (opponent.getState() != UserState.IDLE) {
					evh.write("User " + name + " can't play right now");
					return;
				}
				if (!opponent.getFriendList().contains(this.user)) {
					evh.write("You are not friends with user " + name);
					return;
				}
				String msg = Response.ISSUECHALLENGE.getCode(user.getName()) + "\n";
				try {
					DatagramSocket udpsocket = new DatagramSocket();
					try {

						//Send out an UDP packet
						udpsocket.connect(opponent.getHandler().getLocalAddress().getAddress(), opponent.getUDPPort());
						DatagramPacket pkt = new DatagramPacket(msg.getBytes(StandardCharsets.UTF_8),
						                                        msg.getBytes(StandardCharsets.UTF_8).length);
						udpsocket.send(pkt);
						//and notify the client that we're waiting...
						user.setState(UserState.CHALLENGE_ISSUED);
						opponent.setState(UserState.CHALLENGED);
						evh.write(Response.WAITINGRESPONSE.getCode());
						
						Challenge challenge = new Challenge(user, opponent);
						user.setChallenge(challenge);
						opponent.setChallenge(challenge);

					} catch (Exception e) {e.printStackTrace();}
					finally {udpsocket.close();}
				} catch (Exception e) {e.printStackTrace();}
			} catch (UserNotFoundException e) {evh.write(Response.USERNOTEXISTS_FAILURE.getCode());}
		} catch (NoSuchElementException e) {evh.write(Response.NOUSERNAME_FAILURE.getCode());}
	}
}

class HasWordCommandHandler extends CommandHandler {

	public HasWordCommandHandler(EventHandler evh, User user) {
		super(evh, user);
	}
	public void handle(Scanner scanner) {
		try {
			String word = scanner.next();
			user.getChallenge().hasWord(user, word);
		} catch (NoSuchElementException e) {}
	}	
}