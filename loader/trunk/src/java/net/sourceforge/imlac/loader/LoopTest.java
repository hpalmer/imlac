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
package net.sourceforge.imlac.loader;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.comm.SerialPort;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.imlac.common.swing.FSButton;
import net.sourceforge.imlac.common.swing.FSLabel;
import net.sourceforge.imlac.common.swing.Utilities;
import net.sourceforge.imlac.mazeserver.ServerPanel;

/**
 * A loop test for Imlac Maze.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JPanel
 * @see java.awt.event.ActionListener
 */
public class LoopTest extends JPanel implements ActionListener {

	private static final long serialVersionUID = -2312646919940894276L;
	
	private final SerialPortPanel sio;
	private final ServerPanel svr;
	private final CommRelayPanel rp;
	private final String heading;
	private FSButton startButton;
	private SerialPort mySerialPort;
	private InputStream serialIn;
	private OutputStream serialOut;
	private SocketChannel myServerChan;
	private LoopThread testThread;
	private volatile boolean quit;

	private static final byte[] myName = {
		0101, 0102, 0103, 0104, 0105, 0106, 012
	};
	private byte[] myPosition = {
		002, 0, 001, 004, 005
	};
	
	
	public LoopTest(String heading, SerialPortPanel sio, ServerPanel svr, CommRelayPanel rp) {
		super();
		this.heading = heading;
		this.sio = sio;
		this.svr = svr;
		this.rp = rp;
		buildPanel();
	}

	public LoopTest(SerialPortPanel sio, ServerPanel svr, CommRelayPanel rp) {
		this("Maze Loop Test", sio, svr, rp);
	}
	
	public boolean startTest() {
		boolean result = true;
		quit = false;
		
		// Check if the server is running and warn if not.
		// The server may be running in another process or on another machine.
		if (!svr.isServerRunning()) {
			System.out.println(
				heading + " warning: local Maze server is not running.");
		}
		
		// Prompt to start the relay if it's not already active
		if (!rp.isActive()) {
			error("Please start the serial I/O to TCP relay");
			result = false;
		}
		
		// Open input and output streams on the serial port, and flush
		// any pending input.
		if (result) {
			mySerialPort = sio.getSelectedPort();
			if (mySerialPort != null) {
				serialIn = null;
				serialOut = null;
				int cnt = 0;
				try {
					serialIn = mySerialPort.getInputStream();
					serialOut = mySerialPort.getOutputStream();
					while (true) {
						cnt = serialIn.available();
						if (cnt <= 0)
							break;
						for (int i = 0; i < cnt; ++i) {
							serialIn.read();
						}
					}
				} catch (IOException iox) {
					System.out.println(
						heading + "I/O exception while opening serial port.");
					result = false;
				}
			} else {
				System.out.println(heading + " has no serial port.");
				result = false;
			}
		}

		// Open the connection to the server and send the username
		if (result) {
			try {
				myServerChan = SocketChannel.open();
				myServerChan.socket().setTcpNoDelay(true);
				myServerChan.connect(svr.getServerAddress());
			} catch (IOException iox) {
				result = false;
				System.out.println(
					heading + " could not connect to Maze server");
			}
		}
		
		// Start the test thread	
		if (result) {
			testThread = new LoopThread();
			testThread.setDaemon(true);
			testThread.start();
		}

		return result;
	}
	
	private void error(String message) {
		JOptionPane.showMessageDialog(
			this,
			message,
			"Operation Aborted",
			JOptionPane.ERROR_MESSAGE);
	}
	
	private boolean confirm(String message) {
		int ans =
			JOptionPane.showConfirmDialog(
				this,
				message,
				"Please confirm",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		return (ans == JOptionPane.YES_OPTION);
	}
	
	private void buildPanel() {
		float savePointSize = FSLabel.setDefaultPointSize(16.0f);
		
		setBackground(Color.white);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		Utilities.addCentered(this, new FSLabel(heading, 24.0f, Font.BOLD));
		add(Box.createVerticalStrut(20));
		
		startButton = new FSButton("Start Test");
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		
		add(Box.createVerticalStrut(20));
		Utilities.addCentered(this, startButton);
		
		FSLabel.setDefaultPointSize(savePointSize);
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if ("start".equals(cmd)) {
			if (startTest()) {
				startButton.setActionCommand("stop");
				startButton.setText("Stop Test");
				System.out.println(heading + " started successfully.");
			} else {
				System.out.println(heading + " failed to start.");
			}
		} else if ("stop".equals(cmd)) {
			startButton.setActionCommand("start");
			startButton.setText("Start Test");
			quit = true;
			testThread.interrupt();
		} else if ("exit".equals(cmd)) {
			startButton.setActionCommand("start");
			startButton.setText("Start Test");
			System.out.println(heading + " thread exit.");			
		}
	}
	
	private class LoopThread extends Thread {
		
		public void run() {
			
			ByteBuffer buf = ByteBuffer.wrap(myName);
		
			// Send my name to the server
			sendServer(buf);
			if (quit) {
				System.out.println(heading + " thread stopped.");
				notifyPanel();
				return;
			}
		
			buf = ByteBuffer.allocateDirect(256);
		
			// Receive a type 4 message with my id
			int len = 0;
			while (len < 12) {
				len += receiveServer(buf);
				if (quit) {
					System.out.println(heading + " thread stopped.");
					notifyPanel();
					return;
				}
			}
		
			byte id = buf.get(1);
			
			// Send my position to the server
			ByteBuffer posMsg = ByteBuffer.wrap(myPosition);
			posMsg.put(1, id);
			sendServer(posMsg);
			if (quit) {
				System.out.println(heading + " thread stopped.");
				notifyPanel();
				return;
			}
		
		
			// Wait for a type 4 and a type 2 message
			for (int i = 0; i < 17; ++i) {
				try {
					int b = serialIn.read();
//					System.out.print(Integer.toOctalString(b) + " ");
				} catch (IOException iox) {
					System.out.println(
						heading + " I/O exception while reading serial input.");
					notifyPanel();
					return;
				}
			}
//			System.out.println("done");
			if (quit) {
				System.out.println(heading + " thread stopped.");
				notifyPanel();
				return;
			}
		
			sun.misc.Perf p = sun.misc.Perf.getPerf();
			long frequency = p.highResFrequency();
			long tick = p.highResCounter();

			long[] time5byte = new long[32];
			byte[] rcvBuf = new byte[5];
		
			System.out.println("Beginning test of " + time5byte.length + " 5-byte updates.");
			for (int i = 0; i < time5byte.length; ++i) {
				posMsg.rewind();
				sendServer(posMsg);
				long startTick = p.highResCounter();
				if (quit) {
					System.out.println(heading + " thread stopped.");
					notifyPanel();
					return;
				}
//				System.out.println("Loop sent server a type 2: position=" + posMsg.position()
//				+ ", limit=" + posMsg.limit());
				len = 0;
				while (len < 5) {
					try {
						len += serialIn.read(rcvBuf, len, 5 - len);
//						System.out.println("Loop: received " + len + " bytes so far");
					} catch (IOException iox) {
						throw new RuntimeException(iox);
					}
					if (quit) {
						System.out.println(heading + " thread stopped.");
						notifyPanel();
						return;
					}
				}
				time5byte[i] = p.highResCounter() - startTick;
				for (int j = 0; j < myPosition.length; ++j) {
					if (myPosition[j] != rcvBuf[j]) {
						System.out.println("Loop expected " + Integer.toOctalString(myPosition[j])
						+ ", but got " + Integer.toOctalString(rcvBuf[j]));
					}
				}
			}
		
			System.out.println("Microsecond latency for 5-byte (type 2) updates: ");
			for (int i = 0; i < time5byte.length; ++i) {
				long usec = (time5byte[i] * 1000000L) / frequency;
				System.out.println(usec);
			}
				
			byte[] moveMsg = new byte[1];
			moveMsg[0] = (byte) (0150 | id);
			ByteBuffer move = ByteBuffer.wrap(moveMsg);
		
			System.out.println(
				"Beginning test of " + time5byte.length + " 1-byte updates.");
			for (int i = 0; i < time5byte.length; ++i) {
				move.rewind();
				sendServer(move);
				long startTick = p.highResCounter();
				if (quit) {
					System.out.println(heading + " thread stopped.");
					notifyPanel();
					return;
				}
				try {
					len = serialIn.read();
				} catch (IOException iox) {
					throw new RuntimeException(iox);
				}
				if (quit) {
					System.out.println(heading + " thread stopped.");
					notifyPanel();
					return;
				}
				time5byte[i] = p.highResCounter() - startTick;
			}
		
			System.out.println("Microsecond latency for 1 byte move: ");
			for (int i = 0; i < time5byte.length; ++i) {
				long usec = (time5byte[i] * 1000000L) / frequency;
				System.out.println(usec);
			}
		
			notifyPanel();
		}
	
		private int receiveServer(ByteBuffer buf) {
			int len = 0;
			try {
				len = myServerChan.read(buf);
				if (len < 0) {
					System.out.println("Loop lost server connection");
					quit = true;
				}
			} catch (IOException iox) {
				System.out.println(heading + " I/O exception on server read.");
				quit = true;
			}
			return len;
		}
	
		private void sendServer(ByteBuffer buf) {
			while (buf.hasRemaining()) {
				try {
					myServerChan.write(buf);
				} catch (IOException iox) {
					System.out.println(heading + " I/O exception on server write.");
					quit = true;
				}
			}
		}
		
		private void notifyPanel() {
			try {
				if (serialIn != null) serialIn.close();
				if (serialOut != null) serialOut.close();
				myServerChan.socket().close();
				myServerChan.close();
			} catch (IOException iox) {
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					actionPerformed(new ActionEvent(this, 0, "exit"));
				}
			});
		}
		
	}
}
