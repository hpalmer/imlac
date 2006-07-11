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

import java.io.Serializable;

import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

/**
 * A simple class just to save settings for a serial port.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see java.io.Serializable
 */
public class SerialPortSettings implements Serializable {

	private static final long serialVersionUID = 3490614749542236801L;
	
	private int baudRate;
	private int dataBits;
	private int parity;
	private int stopBits;
	private int flowControl;
	
	/**
	 * 
	 */
	public SerialPortSettings() {
		super();
		baudRate = 9600;
		dataBits = SerialPort.DATABITS_8;
		parity = SerialPort.PARITY_NONE;
		stopBits = SerialPort.STOPBITS_1;
		flowControl = SerialPort.FLOWCONTROL_NONE;
	}

	public SerialPortSettings(SerialPort port) {
		super();
		getSettings(port);
	}
	
	public void getSettings(SerialPort port) {
		baudRate = port.getBaudRate();
		dataBits = port.getDataBits();
		parity = port.getParity();
		stopBits = port.getStopBits();
		flowControl = port.getFlowControlMode();
	}
	
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof SerialPortSettings) {
			SerialPortSettings other = (SerialPortSettings) obj;
			result = (baudRate == other.baudRate) &&
					 (dataBits == other.dataBits) &&
					 (parity == other.parity) &&
					 (stopBits == other.stopBits) &&
					 (flowControl == other.flowControl);
		}
		return result;
	}
	
	public boolean apply(SerialPort port) {
		boolean result = true;
		try {
			port.setSerialPortParams(baudRate, dataBits, stopBits, parity);
			port.setFlowControlMode(flowControl);
		} catch (UnsupportedCommOperationException uco) {
			result = false;
			System.out.println("Failed to apply settings to port");
		}
		return result;
	}
	
	public int getBaudRate() {
		return baudRate;
	}
	
	public int getDataBits() {
		return dataBits;
	}
	
	public int getFlowControl() {
		return flowControl;
	}

	public int getParity() {
		return parity;
	}

	public int getStopBits() {
		return stopBits;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
	}

	public void setFlowControl(int flowControl) {
		this.flowControl = flowControl;
	}

	public void setParity(int parity) {
		this.parity = parity;
	}

	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}
}
