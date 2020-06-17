package wordquizzle.wqserver;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;


/**
 * This class models a WordQuizzle user.
 */
public class User {
	
	/**
	 * Runtime exception thrown when the two users are already friends.
	 */
	public static class AlreadyFriendsException extends RuntimeException {
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
			user.getFriendList().forEach((String name) -> friendlist.add(name));
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
			User user = new User(obj.getAsJsonPrimitive("name").getAsString(),
			                     obj.getAsJsonPrimitive("password").getAsString(),
			                     obj.getAsJsonPrimitive("score").getAsInt());
			JsonArray friendlist = obj.getAsJsonArray("friendlist");
			for (JsonElement elem : friendlist) user.addFriend(elem.getAsString());
			return user;
		}
	}
	/**
	 * The user's nickname.
	 */
	private String name = "";

	/**
	 * The user's password.
	 */
	private String password = "";

	/**
	 * The user's cumulative score.
	 */
	private int score = 0;

	/**
	 * The user's friend list, implemented as a List of nicknames.
	 */
	private List<String> friendlist;

	/**
	 * Constructs an empty User.
	 */
	public User() {
		this.friendlist = new LinkedList<>();
	}
	
	/**
	 * Constructs a full User with an empty friendlist.
	 * @param name the user's nickname.
	 * @param password the user's password.
	 * @param score the user's score.
	 */
	public User(String name, String password, int score) {
		this.name = name;
		this.password = password;
		this.score = score;
		this.friendlist = new LinkedList<>();
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
		this.password = password;
	}

	/**
	 * Returns the user's cumulative score.
	 * @return the user's cumulative score.
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Increases the user's score by {@code points} points.
	 * @param points the points to increase the score by.
	 * @throws IllegalArgumentException if {@code points} is a negative integer.
	 */
	public void incrScore(int points) throws IllegalArgumentException {
		if (points < 0) throw new IllegalArgumentException("points must be a positive number");
		score += points;
	}
	/**
	 * Decreases the user's score by {@code points} points.
	 * @param points the points to decrease the score by.
	 * @throws IllegalArgumentException if {@code points} is a negative integer.
	 */
	public void decrScore(int points) throws IllegalArgumentException {
		if (points < 0) throw new IllegalArgumentException("points must be a positive number");
		score -= points;
	}

	/**
	 * Returns the user's friend list.
	 * @return the user's friend list.
	 */
	public List<String> getFriendList() {
		return new LinkedList<String>(friendlist);
	}

	/**
	 * Adds user with nickname {@code name} to the user's friend list.
	 * @param name name of the user to add to the friend list
	 * @throws AlreadyFriendsException if the two users are already friends.
	 */
	public void addFriend(String name) throws AlreadyFriendsException {
		friendlist.add(name);
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