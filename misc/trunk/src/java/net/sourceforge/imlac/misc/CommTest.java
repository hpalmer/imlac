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
package net.sourceforge.imlac.misc;

import java.util.Enumeration;
import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;

/**
 * Test program for Java serial port API.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class CommTest {

	/**
	 * 
	 */
	public CommTest() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		SerialPort sport = null;
		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
			String portType = "";
			int ptype = port.getPortType();
			if (ptype == CommPortIdentifier.PORT_SERIAL) {
				portType = "serial";
				try {
					sport = (SerialPort) port.open("CommTest", 1000);
				} catch (PortInUseException pix){
					System.err.println(pix.toString());
				}
			} else if (ptype == CommPortIdentifier.PORT_PARALLEL) {
				portType = "parallel";
			} else {
				portType = "" + ptype;
			}
			System.out.println(port.getName() + ": type=" + portType
			+ ", owner=" + port.getCurrentOwner());
		}
		if (sport != null) {
			int baud = sport.getBaudRate();
			int bits = sport.getDataBits();
			int stop = sport.getStopBits();
			int parity = sport.getParity();
			System.out.println("baud=" + baud
			+ ", bits=" + bits
			+ ", stop=" + stop
			+ ", parity=" + parity);
			sport.close();
		}
	}
}
