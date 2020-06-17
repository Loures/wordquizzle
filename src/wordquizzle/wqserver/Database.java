package wordquizzle.wqserver;

import wordquizzle.wqserver.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

/**
 * This class implements the server Database as a Singleton
 */
public class Database {

	public static class UserNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;
	
		public UserNotFoundException() {
			super();
		}
	}

	
	private static Database database;
	private Path dbfile;
	private ConcurrentHashMap<String, User> backend;
	
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
			dbfile = Paths.get("./database.json");
			if (!Files.exists(dbfile)) Files.createFile(dbfile);
			Gson gson = getDBGson();
			Collection<User> userlist = gson.fromJson(new String(Files.readAllBytes(dbfile)),
			                                          new TypeToken<Collection<User>>(){}.getType());
			if (userlist != null) userlist.forEach((User user) -> backend.put(user.getName(), user));
		} catch (FileAlreadyExistsException e) {/*discard*/}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the database singleton
	 * @return database singleton
	 */
	public static Database getDatabase() {
		if (database == null) database = new Database();
		return database;
	}

	public void addUser(User user) {
		try {
			backend.put(user.getName(), user);
			BufferedWriter writer = Files.newBufferedWriter(dbfile, StandardCharsets.UTF_8);
			writer.write(getDBGson().toJson(backend.values()));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public User getUser(String name) throws UserNotFoundException {
		User user = backend.get(name);
		if (user != null) return user; else throw new UserNotFoundException();
	}
}