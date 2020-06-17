package wordquizzle.wqserver;

import wordquizzle.Logger;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.google.gson.*;
import com.google.gson.GsonBuilder; 

public class SampleEventHandler extends EventHandler {

	public SampleEventHandler(SelectionKey key) {
		super(key);
	} 

	@Override
	protected void compute(String msg) {
		try {
			Logger.logInfo("Got ", msg, " from ", ((SocketChannel)key.channel()).getRemoteAddress(), " on ", this.getReactor());
			write(new String("Received " + msg + " and sending it back\n").getBytes());
			User user = new User();
			user.setName(msg);
			user.setPassword(msg + "password");
			user.addFriend("msg");
			Database.getDatabase().addUser(user);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}