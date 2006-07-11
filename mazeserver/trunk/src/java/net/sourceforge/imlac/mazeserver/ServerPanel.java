/*
 * Copyright © 2004, 2005, 2006 by Howard Palmer.  All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.imlac.mazeserver;

import net.sourceforge.imlac.common.swing.FSButton;
import net.sourceforge.imlac.common.swing.FSLabel;
import net.sourceforge.imlac.common.swing.Utilities;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class contains the main entry point and the user interface for the
 * Imlac Maze Server.
 */
public class ServerPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -4732685266408721565L;

	private static final String[] destNameValues = {
		"localhost",
		"0.0.0.0"
	};
	
	private static final Integer[] destPortValues = {
		new Integer(8082)
	};
	
	private final String heading;
	private JComboBox destNameBox;
	private JComboBox destPortBox;
	private FSButton startButton;
	private Server server;
	
	public ServerPanel() {
		this("Maze Server Control");
	}

	public ServerPanel(String heading) {
		super();
		this.heading = heading;
		buildPanel();
	}
	
	public ServerPanel(boolean isDoubleBuffered) {
		this("Maze Server Control", isDoubleBuffered);
	}

	public ServerPanel(String heading, boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		this.heading = heading;
		buildPanel();
	}
	
	public InetSocketAddress getServerAddress() {
		String destName = (String) destNameBox.getSelectedItem();
		Integer destPort = (Integer) destPortBox.getSelectedItem();
		return new InetSocketAddress(destName, destPort.intValue());
	}
	
	public boolean isServerRunning() {
		return (server != null) && server.isAlive();
	}
	
	private void buildPanel() {
		float savePointSize = FSLabel.setDefaultPointSize(16.0f);
		
		setBackground(Color.white);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		Utilities.addCentered(this, new FSLabel(heading, 24.0f, Font.BOLD));
		add(Box.createVerticalStrut(20));
		
		JPanel hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		hPane.add(new FSLabel("Server Name or IP Address:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));

		destNameBox = new JComboBox(destNameValues);
		destNameBox.setEditable(true);
		Font f = destNameBox.getFont();
		f = f.deriveFont(16.0f);
		destNameBox.setFont(f);
		Utilities.fixComponentHeight(destNameBox);
		
		hPane.add(destNameBox);
		Utilities.fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Server Port Number:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));

		destPortBox = new JComboBox(destPortValues);
		destPortBox.setFont(f);
		destPortBox.setEditable(true);
		Utilities.fixComponentSize(destPortBox);
		
		hPane.add(destPortBox);
		hPane.add(Box.createHorizontalGlue());
		Utilities.fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		startButton = new FSButton("Start Server");
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		
		add(Box.createVerticalStrut(20));
		Utilities.addCentered(this, startButton);
		
		FSLabel.setDefaultPointSize(savePointSize);
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if ("start".equals(cmd)) {
			startButton.setActionCommand("stop");
			startButton.setText("Stop Server");
			InetSocketAddress serverAddr = getServerAddress();
			server = null;
			try {
				server = new Server(serverAddr);
			} catch (IOException iox) {
				System.out.println("I/O Exception during server creation");
				throw new RuntimeException(iox);
			}
			server.start();
		} else if ("stop".equals(cmd)) {
			server.shutdown();
			startButton.setActionCommand("start");
			startButton.setText("Start Server");
		}
	}
	public static void main(String[] args) {
		ServerPanel svr = new ServerPanel();
		JFrame frame = new JFrame("Maze Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(svr);
		frame.pack();
		frame.setVisible(true);
	}
}
