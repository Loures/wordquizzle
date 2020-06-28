package wordquizzle.wqclient.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame {

	public static final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	public static JTextArea textArea = new JTextArea(0, 0);
	public static JFrame frame;

	public static void appendLine(String line) {
		textArea.append(line + "\n");
	}

	public static JFrame createFrame() {
		frame = new JFrame("WordQuizzle");
		frame.setResizable(false);
		frame.setBounds((int)(screen.width / 2 - screen.width / 4), (int)(screen.height / 2 - screen.height / (2*1.5)),
		                (int)(screen.width / 2), (int)(screen.height / 1.5));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);

		JPanel borderPanel = new JPanel();
		borderPanel.setLayout(null);
		borderPanel.setBounds(16, 16, frame.getContentPane().getWidth() - 32, frame.getContentPane().getHeight() - 32);


		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 20));
		buttonPanel.setBounds(0, 0, 180, borderPanel.getHeight());
		borderPanel.add(buttonPanel);

		JButton friendlistButton = new JButton("Lista amici");
		friendlistButton.setPreferredSize(new Dimension(180, 40));
		friendlistButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new FriendListCommand().handle();		
			}
		});
		buttonPanel.add(friendlistButton);

		JButton showLeaderboardButton = new JButton("Mostra classifica");
		showLeaderboardButton.setPreferredSize(new Dimension(180, 40));
		showLeaderboardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new LeaderboardCommand().handle();		
			}
		});
		buttonPanel.add(showLeaderboardButton);

		JButton showScoreButton = new JButton("Mostra punteggio");
		showScoreButton.setPreferredSize(new Dimension(180, 40));
		showScoreButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new ScoreCommand().handle();		
			}
		});
		buttonPanel.add(showScoreButton);

		JButton addFriendButton = new JButton("Aggiungi amico");
		addFriendButton.setPreferredSize(new Dimension(180, 40));
		addFriendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				WQClient.setActiveFrame(AddFriendFrame.createFrame());
			}
		});
		buttonPanel.add(addFriendButton);

		JButton challengeFriendButton = new JButton("Sfida amico");
		challengeFriendButton.setPreferredSize(new Dimension(180, 40));
		challengeFriendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				WQClient.setActiveFrame(ChallengeFrame.createFrame());		
			}
		});
		buttonPanel.add(challengeFriendButton);

		JButton quitButton = new JButton("Esci");
		quitButton.setPreferredSize(new Dimension(180, 40));
		quitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new LogoutCommand().handle();
				frame.dispose();
			}
			
		});
		buttonPanel.add(quitButton);

		textArea.setFont(textArea.getFont().deriveFont(Font.BOLD, textArea.getFont().getSize()));
		textArea.setMargin(new Insets(4, 4, 4, 4));
		textArea.setForeground(new Color(68,68,68));
		textArea.setBounds(196, 0, borderPanel.getWidth() - 196, borderPanel.getHeight());
		textArea.setEditable(false);
		borderPanel.add(textArea);

		frame.getContentPane().add(borderPanel);
		frame.repaint();

		return frame;
	}
}