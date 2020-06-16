package wordquizzle.wqserver;

import wordquizzle.Logger; 
import wordquizzle.wqserver.*;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SampleEventHandler extends EventHandler {

	public SampleEventHandler(SelectionKey key) {
		super(key);
	}

	@Override
	protected void compute(String msg) {
		try {
			Logger.logInfo("Got ", new String(msg).trim(), " from ", ((SocketChannel)key.channel()).getRemoteAddress(), " on ", this.getReactor());
			write(new String("Received " + new String(msg).trim() + " and sending it back\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}