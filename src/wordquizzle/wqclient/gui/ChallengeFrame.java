package wordquizzle.wqclient.gui;

import javax.swing.*;

import wordquizzle.Response;
import wordquizzle.UserState;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ChallengeFrame {
	public static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

	private static JLabel responseLabel = new JLabel();
	private static JTextField nameField;
	public static JFrame frame;
	public static JPanel inputPanel;
	public static JPanel awaitResponsePanel;
	private static int responseLabelStartPosX;
	private static int responseLabelStartPosY;

	public static void showError(String err) {
		int len = responseLabel.getFontMetrics(responseLabel.getFont()).stringWidth(err);
		responseLabel.setForeground(new Color(198, 40, 40));
		responseLabel.setText(err);
		responseLabel.setBounds(responseLabelStartPosX - len / 2, responseLabelStartPosY, len, 16);

	}

	public static void showOk(String ok) {
		int len = responseLabel.getFontMetrics(responseLabel.getFont()).stringWidth(ok);
		responseLabel.setForeground(new Color(46, 125, 50));
		responseLabel.setText(ok);
		responseLabel.setBounds(responseLabelStartPosX - len / 2, responseLabelStartPosY, len, 16);
	}

	public static void awaitResponse() {
		for (Component component : inputPanel.getComponents()) component.setVisible(false);
		JLabel awaitResponseLabel = new JLabel();
		awaitResponseLabel.setText("Awaiting response...");
		int len = awaitResponseLabel.getFontMetrics(awaitResponseLabel.getFont()).stringWidth("Awaiting response...");
		awaitResponseLabel.setBounds(inputPanel.getWidth() / 2 - len / 2, inputPanel.getHeight() / 2, len, 16);
		inputPanel.add(awaitResponseLabel);

		frame.repaint();
	}

	public static JFrame createFrame() {
		responseLabel.setText("");

		frame = new JFrame("Sfida amico");
		frame.setResizable(false);
		frame.setBounds(screen.width / 2 - 200, screen.height / 2 - 100, 400, 150);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				WQClient.setActiveFrame(MainFrame.frame);
				if (WQClient.state != UserState.IDLE) new CloseChallengeCommand().handle();
			}
		});
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);

		inputPanel = new JPanel();
		inputPanel.setLayout(null);
		inputPanel.setBounds(16, 16, frame.getContentPane().getWidth() - 32, frame.getContentPane().getHeight() - 32);

		JLabel nameLabel = new JLabel("Username:");
		nameLabel.setBounds(0, 0, 90, 16);
		inputPanel.add(nameLabel);

		nameField = new JTextField();
		nameField.setBounds(nameLabel.getWidth(), 0, inputPanel.getWidth() - nameLabel.getWidth(), 20);
		inputPanel.add(nameField);

		JButton submitButton = new JButton("Submit");
		submitButton.setBounds(inputPanel.getWidth() - 150, inputPanel.getHeight() - 30, 150, 30);
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (nameField.getText().length() > 0) {
					new IssueChallengeCommand().handle(nameField.getText());
				} else showError(Response.NOUSERNAME_FAILURE.getResponse());
			}
		});
		inputPanel.add(submitButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBounds(0, inputPanel.getHeight() - 30, 150, 30);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WQClient.setActiveFrame(MainFrame.frame);
				frame.dispose();
			}
			
		});
		inputPanel.add(cancelButton);
		inputPanel.add(responseLabel);
		responseLabelStartPosX = inputPanel.getWidth() / 2;
		responseLabelStartPosY = inputPanel.getHeight() - 60;
		
		frame.add(inputPanel);
		frame.repaint();

		return frame;

	}
}