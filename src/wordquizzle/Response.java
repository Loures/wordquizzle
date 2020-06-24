package wordquizzle;

public enum Response {
	REGISTRATION_SUCCESS    ("REGISTRATION_SUCCESS", "You have been succesfully registered"),
	ADDFRIEND_SUCCESS       ("ADDFRIEND_SUCCESS:%s:%s", "Friendship between %s and %s created"),
	LOGIN_SUCCESS           ("LOGIN_SUCCESS:%s", "Welcome back %s"),
	SET_STATE               ("SET_STATE:%s", ""),
	QUIT_CHALLENGE			("QUIT_CHALLENGE:%s", "User %s has quit the challenge"),
	BEGIN_CHALLENGE         ("BEGIN_CHALLENGE", "Let the challenge begin"),
	SEND_WORD               ("SEND_WORD:%d:%d:%s", "Word %d of %d: %s"),
	FINISH_CHALLENGE        ("FINISH_CHALLENGE", ""),
	GAME_FINISHED           ("GAME_FINISHED", "You've answered to all the words and must wait for the opponent to finish"),
	WINNER                  ("WINNER:%d:%d", "Congratulations you've won! you earned a bonus %d points for a total of %d points"),
	GAME_TIMEDOUT           ("GAME_TIMEDOUT", "The game has timed out"),
	CHALLENGE_FROM          ("CHALLENGE_FROM:%s", "User %s wants to play a game of WordQuizzle with you, do you accept? (yes/no)"),
	WAITINGRESPONSE         ("WAITING_RESPONSE", "Waiting for the challenge to be accepted..."),
	CHALLENGE_ACCEPTED      ("CHALLENGE_ACCEPTED", "Challenge accepted"),
	GAME_RESULT             ("GAME_RESULT:%d:%d:%d", "You got %d answers correct, %d answers wrong for a total of %d points earned"),
	USERNOTEXISTS_FAILURE   ("USERNOTEXISTS_FAILURE:%s", "The user %s does not exist"),
	ALREADYFRIENDS_FAILURE  ("ALREADYFRIENDS_FAILURE:%s", "You are already friends with %s"),
	FRIENDSELF_FAILURE      ("FRIENDSELF_FAILURE:", "You cannot add yourself as a friend"),
	NOTFRIENDS_FAILURE      ("NOTFRIENDS_FAILURE:%s", "You are not friends with %s"),
	CANTPLAY_FAILURE        ("CANTPLAY_FAILURE:%s", "User %s can't play right now"),
	LOGIN_FAILURE           ("LOGIN_FAILURE", "Failed login"),
	NOTONLINE_FAILURE       ("NOTONLINE_FAILURE:%s", "The user %s is not online"),
	NOUSERNAME_FAILURE      ("NOUSERNAME_FAILURE", "You must specify a username"),
	NOUSERPASS_FAILURE      ("NOUSERPASS_FAILURE", "You must specify a username and a password"),
	USERNAMEEXISTS_FAILURE  ("USERNAMEEXISTS_FAILURE:%s", "A user with username %s already exists"),
	ALREADYLOGGEDIN_FAILURE ("ALREADYLOGGEDIN_FAILURE:%s", "User %s is already online"),
	INVALID_COMMAND         ("INVALID_COMMAND", "Invalid command");

 
	private String code;
    private String response;
 
    Response(String code, String response) {
		this.code = code;
		this.response = response;
		
	}
	
	public String getCode() {
		return code.replaceAll("%:s", "");
	}

	public String getCode(Object... obj) {
		return String.format(code, obj);
	}
 
    public String getResponse(Object... obj) {
		return String.format(response, obj);
    }
}