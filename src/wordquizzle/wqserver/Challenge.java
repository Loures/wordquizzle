package wordquizzle.wqserver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wordquizzle.Response;
import wordquizzle.UserState;
import wordquizzle.wqserver.User.NoHandlerAssignedException;

/**
 * The {@code Challenge} class implements the match between two players.
 */
public class Challenge implements Runnable {

	/**
	 * The {@code ChallengeAcceptanceTimeout} class handles the timeout in the acception/rejection of a challenge
	 */
	class ChallengeAcceptanceTimeout extends TimerTask {
		private Challenge challenge;

		public ChallengeAcceptanceTimeout(Challenge challenge) {
			this.challenge = challenge;
		}

		@Override
		public void run() {
			challenge.abortChallenge();
		}
	}

	/**
	 * The {@code ChallengeTimeout} class handles the expiry of a game.
	 */
	class ChallengeTimeout extends TimerTask {
		private Challenge challenge;

		public ChallengeTimeout(Challenge challenge) {
			this.challenge = challenge;
		}

		@Override
		public void run() {
			challenge.finishGame();
		}
	}

	private static final String url = "https://api.mymemory.translated.net/get?q=%s&langpair=it|en";
	private static final int numWords = 4;
	private static final long acceptanceTimeOut = 15L * 1000L;
	private static final long gameTimeOut = 20L * 1000L;
	private static List<String> words;

	class GameData {
		public int startingScore;
		public int correctAnswers = 0;
		public int wrongAnswers = 0;
		public int numWords = 0;
		public String currentWord = null;

		public GameData(User user) {
			startingScore = user.getScore();
		}
	}

	/**
	 * Load the words from the dict.txt file.
	 */
	public static void loadDictionary() throws FileNotFoundException {
		try {
			Path dict = Paths.get("./dict.txt");
			if (Files.exists(dict)) words = Files.readAllLines(dict);
			else throw new FileNotFoundException();
		} catch (IOException e) {e.printStackTrace();}
	}

	private User player1;
	private User player2;
	private HashMap<User, GameData> gameDataMap = new HashMap<>();
	private HashMap<String, ArrayList<String>> wordsMap = new HashMap<>();
	private ArrayList<String> wordsList = new ArrayList<>(numWords);
	private Gson gson = new Gson();
	private Timer acceptanceTimer;
	private Timer gameTimer;
	public boolean gameStarted = false;

	public Challenge(User player1, User player2) {
		this.player1 = player1;
		this.player2 = player2;
		this.acceptanceTimer = new Timer("acceptanceTimeout" + new Integer(this.hashCode()).toString(), true);
		this.gameTimer = new Timer("gameTimeout" + new Integer(this.hashCode()).toString(), true);
		
		//Launch the acceptance timer.
		this.acceptanceTimer.schedule(new ChallengeAcceptanceTimeout(this), acceptanceTimeOut); 
	}

	private ArrayList<String> fetchAndParseJson(String word) {
		ArrayList<String> translatedWords = new ArrayList<>();
		try {
			StringBuilder content = new StringBuilder();

			//Issue a GET request for the translation of word. This right here is the reason the challenge
			//is run on a separate thread as this GET request would block the reactor the challenge is running on.
			URL targetUrl = new URL(String.format(url, word));
			URLConnection conn = targetUrl.openConnection();
			conn.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			//Read the reply into the content string
			reader.lines().forEach((String line) -> content.append(line));

			//If we got a 200 OK reply go on and parse the JSON string received
			if (((HttpsURLConnection)conn).getResponseCode() == HttpsURLConnection.HTTP_OK) {
				JsonArray matches = gson.fromJson(content.toString(), JsonElement.class)
				                        .getAsJsonObject().getAsJsonArray("matches");
				for (JsonElement match : matches) {
					JsonObject matchObj = match.getAsJsonObject();
					if (matchObj.getAsJsonPrimitive("segment")
					            .getAsString().toLowerCase().equals(word)) {
						//Put the translated word inside the possible translations array
						translatedWords.add(matchObj.getAsJsonPrimitive("translation").getAsString().toLowerCase());
					}
				}
			}
		} catch (MalformedURLException e) {/*discard*/}
		catch (IOException e) {}
		return translatedWords;
	}

	private void buildChallenge() {
		Random random = new Random(System.currentTimeMillis());
		int dictsize = words.size();
		int size = 0;
		int i = 0;

		//Pick numWords random words
		while (size < numWords) {
			String word = words.get(i % dictsize);
			if (random.nextInt(dictsize) == 0 && !wordsMap.containsKey(word)) {
				wordsMap.put(word, fetchAndParseJson(word));
				wordsList.add(word);
				size++;
			}
			i++;
		}

		//Start the challenge timer and send the first word.
		gameTimer.schedule(new ChallengeTimeout(this), gameTimeOut);
		try {
			gameDataMap.get(player1).currentWord = wordsList.get(0);
			gameDataMap.get(player2).currentWord = wordsList.get(0);
			player1.getHandler().write(Response.SEND_WORD.getCode(1, numWords, gameDataMap.get(player1).currentWord));
			player2.getHandler().write(Response.SEND_WORD.getCode(1, numWords, gameDataMap.get(player2).currentWord));
		} catch (NoHandlerAssignedException e) {}
	}

	private void initGame() {
		//Initialize the gameData structure for each player and start picking the words to be translated
		try {
			gameDataMap.put(player1, new GameData(player1));
			gameDataMap.put(player2, new GameData(player2));

			player1.getHandler().write(Response.BEGIN_CHALLENGE.getCode());
			player2.getHandler().write(Response.BEGIN_CHALLENGE.getCode());
			acceptanceTimer.cancel();
			buildChallenge();
		} catch (NoHandlerAssignedException e) {/*silently fail*/}
	}

	public void finishGame() {
		GameData player1GameData = gameDataMap.get(player1); 
		GameData player2GameData = gameDataMap.get(player2);
		try {
			//Send the results to the players
			player1.getHandler().write(Response.GAME_RESULT
			       .getCode(player1GameData.correctAnswers, player1GameData.wrongAnswers,
			       player1.getScore() - player1GameData.startingScore));
			player2.getHandler().write(Response.GAME_RESULT
			       .getCode(player2GameData.correctAnswers, player2GameData.wrongAnswers,
			       player2.getScore() - player2GameData.startingScore));
			
			//Pick the winner (if any) and award him the extra points
			if (player1GameData.correctAnswers > player2GameData.correctAnswers) {
				player1.incrScore(3);
				player1.getHandler().write(Response.WINNER.getCode(3, player1.getScore() - player1GameData.startingScore));
			} else if (player2GameData.correctAnswers > player1GameData.correctAnswers) {
				player2.incrScore(3);
				player2.getHandler().write(Response.WINNER.getCode(3, player2.getScore() - player2GameData.startingScore));
			}
		} catch (NoHandlerAssignedException e) {}

		//Set both players state to IDLE
		player1.setState(UserState.IDLE);
		player2.setState(UserState.IDLE);
	}

	private void startChallenge() {
		player1.setState(UserState.IN_GAME);
		player2.setState(UserState.IN_GAME);
		initGame();
	}

	/**
	 * Handles the received translated word.
	 * @param user           The user who sent the translation.
	 * @param translatedWord The translation the user sent.
	 */
	public void receiveWord(User user, String translatedWord) {
		try {
			GameData userGameData = gameDataMap.get(user);
			
			//We didn't finish loading the words yet so don't do anything
			if (userGameData.currentWord == null) return;

			GameData opponentGameData = gameDataMap.get(getOpponent(user));

			//If a player has sent in all the translations do nothing and notify the player he can't do anything
			if (userGameData.numWords == numWords) {
				user.getHandler().write(Response.GAME_FINISHED.getCode());
				return;
			}

			//Check if the received translation matches and award (or decrease) points accordingly
			if (wordsMap.get(userGameData.currentWord).contains(translatedWord)) {
				userGameData.correctAnswers++;
				user.incrScore(2);
			} else {
				userGameData.wrongAnswers++;
				user.decrScore(1);
			}
			userGameData.numWords++;

			//If both players have finished sending in their translations finish the game
			if (userGameData.numWords == numWords && opponentGameData.numWords == numWords) {
				gameTimer.cancel();
				finishGame();
				return;
			}

			//Notify the player that he can't send in anymore words
			if (userGameData.numWords == numWords) {
				user.getHandler().write(Response.GAME_FINISHED.getCode());
			}

			//Send the next word to the player
			if (userGameData.numWords < numWords) {
				userGameData.currentWord = wordsList.get(userGameData.numWords);
				user.getHandler().write(Response.SEND_WORD.getCode(userGameData.numWords + 1, numWords, userGameData.currentWord));
			}
		} catch (NoHandlerAssignedException e) {e.printStackTrace();}		
	}

	/**
	 * Handles aborting the challenge when a user has decided to quit.
	 * @param user The user who quit.
	 */
	public synchronized void abortChallenge(User user) {
		gameTimer.cancel();
		acceptanceTimer.cancel();
		User opponent = getOpponent(user);
		try {
			opponent.getHandler().write(Response.QUIT_CHALLENGE.getCode(user.getName()));
		} catch (NoHandlerAssignedException e) {};
		opponent.setChallenge(null);
		opponent.setState(UserState.IDLE);
		user.setChallenge(null);
		user.setState(UserState.IDLE);
	}

	/**
	 * Handles aborting the challenge when a timer runs out.
	 */
	public synchronized void abortChallenge() {
		gameTimer.cancel();
		acceptanceTimer.cancel();
		try {
			player1.getHandler().write(Response.GAME_TIMEDOUT.getCode());
			player2.getHandler().write(Response.GAME_TIMEDOUT.getCode());
		} catch (NoHandlerAssignedException e) {}
		player1.setChallenge(null);	
		player1.setState(UserState.IDLE);
		player2.setChallenge(null);	
		player2.setState(UserState.IDLE);
	}

	public User getPlayer1() {
		return player1;
	}

	public User getPlayer2() {
		return player2;
	}

	/**
	 * Gets the player's opponent.
	 * @param player The player.
	 * @return The opponent.
	 */
	public User getOpponent(User player) {
		if (player.equals(player1)) return player2;
		return player1;
	}

	/**
	 * Starts the challenge.
	 */
	@Override public void run() {
		startChallenge();
	}
}