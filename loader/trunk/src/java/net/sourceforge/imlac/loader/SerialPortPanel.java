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

import net.sourceforge.imlac.common.swing.FSButton;
import net.sourceforge.imlac.common.swing.FSLabel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class creates a panel to enable a serial port to be selected
 * and its parameters to be set.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JPanel
 * @see java.awt.event.ActionListener
 */
public class SerialPortPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 6707692166047496632L;

	private static final Integer[] baudRates = {
		new Integer(300),
		new Integer(1200),
		new Integer(2400),
		new Integer(4800),
		new Integer(9600),
		new Integer(19200),
		new Integer(38400),
		new Integer(57600),
		new Integer(115200)
	};
	
	private static final Integer[] dataBits = {
		new Integer(SerialPort.DATABITS_8),
		new Integer(SerialPort.DATABITS_7),
		new Integer(SerialPort.DATABITS_6),
		new Integer(SerialPort.DATABITS_5)
	};
	
	private static final String[] stopBits = {
		"1",
		"1.5",
		"2"
	};
	
	private static final int[] stopBitModes = {
		SerialPort.STOPBITS_1,
		SerialPort.STOPBITS_1_5,
		SerialPort.STOPBITS_2
	};
	
	private static final String[] parityStrings = {
		"None",
		"Even",
		"Odd",
		"Mark",
		"Space"
	};
	
	private static final int[] parityModes = {
		SerialPort.PARITY_NONE,
		SerialPort.PARITY_EVEN,
		SerialPort.PARITY_ODD,
		SerialPort.PARITY_MARK,
		SerialPort.PARITY_SPACE
	};
	
	private static final String[] flowStrings = {
		"None",
		"RTSCTS",
		"XONXOFF"
	};
	
	private static final int[] flowInModes = {
		SerialPort.FLOWCONTROL_NONE,
		SerialPort.FLOWCONTROL_RTSCTS_IN,
		SerialPort.FLOWCONTROL_XONXOFF_IN
	};
	
	private static final int[] flowOutModes = {
		SerialPort.FLOWCONTROL_NONE,
		SerialPort.FLOWCONTROL_RTSCTS_OUT,
		SerialPort.FLOWCONTROL_XONXOFF_OUT
	};
	
	private String heading;
	private SerialPortSettings savedSettings;
	private SerialPortSettings newSettings;
	private final ArrayList<CommPortIdentifier> portIdentifiers
									= new ArrayList<CommPortIdentifier>(8);
	private String[] portNames;
	private FSLabel currentBaud;
	private FSLabel currentData;
	private FSLabel currentStop;
	private FSLabel currentParity;
	private FSLabel currentFlowIn;
	private FSLabel currentFlowOut;
	private JComboBox portSelector;
	private JComboBox baudSelector;
	private JComboBox dataSelector;
	private JComboBox stopSelector;
	private JComboBox paritySelector;
	private JComboBox flowInSelector;
	private JComboBox flowOutSelector;
	private FSButton applyButton;
	
	private SerialPort selectedPort = null;
	
	public SerialPortPanel() {
		this("Serial Port Selection and Settings");
	}
	
	public SerialPortPanel(String heading) {
		super();
		savedSettings = new SerialPortSettings();
		newSettings = new SerialPortSettings();
		this.heading = heading;
		buildPanel();
	}
	
	public SerialPort getSelectedPort() {
		return selectedPort;
	}
	
	private void buildPanel() {
		Enumeration<CommPortIdentifier> portList = (Enumeration<CommPortIdentifier>)CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier port = portList.nextElement();
			if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				portIdentifiers.add(port);
			}
		}
		
		portNames = new String[portIdentifiers.size()];
		for (int i = 0; i < portNames.length; ++i) {
			CommPortIdentifier port = portIdentifiers.get(i);
			portNames[i] = port.getName();
		}
		
		float savePointSize = FSLabel.setDefaultPointSize(16.0f);
		
		applyButton = new FSButton("Set Parameters");
		applyButton.setEnabled(false);
		applyButton.setActionCommand("ok");
		applyButton.addActionListener(this);
		
		setBackground(Color.white);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		addCentered(this, new FSLabel(heading, 24.0f, Font.BOLD));
		
		JPanel gPanel = new JPanel();
		GridLayout glayout = new GridLayout(0, 3);
		glayout.setHgap(30);
		glayout.setVgap(10);
		gPanel.setLayout(glayout);
		gPanel.setBackground(Color.white);
		

		gPanel.add(new FSLabel("Select Port:", 18.0f, Font.BOLD));
		portSelector = new JComboBox(portNames);
		portSelector.setActionCommand("port");
		portSelector.addActionListener(this);
		
		gPanel.add(portSelector);
		gPanel.add(new FSLabel(""));
		
		gPanel.add(new FSLabel("Port Settings", 18.0f, Font.BOLD));
		gPanel.add(new FSLabel("Current", 18.0f, Font.ITALIC));
		gPanel.add(new FSLabel("Desired", 18.0f, Font.ITALIC));
		
		gPanel.add(new FSLabel("Baud Rate"));
		currentBaud = new FSLabel();
		gPanel.add(currentBaud);
		baudSelector = new JComboBox(baudRates);
		fixComponentSize(baudSelector);
		baudSelector.setActionCommand("parm");
		baudSelector.addActionListener(this);
		gPanel.add(baudSelector);
		
		gPanel.add(new FSLabel("Data Bits:"));
		currentData = new FSLabel();
		gPanel.add(currentData);
		dataSelector = new JComboBox(dataBits);
		fixComponentSize(dataSelector);
		dataSelector.setActionCommand("parm");
		dataSelector.addActionListener(this);
		gPanel.add(dataSelector);
		
		gPanel.add(new FSLabel("Stop Bits:"));
		currentStop = new FSLabel();
		gPanel.add(currentStop);
		stopSelector = new JComboBox(stopBits);
		fixComponentSize(stopSelector);
		stopSelector.setActionCommand("parm");
		stopSelector.addActionListener(this);
		gPanel.add(stopSelector);
		
		gPanel.add(new FSLabel("Parity:"));
		currentParity = new FSLabel();
		gPanel.add(currentParity);
		paritySelector = new JComboBox(parityStrings);
		fixComponentSize(paritySelector);
		paritySelector.setActionCommand("parm");
		paritySelector.addActionListener(this);
		gPanel.add(paritySelector);
		
		gPanel.add(new FSLabel("Flow Control In:"));
		currentFlowIn = new FSLabel();
		gPanel.add(currentFlowIn);
		flowInSelector = new JComboBox(flowStrings);
		fixComponentSize(flowInSelector);
		flowInSelector.setActionCommand("parm");
		flowInSelector.addActionListener(this);
		gPanel.add(flowInSelector);
		
		gPanel.add(new FSLabel("Flow Control Out:"));
		currentFlowOut = new FSLabel();
		gPanel.add(currentFlowOut);
		flowOutSelector = new JComboBox(flowStrings);
		fixComponentSize(flowOutSelector);
		flowOutSelector.setActionCommand("parm");
		flowOutSelector.addActionListener(this);
		gPanel.add(flowOutSelector);
		fixComponentSize(gPanel);
		
		add(Box.createVerticalStrut(20));
		add(gPanel);
		
		add(Box.createVerticalStrut(20));
		addCentered(this, applyButton);

		FSLabel.setDefaultPointSize(savePointSize);
		
		baudSelector.setSelectedIndex(4);
		
		selectedPort = null;
		
		for (int i = 0; i < portIdentifiers.size(); ++i) {
			CommPortIdentifier portId = (CommPortIdentifier) portIdentifiers.get(0);
			portSelector.setSelectedIndex(i);
			if (selectedPort != null) {
				break;
			}
		}
		
		if (selectedPort == null) {
			portSelector.setSelectedIndex(-1);
			System.out.println("SerialPortPanel could not find any unused ports.");
		}
	}
	
	private String getStopBitsString(int stopBits) {
		String result;
		switch (stopBits) {
			case SerialPort.STOPBITS_1:
				result = "1";
				break;
			case SerialPort.STOPBITS_1_5:
				result = "1.5";
				break;
			case SerialPort.STOPBITS_2:
				result = "2";
				break;
			default:
				result = "? " + Integer.toString(stopBits);
		}
		return result;
	}
	
	private String getParityString(int parity) {
		String result = "*unknown*";
		for (int i = 0; i < parityModes.length; ++i) {
			if (parity == parityModes[i]) {
				result = parityStrings[i];
				break;
			}
		}
		return result;
	}
	
	private String getInFlowString(int mode) {
		String result = "None";
		
		if ((mode & SerialPort.FLOWCONTROL_RTSCTS_IN) != 0) {
			result = "RTSCTS";
		} else if ((mode & SerialPort.FLOWCONTROL_XONXOFF_IN) != 0) {
			result = "XONXOFF";
		}
		return result;
	}

	private String getOutFlowString(int mode) {
		String result = "None";
		
		if ((mode & SerialPort.FLOWCONTROL_RTSCTS_OUT) != 0) {
			result = "RTSCTS";
		} else if ((mode & SerialPort.FLOWCONTROL_XONXOFF_OUT) != 0) {
			result = "XONXOFF";
		}
		return result;
	}

	private void addCentered(Container c, JComponent elem) {
		elem.setAlignmentX(Component.CENTER_ALIGNMENT);
		c.add(elem);
	}

	private Dimension fixComponentSize(JComponent c) {
		Dimension dim;
		dim = c.getPreferredSize();
		c.setMaximumSize(dim);
		return dim;
	}
	
	private void updateCurrent() {
		currentBaud.setText(Integer.toString(savedSettings.getBaudRate()));
		currentData.setText(Integer.toString(savedSettings.getDataBits()));
		currentStop.setText(getStopBitsString(savedSettings.getStopBits()));
		currentParity.setText(getParityString(savedSettings.getParity()));
		currentFlowIn.setText(getInFlowString(savedSettings.getFlowControl()));
		currentFlowOut.setText(getOutFlowString(savedSettings.getFlowControl()));
	}

	private void updateNew() {
		newSettings.setBaudRate(((Integer) baudSelector.getSelectedItem()).intValue());
		newSettings.setDataBits(((Integer) dataSelector.getSelectedItem()).intValue());
		int i = paritySelector.getSelectedIndex();
		newSettings.setParity(parityModes[i]);
		i = flowInSelector.getSelectedIndex();
		int flow = flowInModes[i];
		i = flowOutSelector.getSelectedIndex();
		flow |= flowOutModes[i];
		newSettings.setFlowControl(flow);
		i = stopSelector.getSelectedIndex();
		newSettings.setStopBits(stopBitModes[i]);
	}
	
	private SerialPort openPort(CommPortIdentifier portId) {
		SerialPort port = null;
		try {
			port = (SerialPort) portId.open(this.getClass().toString(), 1000);
			savedSettings = new SerialPortSettings(port);
			updateCurrent();
		} catch (PortInUseException pix) {
			System.out.println("Port " + portId.getName() + " is already in use.");
		}
		return port;
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if ("ok".equals(cmd)) {
			updateNew();
			newSettings.apply(selectedPort);
			savedSettings.getSettings(selectedPort);
			updateCurrent();
		} else if ("port".equals(cmd)) {
			int i = portSelector.getSelectedIndex();
			if (selectedPort != null) {
				selectedPort.close();
				selectedPort = null;
			}
			if (i >= 0) {
				CommPortIdentifier portId = portIdentifiers.get(i);
				selectedPort = openPort(portId);
				if (selectedPort == null) {
					portSelector.setSelectedIndex(-1);
				}
			}
		} else if ("parm".equals(cmd)) {
			updateNew();
		}
		applyButton.setEnabled(!savedSettings.equals(newSettings));
	}
	
	public static void main(String[] args) {
		SerialPortPanel sp = new SerialPortPanel();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(sp);
		frame.pack();
		frame.setVisible(true);
	}
}
