package wordquizzle.wqclient.gui;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.swing.JFrame;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqclient.Reactor;

public class GUIReactor extends Reactor{
	private GUIReactor(InetSocketAddress addr) {
		super(addr);
	}

	/**
	 * Create a new {@code GUIReactor} instance as a Singleton.
	 * @param addr the address to bind to.
	 * @return The newly created {@code GUIReactor}.
	 */
	protected static Reactor getReactor(InetSocketAddress addr) {
		if (reactor == null) {
			reactor = new GUIReactor(addr);
			reactor.start();
		}
		return reactor;
	}

	public void handleRead(String msg) {
		Scanner scannerstub = new Scanner(msg);
		Scanner scanner = scannerstub.useDelimiter(":");
		String code = scanner.next();
		switch (code) {
			// SET_STATE handling
			case "SET_STATE":
				String newState = scanner.next();
				switch (newState) {
					case "OFFLINE":
						WQClient.state = UserState.OFFLINE;
						break;
					case "IDLE":
						WQClient.state = UserState.IDLE;
						break;
					case "CHALLENGE_ISSUED":
						WQClient.state = UserState.CHALLENGE_ISSUED;
						break;
					case "CHALLENGED":
						WQClient.state = UserState.CHALLENGED;
						break;
					case "IN_GAME":
						WQClient.state = UserState.IN_GAME;
						break;
				}
				break;
			case "LOGIN_FAILURE":
				LoginFrame.showError(Response.LOGIN_FAILURE.getResponse());
				break;
			case "LOGIN_SUCCESS":
				String name = scanner.next();
				LoginFrame.showOk(Response.LOGIN_SUCCESS.getResponse(name));
				LoginFrame.close();
				WQClient.setActiveFrame(MainFrame.createFrame());
				MainFrame.appendLine(Response.LOGIN_SUCCESS.getResponse(name));
				break;
			case "USERNOTEXISTS_FAILURE":
				name = scanner.next();
				JFrame frame = WQClient.getActiveFrame();
				if (frame == ChallengeFrame.frame) 
					ChallengeFrame.showError(Response.USERNOTEXISTS_FAILURE.getResponse(name));
				else if (frame == AddFriendFrame.frame)
					AddFriendFrame.showError(Response.USERNOTEXISTS_FAILURE.getResponse(name));
				else if (frame ==LoginFrame.frame)
					LoginFrame.showError(Response.USERNOTEXISTS_FAILURE.getResponse(name));
				break;
			case "ALREADYFRIENDS_FAILURE":
				name = scanner.next();
				AddFriendFrame.showError(Response.ALREADYFRIENDS_FAILURE.getResponse(name));
				break;
			case "FRIENDSELF_FAILURE":
				AddFriendFrame.showError(Response.FRIENDSELF_FAILURE.getResponse());
				break;
			case "ADDFRIEND_SUCCESS":
				String user1 = scanner.next();
				String user2 = scanner.next();
				MainFrame.appendLine(Response.ADDFRIEND_SUCCESS.getResponse(user1, user2));
				WQClient.setActiveFrame(MainFrame.frame);
				AddFriendFrame.frame.dispose();
				break;
			case "FRIENDLIST":
				String friendlist = scanner.nextLine().substring(1).trim();
				Gson gson = new Gson();
				JsonArray list = gson.fromJson(friendlist, JsonArray.class);
				StringBuilder builder = new StringBuilder();
				list.forEach((JsonElement elem) -> builder.append(elem.getAsString() + " "));
				MainFrame.appendLine(builder.toString());
				break;
			case "LEADERBOARD":
				String leaderboard = scanner.nextLine().substring(1).trim();
				gson = new Gson();
				JsonObject obj = gson.fromJson(leaderboard, JsonObject.class);
				obj.keySet().forEach((String key) -> MainFrame.appendLine(key + " " + obj.getAsJsonPrimitive(key).getAsString()));
				break;
			case "SCORE":
				int score = scanner.nextInt();
				MainFrame.appendLine(Response.SCORE.getResponse(score));
				break;
			case "CHALLENGE_FROM":
				name = scanner.next();
				if (AddFriendFrame.frame != null) AddFriendFrame.frame.dispose();
				if (ChallengeFrame.frame != null) ChallengeFrame.frame.dispose();
				WQClient.setActiveFrame(AcceptChallengeFrame.createFrame(Response.CHALLENGE_FROM.getResponse(name)));
				break;
			case "BEGIN_CHALLENGE":
				if (AcceptChallengeFrame.frame != null) AcceptChallengeFrame.frame.dispose();
				if (ChallengeFrame.frame != null) ChallengeFrame.frame.dispose();
				WQClient.setActiveFrame(GameFrame.createFrame());
				break;
			case "GAME_FINISHED":
				if (GameFrame.frame != null) GameFrame.frame.dispose();
				MainFrame.frame.setEnabled(true);
				break;
			case "GAME_RESULT":
				if (GameFrame.frame != null) GameFrame.frame.dispose();
				WQClient.setActiveFrame(MainFrame.frame);
				MainFrame.appendLine(Response.GAME_RESULT.getResponse(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
				break;
			case "WINNER":
				MainFrame.appendLine(Response.WINNER.getResponse(scanner.nextInt(), scanner.nextInt()));
				break;
			case "SEND_WORD":
				if (GameFrame.frame != null) GameFrame.wordArea.setText(Response.SEND_WORD.getResponse(scanner.nextInt(), scanner.nextInt(), scanner.next()));
				break;
			case "QUIT_CHALLENGE":
				name = scanner.next();
				if (WQClient.getActiveFrame() != AddFriendFrame.frame || WQClient.getActiveFrame() != MainFrame.frame)
					WQClient.getActiveFrame().dispose();
				WQClient.setActiveFrame(MainFrame.frame);
				MainFrame.appendLine(Response.QUIT_CHALLENGE.getResponse(name));
				break;
			case "GAME_TIMEDOUT":
				if (WQClient.getActiveFrame() != AddFriendFrame.frame || WQClient.getActiveFrame() != MainFrame.frame)
					WQClient.getActiveFrame().dispose();
				WQClient.setActiveFrame(MainFrame.frame);
				MainFrame.appendLine(Response.GAME_TIMEDOUT.getResponse());
				break;				
			case "FINISH_CHALLENGE":
				if (WQClient.getActiveFrame() == GameFrame.frame)
					WQClient.getActiveFrame().dispose();
				WQClient.setActiveFrame(MainFrame.frame);
				break;
			case "NOTFRIENDS_FAILURE":
				name = scanner.next();
				ChallengeFrame.showError(Response.NOTFRIENDS_FAILURE.getResponse(name));
				break;
			case "CANTPLAY_FAILURE":
				name = scanner.next();
				ChallengeFrame.showError(Response.CANTPLAY_FAILURE.getResponse(name));
				break;
			case "WAITING_RESPONSE":
				ChallengeFrame.awaitResponse();
				break;
			default:
				try {
					Response response = Response.valueOf(code);
					try {
						//Handle responses with 1 arg only
						MainFrame.appendLine(response.getResponse(scanner.next()));
						break;
					} catch (NoSuchElementException e) {}
					//Handle the responses with no arguments
					MainFrame.appendLine(response.getResponse());
					break;
				} catch (IllegalArgumentException e) {}
				//Print everything else
				MainFrame.appendLine(msg);
				break;
		}
		scanner.close();
	}

}