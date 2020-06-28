package wordquizzle.wqserver;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import wordquizzle.Logger;
import wordquizzle.WQRegisterInterface;

public class RegisterServerHandler extends RemoteServer implements WQRegisterInterface {

	private static final long serialVersionUID = 1L;
	private Pattern regexp = Pattern.compile("(\\w+)");

	public void register(String name, String password) throws RemoteException, UserAlreadyExists,
	                                                          UsernameNotValid, PasswordNotValid {
		Matcher nameMatcher = regexp.matcher(name);
		Matcher pwMatcher = regexp.matcher(password);

		//Username and password must contain numbers and letters only.
		if (!nameMatcher.find() || !nameMatcher.group(1).equals(name)) throw new UsernameNotValid();
		if (!pwMatcher.find() || !pwMatcher.group(1).equals(password)) throw new PasswordNotValid();

		//Check if the username isn't already registered and if not add it to the database
		if (!Database.getDatabase().hasUser(name)) {
			User user = new User();
			user.setName(name);
			user.setPassword(password);
			Database.getDatabase().updateUser(user);
			Logger.logInfo("Registered user ", name);
		} else throw new UserAlreadyExists();
	}

	public void initHandler(int port) {
		try {
            RegisterServerHandler regHandler = new RegisterServerHandler();
            UnicastRemoteObject.exportObject(regHandler, port);
            Registry r = LocateRegistry.createRegistry(WQRegisterInterface.port);
            r.bind("WQ-REGISTER", (WQRegisterInterface)regHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}