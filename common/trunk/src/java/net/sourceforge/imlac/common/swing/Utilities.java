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
package net.sourceforge.imlac.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JComponent;

/**
 * This class contains various static methods to perform useful operations
 * on Swing components.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class Utilities {

	protected Utilities() {
		super();
	}

	/**
	 * Add a <code>JComponent</code> to a <code>Container</code> with
	 * <code>CENTER_ALIGNMENT</code>.
	 * 
	 * @param c		the container
	 * @param elem	the component
	 */
	public static void addCentered(Container c, JComponent elem) {
		elem.setAlignmentX(Component.CENTER_ALIGNMENT);
		c.add(elem);
	}

	/**
	 * Set the maximum size of a component to be its currently preferred size.
	 * 
	 * @param c		the component
	 * @return		the preferred and now maximum size
	 */
	public static Dimension fixComponentSize(JComponent c) {
		Dimension dim;
		dim = c.getPreferredSize();
		c.setMaximumSize(dim);
		return dim;
	}
	
	/**
	 * Set the maximum height of a component to its currently preferred size.
	 * This does not change the maximum width of the component.
	 * 
	 * @param c		the component
	 * @return		the maximum size of the component
	 */
	public static Dimension fixComponentHeight(JComponent c) {
		Dimension pdim = c.getPreferredSize();
		Dimension mdim = c.getMaximumSize();
		mdim.height = pdim.height;
		c.setMaximumSize(mdim);
		return mdim;
	}
}
