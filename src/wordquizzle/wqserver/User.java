package wordquizzle.wqserver;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import wordquizzle.wqserver.Database.UserNotFoundException;

/**
 * The {@code User} class describes a WordQuizzle user.
 */
public class User {
	
	/**
	 * Runtime exception thrown when the two users are already friends.
	 */
	public static class AlreadyFriendsException extends Exception {
		private static final long serialVersionUID = 1L;
	
		public AlreadyFriendsException() {
			super();
		}
	}

	/**
	 * User class JSON serializer.
	 */
	public static class UserJsonSerializer implements JsonSerializer<User> {
		public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject output = new JsonObject();
			output.add("name", new JsonPrimitive(user.getName()));
			output.add("password", new JsonPrimitive(user.getPassword()));
			output.add("score", new JsonPrimitive(user.getScore()));
			JsonArray friendlist = new JsonArray(user.getFriendList().size());
			user.getFriendList().forEach((User usr) -> friendlist.add(usr.getName()));
			output.add("friendlist", friendlist);
			return (JsonElement)output;

		}
	}

	/**
	 * User class JSON deserializer.
	 */
	public static class UserJsonDeserializer implements JsonDeserializer<User> {
		public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
		            throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			JsonArray json_friendlist = obj.getAsJsonArray("friendlist");
			LinkedList<User> friendlist = new LinkedList<>();
			//Build a preliminary friend list
			for (JsonElement elem : json_friendlist) {
				User fakeuser = new User();
				fakeuser.setName(elem.getAsString());
				friendlist.add(fakeuser);
			}
			User user = new User(obj.getAsJsonPrimitive("name").getAsString(),
			                     obj.getAsJsonPrimitive("password").getAsString(),
			                     obj.getAsJsonPrimitive("score").getAsInt(),
			                     friendlist);
			return user;
		}
	}

	/**
	 * Byte to hex converter.
	 * https://www.baeldung.com/sha-256-hashing-java
	 * @param hash hashed data.
	 * @return hexadecimal string representing the hash.
	 */
	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
		String hex = Integer.toHexString(0xff & hash[i]);
		if(hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private String name = "";
	private String password = "";
	private int score = 0;
	private List<User> friendlist;
	private boolean logged = false;

	private LoggedInEventHandler handler;

	/**
	 * Constructs an empty User.
	 */
	public User() {
		this.friendlist = new LinkedList<>();
	}
	
	/**
	 * Constructs a full User with a "fake" friendlist.
	 * @param name       the user's nickname.
	 * @param password   the user's ALREADY HASHED password
	 * @param score      the user's score.
	 * @param friendlist the fake friendlist
	 */
	public User(String name, String password, int score, List<User> friendlist) {
		this.name = name;
		this.password = password;
		this.score = score;
		this.friendlist = new LinkedList<>(friendlist);
	}

	/**
	 * Get a user's nickname.
	 * @return the user's nickname.
	 */
	public String getName() {
		return new String(name);
	}
	/**
	 * Set a user's nickname.
	 * @param name the user's nickname.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get a user's password.
	 * @return the user's password.
	 */
	public String getPassword() {
		return new String(password);
	}

	/**
	 * Set a user's password.
	 * @param password the user's password.
	 */
	public void setPassword(String password) {
		//Hash the password
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			this.password = bytesToHex(hashed);
		} catch (NoSuchAlgorithmException e) {/*discard*/}
	}

	/**
	 * Checks if the supplied password is the user's password.
	 * @param password the supplied password.
	 * @return true iff the supplied password equals the user password, false otherwise.
	 */
	public boolean checkPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hashed).equals(this.password);
		} catch (NoSuchAlgorithmException e) {return false;}	
	}

	/**
	 * Returns the user's cumulative score.
	 * @return the user's cumulative score.
	 */
	public synchronized int getScore() {
		return score;
	}

	/**
	 * Increases the user's score by {@code points} points.
	 * @param points the points to increase the score by.
	 * @throws IllegalArgumentException if {@code points} is a negative integer.
	 */
	public synchronized void incrScore(int points) throws IllegalArgumentException {
		if (points < 0) throw new IllegalArgumentException("argument must be a positive number");
		score += points;
		Database.getDatabase().updateUser(this);
	}
	/**
	 * Decreases the user's score by {@code points} points.
	 * @param points the points to decrease the score by.
	 * @throws IllegalArgumentException if {@code points} is a negative integer.
	 */
	public synchronized void decrScore(int points) throws IllegalArgumentException {
		if (points < 0) throw new IllegalArgumentException("argument must be a positive number");
		score -= points;
		Database.getDatabase().updateUser(this);
	}

	/**
	 * Returns the user's friend list.
	 * @return the user's friend list.
	 */
	public synchronized List<User> getFriendList() {
		return new LinkedList<User>(friendlist);
	}
	
	/**
	 * Adds user with nickname {@code name} to the user's friend list.
	 * @param name name of the user to add to the friend list
	 * @throws AlreadyFriendsException if the two users are already friends.
	 * @throws UserNotFoundException if the user doesn't exist.
	 */
	public synchronized void addFriend(String name) throws AlreadyFriendsException, UserNotFoundException {
		User friend = Database.getDatabase().getUser(name);
		if (!friendlist.contains(friend) && !friend.equals(this)) {
			friendlist.add(friend);
			Database.getDatabase().updateUser(this);
		} else throw new AlreadyFriendsException();
	}

	/**
	 * Initialize the friend list.
	 * @throws AlreadyFriendsException if the friend list contains the user itself.
	 */
	public void initFriendshipRelations() throws AlreadyFriendsException {
		for (int i = 0; i < friendlist.size(); i++) {
			try {
				User friend = Database.getDatabase().getUser(friendlist.get(i).getName());
				if (friend.equals(this)) {
					friendlist.remove(i);
					throw new AlreadyFriendsException();
				}
				else friendlist.set(i, friend);
			} catch (UserNotFoundException e) {/*discard*/};
		}
	}

	/**
	 * Sets the user's status as logged in.
	 */
	public synchronized void login() {
		this.logged = true;
	}

	/**
	 * Sets the user's status as logged out.
	 */
	public synchronized void logout() {
		this.handler = null;
		this.logged = false;
	}

	/**
	 * returns {@code true} iff the user is logged in.
	 * @return {@code true} iff the user is logged in.
	 */
	public synchronized boolean isOnline() {
		return this.logged;
	}

	/**
	 * Sets the user's current event handler.
	 * @param handler the user's new event handler.
	 */
	public void setHandler(LoggedInEventHandler handler) {
		this.handler = handler;
	}
	/**
	 * Returns the user's current event handler.
	 * @return the user's current event handler.
	 */
	public LoggedInEventHandler getHandler() {
		return handler;
	}

	/**
	 * Returns the user's nickname.
	 */
	@Override
	public String toString() {
		return new String(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Returns {@code true} if the user's share the same nickname.
	 */
	@Override
	public boolean equals(Object user) {
		if (user instanceof User) return name.equals(((User)user).getName());
		return false;
	}
}