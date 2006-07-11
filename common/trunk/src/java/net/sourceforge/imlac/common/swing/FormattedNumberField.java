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

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * This class extends <code>JFormattedTextField</code> to provide a specialized
 * field for reading numbers.  A default value, minimum value, and maximum value
 * may also be specified.  It can read Integer or Double types in various formats,
 * including plain, percent, and dollars.
 * 
 * @author Howard Palmer
 * @version $Id: FormattedNumberField.java 109 2005-10-24 06:57:52Z Howard $
 */
public class FormattedNumberField extends JFormattedTextField {

	private static final long serialVersionUID = -9076768259346918016L;

	/**
	 * Read number as an <code>Integer</code>.
	 */
	public static final int TYPE_INTEGER = 0;
	
	/**
	 * Read number as a <code>Double</code>.
	 */
	public static final int TYPE_DOUBLE = 1;
	
	/**
	 * Next available type code.
	 */
	public static final int TYPE_MAX = 2;
	
	/**
	 * Read the number in plain format.
	 */
	public static final int FORMAT_PLAIN = 0;
	
	/**
	 * Read the number as a percentage.
	 */
	public static final int FORMAT_PERCENT = 1;
	
	/**
	 * Read the number as dollars.
	 */
	public static final int FORMAT_DOLLARS = 2;
	
	/**
	 * Next available format code.
	 */
	public static final int FORMAT_MAX = 3;
	
	private int valueType;
	private int valueFormat;
	private Number minValue;
	private Number maxValue;
	private Number defaultValue;
	private Number minFract;
	private Number maxFract;
	
	/**
	 * Constructs a new <code>FormattedNumberField</code> with a specified
	 * type and format.
	 * 
	 * @param valueType		specifies the type (one of <code>TYPE_</code>xxxx).
	 * @param valueFormat	specifies the format (one of <code>FORMAT_</code>xxxx).
	 * @throws IllegalArgumentException if <code>valueType</code> or <code>valueFormat</code>
	 * are not valid.
	 */
	public FormattedNumberField(int valueType, int valueFormat) {
		super();
		
		if ((valueType < 0) || (valueType >= TYPE_MAX)) {
			throw new IllegalArgumentException("invalid valueType");
		}
		if ((valueFormat < 0) || (valueFormat >= FORMAT_MAX)) {
			throw new IllegalArgumentException("invalid valueFormat");
		}
		this.valueType = valueType;
		this.valueFormat = valueFormat;
		setFormatterFactory(makeFormatterFactory());
	}
	
	/**
	 * Constructs a new <code>FormattedNumberField</code> with a specified type,
	 * format, and default value.  The field is created with the default value
	 * showing.
	 * 
	 * @param valueType		specifies the type (one of <code>TYPE_</code>xxxx).
	 * @param valueFormat	specifies the format (one of <code>FORMAT_</code>xxxx).
	 * @param defaultValue	specifies the default value.
	 * @throws IllegalArgumentException if <code>valueType</code> or <code>valueFormat</code>
	 * are not valid.
	 */
	public FormattedNumberField(int valueType, int valueFormat, Number defaultValue) {
		this(valueType, valueFormat);
		setDefaultValue(defaultValue);
	}
	
	/**
	 * Constructs a new <code>FormattedNumberField</code> with a specified type,
	 * format, default value, minimum value, and maximum value.  The field is
	 * created with the default value showing.
	 * 
	 * @param valueType		specifies the type (one of <code>TYPE_</code>xxxx).
	 * @param valueFormat	specifies the format (one of <code>FORMAT_</code>xxxx).
	 * @param defaultValue	specifies the default value.
	 * @param minValue		specifies the minimum value.
	 * @param maxValue		specifies the maximum value.
	 * @throws IllegalArgumentException if <code>valueType</code> or <code>valueFormat</code>
	 * are not valid.
	 */
	public FormattedNumberField(
		int valueType,
		int valueFormat,
		Number defaultValue,
		Number minValue,
		Number maxValue) {
			
		this(valueType, valueFormat);
		setDefaultValue(defaultValue);
		setAllowedRange(minValue, maxValue);
	}
	
	/**
	 * Sets the minimum and maximum values allowed for this field.
	 * 
	 * @param minValue	the minimum value.
	 * @param maxValue	the maximum value.
	 */
	public void setAllowedRange(Number minValue, Number maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		if ((minValue != null) || (maxValue != null)) {
			setInputVerifier(new RangeVerifier());
		}
	}
	
	/**
	 * Returns the default value of this field.
	 * 
	 * @return	the default value.
	 */
	public Number getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * Sets the default value of this field.  This value will be showing in the
	 * field when it is displayed.
	 * 
	 * @param value	the default value.
	 */
	public void setDefaultValue(Number value) {
		Number dv = null;
		if (value != null) {
			switch (valueType) {
				case TYPE_INTEGER:
					dv = new Integer(value.intValue());
					break;
				case TYPE_DOUBLE:
					dv = new Double(value.doubleValue());
					break;
			}
		}
		this.defaultValue = dv;
		setValue(dv);
	}
	
	/**
	 * Set the minimum and maximum number of digits in the fraction part of a
	 * <code>TYPE_DOUBLE</code> field.  Has no effect on a <code>TYPE_INTEGER</code>
	 * field.
	 * 
	 * @param minFract	the minimum number of fractional digits.
	 * @param maxFract	the maximum number of fractional digits.
	 */
	public void setFractionRange(Number minFract, Number maxFract) {
		this.minFract = minFract;
		this.maxFract = maxFract;
		setFormatterFactory(makeFormatterFactory());
	}
	
	private DefaultFormatterFactory makeFormatterFactory() {
		DefaultFormatterFactory factory;
		NumberFormatter displayFormatter = null;
		NumberFormatter editFormatter = null;
		NumberFormat df;
		NumberFormat ef;
		
		switch (valueType) {
			case TYPE_INTEGER:
				switch (valueFormat) {
					case FORMAT_PLAIN:
					default:
						df = NumberFormat.getIntegerInstance();
						ef = NumberFormat.getIntegerInstance();
						break;
					case FORMAT_PERCENT:
						df = NumberFormat.getPercentInstance();
						df.setParseIntegerOnly(true);
						ef = null;
						editFormatter = new PercentFormatter(valueType, minFract, maxFract);
						break;
					case FORMAT_DOLLARS:
						df = NumberFormat.getCurrencyInstance();
						df.setMaximumFractionDigits(0);
						df.setParseIntegerOnly(true);
						ef = NumberFormat.getIntegerInstance();
						break;
				}
				displayFormatter = new NumberFormatter(df);
				if (editFormatter == null)
					editFormatter = new NumberFormatter(ef);
				break;
			case TYPE_DOUBLE:
				switch (valueFormat) {
					case FORMAT_PLAIN:
					default:
						df = NumberFormat.getNumberInstance();
						ef = NumberFormat.getNumberInstance();
						break;
					case FORMAT_PERCENT:
						df = NumberFormat.getPercentInstance();
						ef = null;
						editFormatter = new PercentFormatter(valueType, minFract, maxFract);
						break;
					case FORMAT_DOLLARS:
						df = NumberFormat.getCurrencyInstance();
						ef = NumberFormat.getNumberInstance();
						break;
				}
				if (minFract != null) {
					df.setMinimumFractionDigits(minFract.intValue());
					if (ef != null)
						ef.setMinimumFractionDigits(minFract.intValue());
				}
				if (maxFract != null) {
					df.setMaximumFractionDigits(maxFract.intValue());
					if (ef != null)
						ef.setMaximumFractionDigits(maxFract.intValue());
				}
				
				displayFormatter = new NumberFormatter(df);
				if (editFormatter == null)
					editFormatter = new NumberFormatter(ef);
				break;
		}
		
		factory = new DefaultFormatterFactory(displayFormatter,
											  displayFormatter,
											  editFormatter);
		return factory;
	}
	
	private class RangeVerifier extends InputVerifier {
		public boolean verify(JComponent input) {
			boolean result = true;
			if (input instanceof FormattedNumberField) {
				FormattedNumberField fnf = (FormattedNumberField) input;
				JFormattedTextField.AbstractFormatter fmt = fnf.getFormatter();
				if (fmt != null) {
					Number val = null;
					try {
						String text = fnf.getText();
						Object obj = fmt.stringToValue(text);
						if (obj instanceof Number) val = (Number) obj;
					} catch (ParseException pe) {
						result = false;
					}
					if (val != null) {
						switch (valueType) {
							case TYPE_INTEGER :
								int iv = val.intValue();
								if ((minValue != null)
									&& (iv < minValue.intValue()))
									result = false;
								if ((maxValue != null)
									&& (iv > maxValue.intValue()))
									result = false;
								break;
							case TYPE_DOUBLE:
								double dv = val.doubleValue();
								if ((minValue != null) 
									&& (dv < minValue.doubleValue()))
									result = false;
								if ((maxValue != null)
									&& (dv > maxValue.doubleValue()))
									result = false;
								break;
						}
					}
				}
			}
			if (!result) setValue(getValue());
			return result;
		}
	}
	
	private static class PercentFormatter extends NumberFormatter {

		private static final long serialVersionUID = -7978443155547960859L;

		public PercentFormatter(int valueType, Number minFract, Number maxFract) {
			super();
			NumberFormat nf = NumberFormat.getNumberInstance();
			if (valueType == TYPE_INTEGER) {
				nf.setParseIntegerOnly(true);
			} else {
				if (minFract != null)
					nf.setMinimumFractionDigits(minFract.intValue());
				if (maxFract != null)
					nf.setMaximumFractionDigits(maxFract.intValue());
			}
			setFormat(nf);
		}
		
		public String valueToString(Object o) throws ParseException {
			Number number = (Number) o;
			if (number != null) {
				double d = number.doubleValue() * 100.0;
				number = new Double(d);
			}
			return super.valueToString(number);
		}

		public Object stringToValue(String s) throws ParseException {
			Number number = (Number) super.stringToValue(s);
			if (number != null) {
				double d = number.doubleValue() / 100.0;
				number = new Double(d);
			}
			return number;
		}
	}
}
