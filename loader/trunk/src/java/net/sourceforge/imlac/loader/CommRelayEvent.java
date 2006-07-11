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

import java.util.EventObject;

/**
 * This class is used to is used to present information to clients of
 * communication relay objects, such as SerialTCPRelay.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see java.util.EventObject
 */
public class CommRelayEvent extends EventObject {

	private static final long serialVersionUID = -8482098477827707618L;
	
	public static final int EVENT_STATUSCHANGE = 1;
	public static final int EVENT_CONNFAILURE = 2;
	public static final int EVENT_SERIALINFAILURE = 3;
	public static final int EVENT_SERIALOUTFAILURE = 4;
	public static final int EVENT_SERIALOUTEXIT = 5;
	public static final int EVENT_SERIALINEXIT = 6;
	
	private String message;
	private int type;

	/**
	 * @param source
	 */
	public CommRelayEvent(Object source) {
		super(source);
	}

	public CommRelayEvent(Object source, int type) {
		super(source);
		this.type = type;
	}
	
	public CommRelayEvent(Object source, int type, String message) {
		super(source);
		this.type = type;
		this.message = message;
	}
	
	public int getType() {
		return type;
	}
	
	public String getMessage() {
		return message;
	}
}
