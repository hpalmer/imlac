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
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * This class implements a fixed-size <code>JLabel</code>.  The size is fixed as soon
 * as the text is set.  It also provides for different font styles and
 * sizes.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JLabel
 */
public class FSLabel extends JLabel {

	private static final long serialVersionUID = 4629315356132217314L;
	public static float defaultPointSize = 0.0f;
	
	public static float setDefaultPointSize(float pointSize) {
		float oldSize = defaultPointSize;
		defaultPointSize = pointSize;
		return oldSize;
	}
	
	private Dimension prefSize = null;
	private boolean superInitialized = false;
	private float pointSize = 0.0f;
	private int style = Font.PLAIN;
	private Font myFont;
	
	/**
	 * Default constructor.
	 *
	 */
	public FSLabel() {
		super();
		superInitialized = true;
		setPointSize(defaultPointSize);
	}

	/**
	 * Create <code>FSLabel</code> with the specified initial text.
	 * 
	 * @param text	text for label
	 */
	public FSLabel(String text) {
		this();
		setText(text);
	}

	/**
	 * Create <code>FSLabel</code> with the specified initial text, rendered at
	 * a given point size and style.
	 * 
	 * @param text			text for label
	 * @param pointSize		point size
	 * @param style			style, e.g. <code>Font.PLAIN</code>
	 */
	public FSLabel(String text, float pointSize, int style) {
		super();
		superInitialized = true;
		setText(text, pointSize, style);
	}
	
	/**
	 * Create <code>FSLabel</code> with the specified initial text and horizontal alignment.
	 * 
	 * @param text					text for label
	 * @param horizontalAlignment	horizontal alignment, e.g. <code>Component.CENTER_ALIGNMENT</code>
	 */
	public FSLabel(String text, int horizontalAlignment) {
		this();
		setHorizontalAlignment(horizontalAlignment);
		setText(text);
	}

	/**
	 * Create <code>FSLabel</code with a specified icon.
	 * 
	 * @param image		icon for label
	 */
	public FSLabel(Icon image) {
		super(image);
		superInitialized = true;
		setPointSize(defaultPointSize);
	}

	/**
	 * Create <code>FSLabel</code> with a specified icon and horizontal alignment.
	 * 
	 * @param image					icon for label
	 * @param horizontalAlignment	horizontal alignment, e.g. <code>Component.CENTER_ALIGNMENT</code>
	 */
	public FSLabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
		superInitialized = true;
		setPointSize(defaultPointSize);
	}

	/**
	 * Create <code>FSLabel</code> with the specified initial text, icon, and horizontal
	 * alignment.
	 * 
	 * @param text					text for label
	 * @param image					icon for label
	 * @param horizontalAlignment	horizontal alignment, e.g. <code>Component.CENTER_ALIGNMENT</code>
	 */
	public FSLabel(String text, Icon image, int horizontalAlignment) {
		this(image, horizontalAlignment);
		setText(text);
	}
	
	/**
	 * Set the text in a label.
	 * 
	 * @param text	new text for the label
	 */
	@Override
	public void setText(String text) {
		if (superInitialized) {
			super.setText(text);
			if (myFont == null) {
				setMyFont();
			}
			capturePreferredSize();
		} else {
			super.setText(text);
		}
	}

	/**
	 * Set the text and point size for a label.
	 * 
	 * @param text			new text for the label
	 * @param pointSize		point size to be used to render label text
	 */
	public void setText(String text, float pointSize) {
		super.setText(text);
		if (pointSize != this.pointSize) {
			setPointSize(pointSize);
		}
		capturePreferredSize();
	}
	
	/**
	 * Set the text, point size, and style for a label.
	 * 
	 * @param text			new text for label
	 * @param pointSize		point size for rendering text
	 * @param style			style for rendering text, e.g. <code>Font.PLAIN</code>
	 */
	public void setText(String text, float pointSize, int style) {
		super.setText(text);
		this.style = style;
		if (this.pointSize != pointSize) {
			setPointSize(pointSize);
		}
		if (myFont == null) {
			setMyFont();
		}
		capturePreferredSize();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return (prefSize == null) ? super.getPreferredSize() : prefSize;
	}

	/**
	 * Set the point size used to render a label's text.
	 * 
	 * @param pointSize		the new point size
	 */
	public void setPointSize(float pointSize) {
		this.pointSize = pointSize;
		setMyFont();
	}
	
	/**
	 * Set the font style used to render a label's text.
	 * 
	 * @param style		the new style, e.g. <code>Font.PLAIN</code>
	 */
	public void setTextStyle(int style) {
		this.style = style;
		Font f = getFont();
		setFont(f.deriveFont(style));
	}
	
	private void setMyFont() {
		if ((pointSize != 0.0f) || (style != Font.PLAIN)) {
			String saveText = super.getText();
			boolean resetText = false;
			Font f = getFont();
			if (f == null) {
				super.setText(" ");
				resetText = true;
				f = getFont();
			}
			if (pointSize == 0.0f) {
				f = f.deriveFont(style);
			} else {
				f = f.deriveFont(style, pointSize);
			}
			myFont = f;
			setFont(f);
			if (resetText) {
				super.setText(saveText);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void useDefaultPointSize() {
		if ((pointSize == 0.0f) && (defaultPointSize != 0.0f)) {
			setPointSize(defaultPointSize);
		}
	}
	
	private Dimension capturePreferredSize() {
		prefSize = super.getPreferredSize();
		if (prefSize != null) {
			prefSize = new Dimension(prefSize);
		}
		return prefSize;
	}
}
