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

import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This class extends <code>JTextField</code> to provide a field for reading
 * integer values.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JTextField
 */
public class WholeNumberField extends JTextField {

	private static final long serialVersionUID = -8291303177448845391L;
	private Toolkit toolkit;
	private NumberFormat integerFormatter;

	/**
	 * Create a new <code>WholeNumberField</code> with a given initial value and
	 * a specified width, in columns.
	 * 
	 * @param value		the initial value
	 * @param columns	the width of the field, in columns
	 */
	public WholeNumberField(int value, int columns) {
		super(columns);
		toolkit = Toolkit.getDefaultToolkit();
		integerFormatter = NumberFormat.getNumberInstance(Locale.US);
		integerFormatter.setParseIntegerOnly(true);
		setValue(value);
	}

	/**
	 * Return the current value of the field.
	 * 
	 * @return	the current value
	 */
	public int getValue() {
		int retVal = 0;
		try {
			retVal = integerFormatter.parse(getText()).intValue();
		} catch (ParseException e) {
			// This should never happen because insertString allows
			// only properly formatted data to get in the field.
			toolkit.beep();
		}
		return retVal;
	}

	/**
	 * Set the value of the field.
	 * 
	 * @param value		the new value
	 */
	public void setValue(int value) {
		setText(integerFormatter.format(value));
	}

	protected Document createDefaultModel() {
		return new WholeNumberDocument();
	}

	protected class WholeNumberDocument extends PlainDocument {

		private static final long serialVersionUID = 7457878190607055695L;

		public void insertString(int offs, String str, AttributeSet a)
			throws BadLocationException {
			for (int i = 0; i < str.length(); ++i) {
				if (!Character.isDigit(str.charAt(i))) {
					toolkit.beep();
					str = str.substring(0, i);
					break;
				}
			}
			super.insertString(offs, str, a);
		}
	}
}
