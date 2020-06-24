package wordquizzle;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WQRegisterInterface extends Remote {

	public static final int port = 6666;

	void register(String name, String password) throws RemoteException, UserAlreadyExists,
	                                                   UsernameNotValid, PasswordNotValid;

	class UserAlreadyExists extends Exception {
		private static final long serialVersionUID = 1L;
	};
	class UsernameNotValid extends Exception {
		private static final long serialVersionUID = 1L;
	};
	class PasswordNotValid extends Exception {
		private static final long serialVersionUID = 1L;
	};

}