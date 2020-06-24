package wordquizzle.wqserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqserver.User.NoHandlerAssignedException;


public class Challenge {
	
	class GameData {
		public int startingScore;
		public int correctAnswers = 0;
		public int wrongAnswers = 0;
		public int numWords = 0;

		public GameData(User user) {
			startingScore = user.getScore();
		}
	}

	private User player1;
	private User player2;
	private List<String> words;
	private HashMap<User, GameData> gameDataMap = new HashMap<>();

	public Challenge(User player1, User player2) {
		this.player1 = player1;
		this.player2 = player2;
	}

	private void initGame() {
		try {
			Path dict = Paths.get("./dict.txt");
			if (Files.exists(dict))
				words = Files.readAllLines(dict);

			gameDataMap.put(player1, new GameData(player1));
			gameDataMap.put(player2, new GameData(player2));

			player1.getHandler().write("Let the game begin!");
			player2.getHandler().write("Let the game begin!");
		} catch (IOException | NoHandlerAssignedException e) {/*silently fail*/}
	}

	public void finishGame() {
		//print shit
		GameData player1GameData = gameDataMap.get(player1); 
		GameData player2GameData = gameDataMap.get(player2); 
		try {
			player1.getHandler().write(Response.GAME_RESULT
				   .getCode(player1GameData.correctAnswers, player1GameData.wrongAnswers,
				   player1.getScore() - player1GameData.startingScore));
			player2.getHandler().write(Response.GAME_RESULT
				   .getCode(player2GameData.correctAnswers, player2GameData.wrongAnswers,
				   player2.getScore() - player2GameData.startingScore));
		} catch (NoHandlerAssignedException e) {}
		player1.setState(UserState.IDLE);
		player2.setState(UserState.IDLE);
	}

	public void startChallenge(User starter) {
		System.out.println("Challenge starteeed");
		player1.setState(UserState.IN_GAME);
		player2.setState(UserState.IN_GAME);
		initGame();
	}

	public void hasWord(User user, String word) {
		try {

			User opponent = getOpponent(user);
			GameData userGameData = gameDataMap.get(user);
			if (userGameData.numWords == 4) {
				user.getHandler().write("Your game has finished, wait for the other player!");
				return;
			}
			GameData opponentGameData = gameDataMap.get(opponent);
			if (words.contains(word)) {
				user.incrScore(1);
				userGameData.correctAnswers++;
				user.getHandler().write("You got " + word + " right!");
			} else {
				userGameData.wrongAnswers++;
				user.getHandler().write("Wrong!");
			}
			userGameData.numWords++;
			if (userGameData.numWords == 4 && opponentGameData.numWords == 4) {
				finishGame();
			}
		} catch (NoHandlerAssignedException e) {e.printStackTrace();}		
	}
		
		public void finishChallenge() {
			
		}

	public void abortChallenge(User user) {
		User opponent = getOpponent(user);
		try {
			opponent.getHandler().write(Response.RESETCHALLENGE.getCode(user.getName()));
		} catch (NoHandlerAssignedException e) {};
		opponent.setChallenge(null);
		opponent.setState(UserState.IDLE);
		user.setChallenge(null);
		user.setState(UserState.IDLE);
	}

	public User getPlayer1() {
		return player1;
	}

	public User getPlayer2() {
		return player2;
	}

	public User getOpponent(User player) {
		if (player.equals(player1)) return player2;
		return player1;
	}
}