package wordquizzle;

public enum Response {
	REGISTRATION_SUCCESS    ("REGISTRATION_SUCCESS", "You have been succesfully registered"),
	ADDFRIEND_SUCCESS       ("ADDFRIEND_SUCCESS:%s:%s", "Friendship between %s and %s created"),
	LOGIN_SUCCESS           ("LOGIN_SUCCESS:%s", "Welcome back %s"),
	SETSTATE                ("SET_STATE:%s", ""),
	RESETCHALLENGE			("ABORTED_CHALLENGE:%s", "User %s has aborted the challenge"),
	FINISHCHALLENGE         ("FINISH_CHALLENGE", ""),
	ISSUECHALLENGE          ("CHALLENGE_FROM:%s", "User %s wants to play a game of WordQuizzle with you, do you accept? (yes/no)"),
	WAITINGRESPONSE         ("WAITING_RESPONSE", "Waiting for the challenge to be accepted..."),
	CHALLENGE_ACCEPTED      ("CHALLENGE_ACCEPTED", "Challenge accepted"),
	GAME_RESULT             ("GAME_RESULT:%d:%d:%d", "You got %d answers correct, %d answers wrong for a total of %d points earned"),
	USERNOTEXISTS_FAILURE   ("USERNOTEXISTS_FAILURE:%s", "The user %s does not exist"),
	ALREADYFRIENDS_FAILURE  ("ALREADYFRIENDS_FAILURE:%s", "You are already friends with %s"),
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