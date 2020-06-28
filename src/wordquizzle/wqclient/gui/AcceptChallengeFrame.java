package wordquizzle.wqclient.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AcceptChallengeFrame {
	public static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

	public static JFrame frame;
	public static JPanel inputPanel;

	public static JFrame createFrame(String msg) {
		frame = new JFrame("Sfida ricevuta");
		frame.setResizable(false);
		frame.setBounds(screen.width / 2 - 200, screen.height / 2 - 100, 400, 150);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				new RejectChallengeCommand().handle();
				WQClient.setActiveFrame(MainFrame.frame);
			}
		});
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);

		inputPanel = new JPanel();
		inputPanel.setLayout(null);
		inputPanel.setBounds(16, 16, frame.getContentPane().getWidth() - 32, frame.getContentPane().getHeight() - 32);

		JTextArea nameLabel = new JTextArea(msg);
		nameLabel.setBackground(inputPanel.getBackground());
		nameLabel.setEditable(false);
		nameLabel.setWrapStyleWord(true);
		nameLabel.setLineWrap(true);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, nameLabel.getFont().getSize()));
		nameLabel.setBounds(0, 0, inputPanel.getWidth(), inputPanel.getHeight() - 48);
		inputPanel.add(nameLabel);

		JButton submitButton = new JButton("Yes");
		submitButton.setBounds(inputPanel.getWidth() - 150, inputPanel.getHeight() - 30, 150, 30);
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new AcceptChallengeCommand().handle();
				WQClient.setActiveFrame(MainFrame.frame);
				frame.dispose();
			}
		});
		inputPanel.add(submitButton);

		JButton cancelButton = new JButton("No");
		cancelButton.setBounds(0, inputPanel.getHeight() - 30, 150, 30);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new RejectChallengeCommand().handle();
				WQClient.setActiveFrame(MainFrame.frame);
				frame.dispose();
			}
			
		});
		inputPanel.add(cancelButton);

		frame.add(inputPanel);
		frame.repaint();

		return frame;
	}
}