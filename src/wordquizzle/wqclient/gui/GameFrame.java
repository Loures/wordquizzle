package wordquizzle.wqclient.gui;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame {
	public static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

	public static JFrame frame;
	public static JTextArea wordArea;

	public static JFrame createFrame() {
		frame = new JFrame("Traduci la parola");
		frame.setResizable(false);
		frame.setBounds(screen.width / 2 - 200, screen.height / 2 - 100, 400, 150);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				new CloseChallengeCommand().handle();
				WQClient.setActiveFrame(MainFrame.frame);
			}
		});
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(null);
		inputPanel.setBounds(16, 16, frame.getContentPane().getWidth() - 32, frame.getContentPane().getHeight() - 32);

		wordArea = new JTextArea("Waiting data...");
		wordArea.setBackground(inputPanel.getBackground());
		wordArea.setEditable(false);
		wordArea.setWrapStyleWord(true);
		wordArea.setLineWrap(true);
		wordArea.setFont(wordArea.getFont().deriveFont(Font.BOLD, wordArea.getFont().getSize()));
		wordArea.setBounds(0, 0, inputPanel.getWidth(), inputPanel.getHeight() - 48);
		inputPanel.add(wordArea);

		JTextField answerTextField = new JTextField();
		answerTextField.setBounds(inputPanel.getWidth() / 2 - 125, inputPanel.getHeight() - 24, 250, 20);
		answerTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (answerTextField.getText().trim().length() > 0) {
					new SendWordCommand().handle(answerTextField.getText().trim());
					answerTextField.setText("");
				}
			}
		});
		inputPanel.add(answerTextField);

		frame.getContentPane().add(inputPanel);
		frame.repaint();

		return frame;
	}
}