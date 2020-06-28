package wordquizzle.wqclient.gui;

import javax.swing.*;

import wordquizzle.Response;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame {
	public static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

	private static JLabel responseLabel = new JLabel();
	public static JFrame frame;
	private static JTextField nameField;
	private static JPasswordField passwordField;
	private static int responseLabelStartPosX;
	private static int responseLabelStartPosY;

	public static void showError(String err) {
		int len = responseLabel.getFontMetrics(responseLabel.getFont()).stringWidth(err);
		responseLabel.setForeground(new Color(198, 40, 40));
		responseLabel.setText(err);
		responseLabel.setBounds(responseLabelStartPosX - len/2, responseLabelStartPosY, len, 16);
		nameField.setText("");
		passwordField.setText("");
	}

	public static void showOk(String ok) {
		int len = responseLabel.getFontMetrics(responseLabel.getFont()).stringWidth(ok);
		responseLabel.setForeground(new Color(46, 125, 50));
		responseLabel.setText(ok);
		responseLabel.setBounds(responseLabelStartPosX - len/2, responseLabelStartPosY, len, 16);
		nameField.setText("");
		passwordField.setText("");
	}

	public static JFrame createFrame() {
		frame = new JFrame("GUI test");
		frame.setResizable(false);
		frame.setBounds(screen.width / 2 - 200, screen.height / 2 - 100, 400, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(null);
		inputPanel.setBounds(48, 16, frame.getContentPane().getWidth() - 96, frame.getContentPane().getHeight() - 32);

		JLabel nameLabel = new JLabel("Username:");
		nameLabel.setBounds(0, 0, 90, 16);
		inputPanel.add(nameLabel);

		nameField = new JTextField();
		nameField.setBounds(nameLabel.getWidth(), 0, inputPanel.getWidth() - nameLabel.getWidth(), 20);
		inputPanel.add(nameField);

		JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setBounds(0, 36, 90, 16);
		inputPanel.add(passwordLabel);

		passwordField = new JPasswordField();
		passwordField.setBounds(passwordLabel.getWidth(), 36, inputPanel.getWidth() - passwordLabel.getWidth(), 20);
		inputPanel.add(passwordField);

		JButton registerButton = new JButton("Register");
		registerButton.setBounds(16, frame.getContentPane().getHeight() - 40, 150, 30);
		registerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (nameField.getText().trim().length() > 0 && new String(passwordField.getPassword()).trim().length() > 0)
					new RegisterCommand().handle(nameField.getText().trim(), new String(passwordField.getPassword()).trim());
				else showError(Response.NOUSERPASS_FAILURE.getResponse());
			}
		});
		frame.getContentPane().add(registerButton);
		
		JButton loginButton = new JButton("Login");
		loginButton.setBounds(frame.getContentPane().getWidth() - 166, frame.getContentPane().getHeight() - 40, 150, 30);
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (nameField.getText().trim().length() > 0 && new String(passwordField.getPassword()).trim().length() > 0)
					new LoginCommand().handle(nameField.getText().trim(), new String(passwordField.getPassword()).trim());
				else showError(Response.NOUSERPASS_FAILURE.getResponse());
			}
		});
		frame.getContentPane().add(loginButton);
		
		frame.getContentPane().add(responseLabel);
		frame.getContentPane().add(inputPanel);

		responseLabelStartPosX = frame.getContentPane().getWidth() / 2;
		responseLabelStartPosY = frame.getContentPane().getHeight() - 80;
		
		frame.getContentPane().repaint();

		return frame;
	}

	public static void close() {
		frame.dispose();
	}
}