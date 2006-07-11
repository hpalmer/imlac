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

import java.awt.Dimension;

import javax.swing.JButton;

/**
 * This class implements a fixed-size <code>JButton</code>.  The size is determined
 * by the preferred size of the button after its initial text is set.
 *
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JButton
 */
public class FSButton extends JButton {

	private static final long serialVersionUID = -8345311851519805676L;
	private Dimension myPrefSize;

	/**
	 * Create a <code>FSButton</code> labeled with the specified text.
	 * 
	 * @param text	text for the label
	 */
	public FSButton(String text) {
		super(text);

		myPrefSize = super.getPreferredSize();
		if (myPrefSize != null) {

			myPrefSize = new Dimension(myPrefSize);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return (myPrefSize == null) ? super.getPreferredSize() : myPrefSize;
	}

}
