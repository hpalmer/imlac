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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import javax.comm.SerialPort;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.imlac.common.swing.FSButton;
import net.sourceforge.imlac.common.swing.FSLabel;
import net.sourceforge.imlac.common.swing.Utilities;
import net.sourceforge.imlac.mazeserver.ServerPanel;

/**
 * This class implements a function that relays a serial port to a TCP/IP connection.
 * It was developed for the Vintage Computer Fair 2004, as part of the effort to
 * get Mazewar working between an Imlac and the MIT Mazewar server running on a
 * simulated KL-10.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class CommRelayPanel
	extends JPanel
	implements ActionListener, CommRelayEventListener {
		
	private static final long serialVersionUID = -936106432571952937L;

	private static final String[] destNameValues = {
		"localhost"
	};
	
	private static final Integer[] destPortValues = {
		new Integer(8082)
	};
	
	private static final Charset ascii = Charset.forName("US-ASCII");
	private static final CharsetEncoder asciiEncoder = ascii.newEncoder();

	private final String heading;
	private final SerialPortPanel portPanel;
	private final ServerPanel serverPanel;
	
	private FSLabel portLabel;
	private FSLabel statusLabel;
	private JComboBox destNameBox;
	private JComboBox destPortBox;
	private JTextField userNameField;
	private JSlider throttleSlider;
	private JCheckBox serialInTraceBox;
	private JCheckBox serialOutTraceBox;
	private FSButton startButton;
	private SerialTCPRelay relay;
	
	public CommRelayPanel(SerialPortPanel portPanel, ServerPanel serverPanel) {
		this(portPanel, serverPanel, "Serial I/O To TCP/IP Relay");
	}

	public CommRelayPanel(
		SerialPortPanel portPanel,
		ServerPanel serverPanel,
		String heading) {
		super();
		this.heading = heading;
		this.portPanel = portPanel;
		this.serverPanel = serverPanel;
		relay = null;
		buildPanel();
	}
	
	public boolean isActive() {
		return (relay != null) && (relay.getStatus() == SerialTCPRelay.STATUS_ACTIVE);
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if ("start".equals(cmd)) {
			startButton.setActionCommand("stop");
			startButton.setText("Stop Relay");
			String destName = (String) destNameBox.getSelectedItem();
			Integer destPort = (Integer) destPortBox.getSelectedItem();
//			InetSocketAddress destAddr = serverPanel.getServerAddress();
			InetSocketAddress destAddr =
				new InetSocketAddress(destName, destPort.intValue());
			String username = userNameField.getText() + "\n";
			CharBuffer userBuf = CharBuffer.wrap(username);
			ByteBuffer userBytes = null;
			try {
				userBytes = asciiEncoder.encode(userBuf);
			} catch (CharacterCodingException ccx) {
			}
			relay = new SerialTCPRelay(portPanel.getSelectedPort());
			relay.setSocketRemoteAddress(destAddr);
			relay.addCommRelayEventListener(this);
			if (userBytes != null) {
				relay.setInitialData(userBytes);
			}
			int n = throttleSlider.getValue();
			relay.setThrottle(n);
			System.out.println(
				"Serial output throttle set to "
					+ n
					+ " milliseconds per byte");
			setTraceFlags();
			relay.start();
		} else if ("stop".equals(cmd)) {
			relay.close();
			startButton.setActionCommand("start");
			startButton.setText("Start Relay");
			relay = null;
		}
	}
	public void commRelayEventNotice(CommRelayEvent event) {
		int type = event.getType();
		String message = event.getMessage();
		if (type == CommRelayEvent.EVENT_STATUSCHANGE) {
			statusLabel.setText(message);
			if ((relay == null)
				|| (relay.getStatus() == SerialTCPRelay.STATUS_INACTIVE)) {
				startButton.setText("Start Relay");
				startButton.setActionCommand("start");
			}
		} else {
			System.out.println("CommRelayEvent: id=" + event.getType());
			if (event.getMessage() != null) {
				System.out.println(event.getMessage());
			}
		}
	}

	private void setTraceFlags() {
		int trace = 0;
		if (serialInTraceBox.isSelected()) trace = SerialTCPRelay.TRACE_SERIAL_IN;
		if (serialOutTraceBox.isSelected()) trace |= SerialTCPRelay.TRACE_SERIAL_OUT;
		if (relay != null) relay.setTrace(trace);
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
		
		hPane.add(new FSLabel("Current Serial Port:", 18.0f, Font.PLAIN));
		hPane.add(Box.createHorizontalStrut(6));
		portLabel = new FSLabel();
		SerialPort port = portPanel.getSelectedPort();
		if (port != null) {
			portLabel.setText(port.getName(), 18.0f, Font.PLAIN);
		}
		hPane.add(portLabel);
		hPane.add(Box.createHorizontalGlue());
		hPane.add(new FSLabel("Relay Status:", 18.0f, Font.PLAIN));
		hPane.add(Box.createHorizontalStrut(6));
		statusLabel = new FSLabel("Inactive", 18.0f, Font.ITALIC);
		hPane.add(statusLabel);
		
		Utilities.fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		Font f;
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Server Name or IP Address:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));

		destNameBox = new JComboBox(destNameValues);
		destNameBox.setEditable(true);
		f = destNameBox.getFont();
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
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Server Command:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));

		userNameField = new JTextField(40);
		f = userNameField.getFont();
		if (f != null) {
			f = f.deriveFont(16.0f);
			userNameField.setFont(f);
		}
		Utilities.fixComponentSize(userNameField);
		
		hPane.add(userNameField);
		hPane.add(Box.createHorizontalGlue());
		Utilities.fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Serial Output Throttle (ms/byte):", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));

		throttleSlider = new JSlider(0, 10, 0);
		throttleSlider.setBackground(Color.white);
		throttleSlider.setMajorTickSpacing(1);
		throttleSlider.setPaintTicks(true);
		throttleSlider.setPaintLabels(true);
		throttleSlider.setPaintTrack(true);
		throttleSlider.setSnapToTicks(true);
		throttleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				if (!throttleSlider.getValueIsAdjusting()) {
					if (relay != null) {
						int n = throttleSlider.getValue();
						if (relay != null) {
							relay.setThrottle(n);
							System.out.println(
								"Serial output throttle set to "
									+ n
									+ " milliseconds per byte");
						}
					}
				}
			}
		});
		hPane.add(throttleSlider);
		Utilities.fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		serialInTraceBox = new JCheckBox("Trace serial input");
		serialInTraceBox.setBackground(Color.WHITE);
		serialInTraceBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				if (relay != null) {
					setTraceFlags();
				}
			}
		});
		add(serialInTraceBox);
		
		serialOutTraceBox = new JCheckBox("Trace serial output");
		serialOutTraceBox.setBackground(Color.WHITE);
		serialOutTraceBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				if (relay != null) {
					setTraceFlags();
				}
			}
		});
		add(serialOutTraceBox);
		
		startButton = new FSButton("Start Relay");
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		
		add(Box.createVerticalStrut(20));
		Utilities.addCentered(this, startButton);
		
		FSLabel.setDefaultPointSize(savePointSize);
	}	
}
