/*
 * Copyright © 2005, 2006 by Howard Palmer.  All rights reserved.
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

import java.awt.Point;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * This class extends <code>JTextPane</code> to implement a text component that
 * is suitable for use by a terminal emulator program.  The terminal is assumed
 * to have a well-defined size, in terms of rows and columns.  The rows are
 * addressed as [0..NROWS-1], columns as [0..NCOLUMNS-1], with (0, 0) being the
 * upper left corner.  The size is set when a <code>TerminalPane</code> is created,
 * but may be changed later.
 * <p>
 * The terminal is assumed to use only one font, which should be a fixed width
 * font.  Each character can be displayed normally or in reverse video.  The font
 * family and size can be changed, but such changes always apply to all characters.
 * <p>
 * This class provides methods that implement functions commonly needed by a
 * terminal emulator, such as writing text to a particular row and column position
 * on the screen, and scrolling an arbitrary window up or down.
 * 
 * @author Howard Palmer
 * @version $Id: TerminalPane.java 135 2005-11-03 04:15:04Z Howard $
 * @see javax.swing.JTextPane
 *
 */
public class TerminalPane extends JTextPane {

	private static final long serialVersionUID = -2557916312977062280L;
	/**
	 * The default number of rows for the terminal.
	 */
	public static final int DEFAULT_ROWS = 40;
	/**
	 * The default number of columns for the terminal.
	 */
	public static final int DEFAULT_COLUMNS = 80;

	private static final String DEFAULT_FONTFAMILY = "Monospaced";
	private static final int DEFAULT_FONTSIZE = 16;
	
	private String blankLine;
	private int rows;
	private int columns;
	private Style plain;
	private Style reverse;

	/**
	 * Default constructor for a <code>TerminalPane</code> with <code>DEFAULT_ROWS</code>
	 * rows and <code>DEFAULT_COLUMNS</code> columns.
	 *
	 */
	public TerminalPane() {
		this(DEFAULT_ROWS, DEFAULT_COLUMNS);
	}

	/**
	 * Constructor for a <code>TerminalPane</code> with a specified number of rows
	 * and columns.
	 * 
	 * @param rows		the number of rows.
	 * @param columns	the number of columns.
	 */
	public TerminalPane(int rows, int columns) {
		super();
		setEditable(false);
		this.rows = rows;
		this.columns = columns;
		addStyles();
		clear();
		setCaretPosition(0);
	}

	
	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	/**
	 * Clear the terminal screen.
	 *
	 */
	public void clear() {
		StyledDocument doc = getStyledDocument();

		StringBuilder linebuf = new StringBuilder(columns + 1);
		for (int i = 0; i < columns; ++i) {
			linebuf.append(' ');
		}
		linebuf.append('\n');

		blankLine = linebuf.toString();

		try {
			doc.remove(0, doc.getLength());
			for (int i = 0; i < rows; ++i) {
				doc.insertString(doc.getLength(), blankLine, plain);
			}
		} catch (BadLocationException ble) {
			System.err.println(ble);
		}
	}

	/**
	 * Return the number of rows of text allowed by the current terminal size.
	 * 
	 * @return	the number of rows.
	 */
	public int getRows() {
		return rows;
	}
	
	/**
	 * Return the number of columns of text allowed by the current terminal size.
	 * 
	 * @return	the number of columns.
	 */
	public int getColumns() {
		return columns;
	}
	
	/**
	 * Return the default font family for new <code>TerminalPane</code>s.
	 * 
	 * @return	the default font family.
	 */
	public String getDefaultFontFamily() {
		return DEFAULT_FONTFAMILY;
	}
	
	/**
	 * Return the default font size for new <code>TerminalPane</code>s.
	 * 
	 * @return	the default font size.
	 */
	public int getDefaultFontSize() {
		return DEFAULT_FONTSIZE;
	}
	
	/**
	 * Return the current font family of this <code>TerminalPane</code>.
	 * 
	 * @return	the current font family.
	 */
	public String getFontFamily() {
		return StyleConstants.getFontFamily(plain);
	}

	/**
	 * Return the current font size of this <code>TerminalPane</code>.
	 * 
	 * @return	the current font size.
	 */
	public int getFontSize() {
		return StyleConstants.getFontSize(plain);
	}

	/**
	 * Highlights a character at a specified row and column by changing its style.
	 * The style is set to <code>reverse</code>, unless it is already <code>reverse</code>,
	 * in which case it is set to <code>plain</code>.
	 * 
	 * @param row		the row address
	 * @param column	the column address
	 * @throws BadLocationException	if <code>row</code> or <code>column</code>
	 * are not valid.
	 */
	public void highlightCharacter(int row, int column) throws BadLocationException {
		if ((row < 0) || (row >= rows) || (column < 0) || (column >= columns)) {
			throw new BadLocationException("highlightCharacter(" + row + ", " + column + ")", 0);
		}
		
		StyledDocument doc = getStyledDocument();
		int pos = row * (columns + 1) + column;
		Element ch = doc.getCharacterElement(pos);
		AttributeSet att = ch.getAttributes();
		if (att == reverse) {
			doc.setCharacterAttributes(pos, 1, plain, true);
		} else {
			doc.setCharacterAttributes(pos, 1, reverse, true);
		}
	}
	
	/**
	 * Erase a specified line, leaving it blank.
	 * 
	 * @param row	the row address of the line to be erased.
	 * @throws BadLocationException	if <code>row</code> is not a valid row
	 * address.
	 */
	public void eraseLine(int row) throws BadLocationException {
		if ((row < 0) || (row >= rows)) {
			throw new BadLocationException("eraseLine: invalid line number", row);
		}
		
		try {
			StyledDocument doc = getStyledDocument();
			doc.remove(row * (columns + 1), columns + 1);
			doc.insertString(row * (columns + 1), blankLine, plain);
		} catch (BadLocationException ble) {
			System.err.println("bad location: eraseLine(" + row + ")");
		}
	}
	
	/**
	 * Insert a blank line at a specified row.  The specified row through the
	 * last row are scrolled down one line, leaving a blank line at the
	 * specified row.
	 * 
	 * @param row	the row at which a blank line will be inserted.
	 * @throws BadLocationException	if <code>row</code> is not a valid row
	 * address.
	 */
	public void insertLine(int row) throws BadLocationException {
		if ((row < 0) || (row >= rows)) {
			throw new BadLocationException("insertLine: invalid line number",
					row);
		}

		try {
			StyledDocument doc = getStyledDocument();
			doc.remove((rows - 1) * (columns + 1), columns + 1);
			doc.insertString(row * (columns + 1), blankLine, plain);
		} catch (BadLocationException ble) {
			System.err.println("bad location: insertLine(" + row + ")");
		}
	}

	/**
	 * Put a character at a specified row and column on the terminal, optionally
	 * using reverse video mode.  Note that the character should not be a control
	 * character such as '\n' or '\r'.  Specifying a character that does not display
	 * as a normal printable character may result in undesirable behavior.  It is
	 * up to the terminal emulator program to interpret control characters.
	 * 
	 * @param row		the row address.
	 * @param column	the column address.
	 * @param ch		the character to be written.
	 * @param reverse	<code>true</code> for reverse video mode.
	 * @throws BadLocationException	if the row and column address is invalid for
	 * the current terminal size.
	 */
	public void putChar(int row, int column, char ch, boolean reverse)
			throws BadLocationException {
		if ((row < 0) || (row >= rows) || (column < 0) || (column >= columns)) {
			throw new BadLocationException("putChar(" + row + ", " + column + ", ..)", 0);
		}
		if ((ch < ' ') || (ch > '\176')) {
			System.err.println("putChar: " + Integer.toOctalString((int)ch));
			return;
		}
		int pos = row * (columns + 1) + column;
		StyledDocument doc = getStyledDocument();
		doc.remove(pos, 1);
		doc.insertString(pos, Character.toString(ch), (reverse) ? this.reverse : plain);
	}
	
	/**
	 * Write a specified string to a specified row and column on the terminal,
	 * optionally using reverse video mode.  The string is truncated if it runs
	 * beyond the end of the specified row.  The string should not contain any
	 * control characters such as '\n' or '\r'.  Including a character that does
	 * not display as a normal printable character may result in undesirable
	 * behavior.  It is up to the terminal emulator program to interpret control
	 * characters.
	 * 
	 * @param row		the row address.
	 * @param column	the column address.
	 * @param s			the string to write.
	 * @param reverse	<code>true</code> for reverse video mode.
	 * @throws BadLocationException	if the row and column address is invalid for
	 * the current terminal size, but not if the string runs beyond the end of a
	 * valid row.
	 */
	public void putString(int row, int column, String s, boolean reverse)
			throws BadLocationException {
		
		// Validate the address
		if ((row < 0) || (row >= rows) || (column < 0) || (column >= columns)) {
			throw new BadLocationException("putString(" + row + ", " + column
					+ ", ..)", 0);
		}
		
		StringBuilder sb = null;
		for (int i = 0; i < s.length(); ++i) {
			char ch = s.charAt(i);
			if ((ch < ' ') || (ch > '\176')) {
				if (sb == null) {
					sb = new StringBuilder(s);
				}
				sb.deleteCharAt(i);
				System.err.println("putString: " + Integer.toOctalString((int)ch));
			}
		}
		
		if (sb != null) {
			s = sb.toString();
		}
		
		// Limit the string length if necessary
		int len = s.length();
		if ((len + column) > columns) {
			len = columns - column;
			s = s.substring(0, len);
		}
		
		// Write the string
		if (len > 0) {
			int pos = row * (columns + 1) + column;
			StyledDocument doc = getStyledDocument();
			doc.remove(pos, len);
			doc.insertString(pos, s, (reverse) ? this.reverse : plain);
		}
	}
	
	/**
	 * Scroll a specified window of text down a specified number of lines,
	 * leaving blank lines at the top of the window.  The window is specified
	 * by the addresses of its top left and bottom right characters.  Text
	 * outside the specified window is unaffected.
	 * 
	 * @param top		the row address of the upper left corner of the window.
	 * @param left		the column address of the upper left corner of the window.
	 * @param bottom	the row address of the lower right corner of the window.
	 * @param right		the column address of the lower right corner of the window.
	 * @param nlines	the number of lines to scroll down.
	 * @throws BadLocationException	if the window specification is invalid for the
	 * current terminal size.
	 */
	public void scrollDown(int top, int left, int bottom, int right, int nlines)
			throws BadLocationException {
		
/*		System.out.println("scrollDown(" + top + ", " + left + ", " + bottom + ", " +
				right + ", " + nlines + ")");
*/		
		// Validate the specified window
		validateRegion(top, left, bottom, right);
		
		if (nlines > 0) {
			try {
				StyledDocument doc = getStyledDocument();
				
				// Optimize if scrolling whole rows
				if ((left == 0) && (right == (columns - 1))) {
					
					// Remove bottom lines
					doc.remove((bottom - nlines + 1) * (columns + 1),
							nlines * (columns + 1));
					
					// Insert blank lines at top
					for (int i = 0; i < nlines; ++i) {
						doc.insertString(top * (columns + 1), blankLine, plain);
					}
				} else {
					int start = bottom * (columns + 1) + left;
					int len = right - left + 1;
					for (int i = 0; i < (nlines - 1); ++i) {
						String prevLine = doc.getText(start - (columns + 1), len);
						doc.remove(start, len);
						doc.insertString(start, prevLine, plain);
						for (int j = start; j < (start + len); ++j) {
							Element ch = doc.getCharacterElement(j - (columns + 1));
							doc.setCharacterAttributes(j, 1, ch.getAttributes(),
									true);
						}
						start -= (columns + 1);
					}					
				}
			} catch (BadLocationException ble) {
				System.err.println("bad location: scrollDown(" + top + ", "
						+ left + ", " + bottom + ", " + right + ", " + nlines
						+ ")");
			}
		}
	}
	
	/**
	 * Scroll a specified window of text up a specified number of lines,
	 * leaving blank lines at the bottom of the window.  The window is specified
	 * by the addresses of its top left and bottom right characters.  Text
	 * outside the specified window is unaffected.
	 * 
	 * @param top		the row address of the upper left corner of the window.
	 * @param left		the column address of the upper left corner of the window.
	 * @param bottom	the row address of the lower right corner of the window.
	 * @param right		the column address of the lower right corner of the window.
	 * @param nlines	the number of lines to scroll up.
	 * @throws BadLocationException	if the window specification is invalid for the
	 * current terminal size.
	 */
	public void scrollUp(int top, int left, int bottom, int right, int nlines)
			throws BadLocationException {
		
/*		System.out.println("scrollUp(" + top + ", " + left + ", " + bottom + ", " +
				right + ", " + nlines + ")");
*/		
		// Validate the specified window
		validateRegion(top, left, bottom, right);
		
		if (nlines > 0) {
			try {
				StyledDocument doc = getStyledDocument();
				
				// Optimize if scrolling whole rows
				if ((left == 0) && (right == (columns - 1))) {
					
					// Remove lines at the top
					doc.remove(top * (columns + 1), nlines * (columns + 1));
					
					// Insert blank lines at the bottom
					for (int i = 0; i < nlines; ++i) {
						int pos = (bottom - nlines + i + 1) * (columns + 1);
						doc.insertString(pos, blankLine, plain);
					}
				} else {
					int start = top * (columns + 1) + left;
					int len = right - left + 1;
					for (int i = 0; i < (bottom - top + 1 - nlines); ++i) {
						String nextLine = doc.getText(start + nlines * (columns + 1), len);
						doc.remove(start, len);
						doc.insertString(start, nextLine, plain);
						for (int j = start; j < (start + len); ++j) {
							Element ch = doc.getCharacterElement(j + nlines * (columns + 1));
							doc.setCharacterAttributes(j, 1, ch.getAttributes(),
									true);
						}
						start += (columns + 1);
					}
					for (int i = 0; i < nlines; ++i) {
						doc.remove(start, len);
						doc.insertString(start, blankLine.substring(0, len), plain);
						start += (columns + 1);
					}
				}
			} catch (BadLocationException ble) {
				System.err.println("bad location: scrollUp(" + top + ", "
						+ left + ", " + bottom + ", " + right + ", " + nlines
						+ ")");
			}
		}
	}
	
	public void setFontFamily(String fontFamily) {
		StyleConstants.setFontFamily(plain, fontFamily);
		// TODO:
		// Determine whether the attributes need to be set on each character
		// again.
		// If so, need to maintain plain/reverse attribute on each character.
		// StyledDocument doc = getStyledDocument();
		// doc.setCharacterAttributes(0, doc.getLength() + 1, plain, true);
	}

	public void setFontSize(int size) {
		StyleConstants.setFontSize(plain, size);
		// StyledDocument doc = getStyledDocument();
	}

	public Point translate(Point p) {
		int pos = viewToModel(p);
		int row = pos / (columns + 1);
		int column = pos - row * (columns + 1);
		System.out.println("[" + column + ", " + row + "]");
		return new Point(column, row);
	}
	
	private void addStyles() {
		StyledDocument doc = getStyledDocument();
		Style def = StyleContext.getDefaultStyleContext().getStyle(
				StyleContext.DEFAULT_STYLE);
		plain = doc.addStyle("plain", def);
		StyleConstants.setFontFamily(plain, DEFAULT_FONTFAMILY);
		StyleConstants.setFontSize(plain, DEFAULT_FONTSIZE);
		reverse = doc.addStyle("reverse", plain);
		StyleConstants.setForeground(reverse, getBackground());
		StyleConstants.setBackground(reverse, getForeground());
	}
	
	private void validateRegion(int top, int left, int bottom, int right)
			throws BadLocationException {
		if ((top < 0) || (top >= rows) || (left < 0) || (left >= columns)
				|| (bottom < 0) || (bottom >= rows) || (right < 0)
				|| (right >= columns) || (left > right) || (top > bottom)) {
			throw new BadLocationException("validateRegion: invalid region", 0);
		}
	}
}
