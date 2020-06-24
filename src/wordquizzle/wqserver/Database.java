package wordquizzle.wqserver;

import wordquizzle.wqserver.User.AlreadyFriendsException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;


/**
 * The {@code Database} class implements the server Database as a Singleton
 */
public class Database {
	
	/**
	 * Exception thrown when the requested user doesn't exist inside the database.
	 */
	public static class UserNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;
	
		public UserNotFoundException() {
			super();
		}
	}

	private static volatile Database database;
	private Path dbfile;
	private ConcurrentHashMap<String, User> backend;
	
	/**
	 * Return a GSon object including the custom serializer and deserializers for the User class.
	 * @return the custom GSon object.
	 */
	public static Gson getDBGson() {
		return new GsonBuilder()
		           .registerTypeAdapter(User.class, new User.UserJsonSerializer())
		           .registerTypeAdapter(User.class, new User.UserJsonDeserializer())
		           .setPrettyPrinting()
		           .create();
	}

	private Database() {
		backend = new ConcurrentHashMap<>();
		try {
			//Read the JSON file and parse it, create it if it doesn't exist.
			dbfile = Paths.get("./database.json");
			if (!Files.exists(dbfile)) Files.createFile(dbfile);

			//Store all of the DB on memory, this can get extremely costly but it will do for the project's purposes.
			Gson gson = getDBGson();
			Collection<User> userlist = gson.fromJson(new String(Files.readAllBytes(dbfile)),
			                                          new TypeToken<Collection<User>>(){}.getType());
			if (userlist != null) userlist.forEach((User user) -> backend.put(user.getName(), user));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return the database singleton
	 * @return database singleton
	 */
	public static Database getDatabase() {
		if (database == null) {
			synchronized(Database.class) {
				if (database == null) {
					database = new Database();
					//Now that the database has been loaded, replace the friendlist User stub with the real ones
					database.initFriendshipRelations();
				}
			}
		}
		return database;
	}

	/**
	 * Initialize all the friend lists.
	 */
	private void initFriendshipRelations() {
		for (User user : backend.values()) {
			try {
				user.initFriendshipRelations();
			} catch (AlreadyFriendsException e) {e.printStackTrace();}
		}
	}

	/**
	 * Updates (inserts if it doesn't exist) a user "row" inside the database.
	 * @param user the user to update.
	 */
	public synchronized void updateUser(User user) {
		try {
			backend.put(user.getName(), user);
			BufferedWriter writer = Files.newBufferedWriter(dbfile, StandardCharsets.UTF_8);
			writer.write(getDBGson().toJson(backend.values()));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the user with username {@code name} if it exists inside the database.
	 * @param name the user's username.
	 * @return the relative User object.
	 * @throws UserNotFoundException if the user hasn't been found inside the database.
	 */
	public User getUser(String name) throws UserNotFoundException {
		User user = backend.get(name);
		if (user != null) return user; else throw new UserNotFoundException();
	}

	/**
	 * Returns {@code true} iff the user with username {@code name} exists inside the database.
	 * @param name the user's username.
	 * @return {@code true} iff the user with username {@code name} exists inside the database.
	 */
	public boolean hasUser(String name) {
		try {
			getUser(name);
			return true;
		} catch (UserNotFoundException e) {
			return false;
		}
	}
}