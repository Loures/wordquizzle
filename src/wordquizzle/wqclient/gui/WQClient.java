package wordquizzle.wqclient.gui;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;

import wordquizzle.Logger;
import wordquizzle.UserState;

public class WQClient {
	

	private static int port;
	private static String addr;
	public static String name;
	private static JFrame currentActiveFrame;
	public static UserState state = UserState.OFFLINE;

	public static void setActiveFrame(JFrame frame) {
		if (currentActiveFrame != null) currentActiveFrame.setEnabled(false);
		currentActiveFrame = frame;
		frame.setEnabled(true);
	}

	public static JFrame getActiveFrame() {
		return currentActiveFrame;
	}

	//Taken from https://www.techiedelight.com/validate-ip-address-java/
	public static boolean isValidInet4Address(String ip)
	{
		String[] groups = ip.split("\\.");

		if (groups.length != 4)
			return false;

		try {
			return Arrays.stream(groups)
			.map(Integer::parseInt)
			.filter(i -> (i >= 0 && i <= 255))
			.count() == 4;


		} catch(NumberFormatException e) {
			return false;
		}
	}

	public static void main(final String[] args) {
		
		class Shutdown extends Thread {
			@Override
			public void run() {
				if (state != UserState.OFFLINE) new LogoutCommand().handle();
				if (GUIReactor.getReactor() != null) GUIReactor.getReactor().close();
				Random rand = new Random(System.nanoTime());
				switch(rand.nextInt(3)) {
					case 0:
					System.err.println("\nGoodbye!");
					break;
					case 1:
					System.err.println("\nBye!");
					break;
					case 2:
					System.err.println("\nMorituri te salutant.");
					break;
				}
			}
		}

		Runtime.getRuntime().addShutdownHook(new Shutdown());

		//Check for correct port argument
		if (args.length > 0) {
			try {
				addr = args[0];
				if (!isValidInet4Address(addr)) {
					Logger.logErr("Invalid address supplied");
					System.exit(1);
				}
				port = Integer.valueOf(args[1]);
			} catch (NumberFormatException e) {
				Logger.logErr("Invalid port supplied");
				System.exit(1);
				return;
			}
		} else {
			System.out.println("usage: wordquizzle.wqclient.gui.WQClient <ip> <port>");
			return;
		}

		//Create the reactor
		GUIReactor.getReactor(new InetSocketAddress(addr, port));

		WQClient.setActiveFrame(LoginFrame.createFrame());
	}
}