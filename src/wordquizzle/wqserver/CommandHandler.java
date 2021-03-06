package wordquizzle.wqserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/**
 * The {@code CommandHandler} abstract class describes how to
 * handle commands sent by an user
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

/**
 * The {@code LoginHandler} class handles login requests.
 */
class LoginHandler extends CommandHandler {

	public LoginHandler(EventHandler evh, User user) {
		super(evh, user);
	}
	
	public void handle(Scanner scanner) {
		try {
			String username = scanner.next();
			String password = scanner.next();

			//UDP port the user is listening on.
			int port = new Integer(scanner.next()).intValue();
			try {
				//Fetch the user from the database
				User user = Database.getDatabase().getUser(username);

				//You can't login again if you're already logged in
				if (user.getState() != UserState.OFFLINE) {
					evh.write(Response.ALREADYLOGGEDIN_FAILURE.getCode(user));
					return;
				}

				//Check if the password sent by the user matches the one stored in the database, if so log the user in.
				if (user.checkPassword(password)) {
					//Assing the user to the EventHandler and viceversa, then log the user in
					user.setHandler(evh);
					evh.setUser(user);
					user.login(port);
					evh.write(Response.LOGIN_SUCCESS.getCode(username));
					Logger.logInfo("User ", username, " logged in succesfully");
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

/**
 * The {@code LogoutHandler} class handles logout requests.
 */
class LogoutHandler extends CommandHandler {
	public LogoutHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		if (user.getState() != UserState.OFFLINE) {
			user.logout();
			evh.write("Bye " + user.getName());
		}
	}
}

/**
 * The {@code FriendListHandler} class handles "show friendlist" requests.
 */
class FriendListHandler extends CommandHandler {	
	public FriendListHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		//Build the friendlist and send it as a JSON array
		List<String> userlist = new ArrayList<>(user.getFriendList().size());
		user.getFriendList().forEach((User friend) -> userlist.add(friend.getName()));
		evh.write(Response.FRIENDLIST.getCode(new Gson().toJson(userlist)));
	}
}

/**
 * The {@code LeaderboardHandler} class handles "show leaderboard" requests.
 */
class LeaderboardHandler extends CommandHandler {
	public LeaderboardHandler(EventHandler evh, User user) {
		super(evh, user);
	};

	public void handle(Scanner scanner) {
		//Build a list of (nick, score) pairs
		LinkedHashMap<String, Integer> userlist = new LinkedHashMap<>();
		userlist.put(user.getName(), user.getScore());
		user.getFriendList().forEach((User friend) -> userlist.put(friend.getName(), friend.getScore()));

		//Build another list of (nick, score), but this time sorted in descending order and send it as a JSON object
		LinkedHashMap<String, Integer> userlist_sorted= new LinkedHashMap<>();
		userlist.entrySet().stream()
				.sorted(Map.Entry.comparingByValue((v1, v2) -> v1 >= v2 ? -1 : 0))
		        .forEach(entry -> userlist_sorted.put(entry.getKey(), entry.getValue()));
		evh.write(Response.LEADERBOARD.getCode(new Gson().toJson(userlist_sorted)));
	}
}

/**
 * The {@code ScoreHandler} class handles "show score" requests.
 */
class ScoreHandler extends CommandHandler {
	public ScoreHandler(EventHandler evh, User user) {
		super(evh, user);

	};

	public void handle(Scanner scanner) {
		evh.write(Response.SCORE.getCode(user.getScore()));
	}
}

/**
 * The {@code AddFriendHandler} class handles "add friend" requests.
 */
class AddFriendHandler extends CommandHandler {
	public AddFriendHandler(EventHandler evh, User user) {
		super(evh, user);
	}

	public void handle(Scanner scanner) {
		try {
			String name = scanner.next();
			try {
				user.addFriend(name);

				//If X is friend of Y then Y is friend of X
				Database.getDatabase().getUser(name).addFriend(user.getName());
				evh.write(Response.ADDFRIEND_SUCCESS.getCode(user, name));
			} catch (UserNotFoundException e) {
				evh.write(Response.USERNOTEXISTS_FAILURE.getCode(name));
			} catch (AlreadyFriendsException e) {
				evh.write(Response.ALREADYFRIENDS_FAILURE.getCode(name));
			} catch (SelfFriendException e) {
				evh.write(Response.FRIENDSELF_FAILURE.getCode());
			}
		} catch (NoSuchElementException e) {
			evh.write(Response.NOUSERNAME_FAILURE.getCode());
		}
	}
}


/**
 * The {@code IssueChallengehandler} class handles challenge requests.
 */
class IssueChallengeHandler extends CommandHandler {

	public IssueChallengeHandler(EventHandler evh, User user) {
		super(evh, user);
	}

	public void handle(Scanner scanner) {
		try {
			String name = scanner.next();
			try {
				User opponent = Database.getDatabase().getUser(name);

				//If the opponent is not our friend we can't challenge him
				if (!opponent.getFriendList().contains(this.user)) {
					evh.write(Response.NOTFRIENDS_FAILURE.getCode(name));
					return;
				}

				//If the opponent is not idle we can't challenge him
				if (opponent.getState() != UserState.IDLE) {
					evh.write(Response.CANTPLAY_FAILURE.getCode(name));
					return;
				}

				//This is the message that will be sent as an UDP packet to the opponent
				String msg = Response.CHALLENGE_FROM.getCode(user.getName()) + "\n";
				try {
					DatagramSocket udpsocket = new DatagramSocket();
					try {
						//Send out an UDP packet
						udpsocket.connect(opponent.getHandler().getLocalAddress().getAddress(), opponent.getUDPPort());
						DatagramPacket pkt = new DatagramPacket(msg.getBytes(StandardCharsets.UTF_8),
																msg.getBytes(StandardCharsets.UTF_8).length);
						udpsocket.send(pkt);

						//Set the two players state to "challenged" and to "awaiting response"
						user.setState(UserState.CHALLENGE_ISSUED);
						opponent.setState(UserState.CHALLENGED);

						//and notify the client that we're waiting...
						evh.write(Response.WAITINGRESPONSE.getCode());
						
						//Finally create the challenge
						Challenge challenge = new Challenge(user, opponent);
						user.setChallenge(challenge);
						opponent.setChallenge(challenge);
						Logger.logInfo("Initiated challenge between ", user.getName(), " and ", opponent.getName());

					} catch (Exception e) {e.printStackTrace();}
					finally {udpsocket.close();}
				} catch (Exception e) {e.printStackTrace();}
			} catch (UserNotFoundException e) {evh.write(Response.USERNOTEXISTS_FAILURE.getCode(name));}
		} catch (NoSuchElementException e) {evh.write(Response.NOUSERNAME_FAILURE.getCode());}
	}
}

/**
 * The {@code SendWordCommandHandler} class handles the received translations sent by the players.
 */
class SendWordCommandHandler extends CommandHandler {

	public SendWordCommandHandler(EventHandler evh, User user) {
		super(evh, user);
	}
	public void handle(Scanner scanner) {
		try {
			String word = scanner.next();
			user.getChallenge().receiveWord(user, word);
		} catch (NoSuchElementException e) {}
	}	
}