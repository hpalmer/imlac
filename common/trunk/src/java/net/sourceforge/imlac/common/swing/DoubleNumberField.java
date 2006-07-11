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

import java.text.DecimalFormat;

import javax.swing.JFormattedTextField;

/**
 * This class extends <code>JFormattedTextField</code> to provide a specialized
 * field for reading <code>double</code> values.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JFormattedTextField
 */
public class DoubleNumberField extends JFormattedTextField {

	private static final long serialVersionUID = 3636746245294166271L;
	private int columns;
	private DecimalFormat doubleFormatter;

	private DoubleNumberField(DecimalFormat fmt) {
		super(fmt);
		doubleFormatter = (DecimalFormat) fmt;
		doubleFormatter.setParseIntegerOnly(false);
		doubleFormatter.setMaximumFractionDigits(6);
		doubleFormatter.setMinimumIntegerDigits(0);
		setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
	}

	/**
	 * Create a <code>DoubleNumberField</code> containing a specified initial
	 * value, with a specified width in columns.
	 * 
	 * @param value		the initial value
	 * @param columns	the width of the field in columns
	 */
	public DoubleNumberField(double value, int columns) {
		this(new DecimalFormat("#.#"));
		this.columns = columns;
		setValue(value);
	}

	/**
	 * Returns the current <code>double</code> value in the field.
	 * 
	 * @return	the <code>double</code> value
	 */
	public double getDoubleValue() {
		return ((Number) getValue()).doubleValue();
	}

	/**
	 * Set a specified <code>double</code> value as the value in the field.
	 * 
	 * @param value		the <code>double</code> value
	 */
	public void setValue(double value) {
		setValue(new Double(value));
		setColumns(columns);
	}
}
