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
package net.sourceforge.imlac.assembler;

import net.sourceforge.imlac.common.util.StringUtil;

import java.util.ArrayList;

/**
 * This class represents a single line of Imlac assembly language
 * source code.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see CharSource
 */
public class SourceLine extends CharSource {
	public static int TYPE_UNCLASSIFIED = -1;
	public static int TYPE_BLANK = 0;
	public static int TYPE_PAGEBREAK = 1;
	public static int TYPE_COMMENT = 2;
	public static int TYPE_LABELONLY = 3;
	public static int TYPE_DEFINITION = 4;
	public static int TYPE_DATA = 5;
	public static int TYPE_CODE = 6;
	public static int TYPE_DCODE = 7;
	public static int TYPE_INC = 8;
	public static int TYPE_PSEUDO = 9;
	
	private final String line;			// The source line as read from the input file
	private final int lineNumber;		// Line number in source file
	private ArrayList<CodeElement> codeList;
	private ArrayList<String> errorList;
	private int type;
	
	public SourceLine(String line, int lineNumber) {
		super();
		this.line = line;
		this.lineNumber = lineNumber;
		type = TYPE_UNCLASSIFIED;
		codeList = null;
		errorList = null;
	}
	public int getLineNumber() {
		return lineNumber;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public String format(int tabWidth) {
		return StringUtil.expandTabs(line, tabWidth);
	}
	
	public String format(int[] tabList) {
		return StringUtil.expandTabs(line, tabList);
	}

	public void queueCodeElement(CodeElement ce) {
		if (codeList == null) {
			codeList = new ArrayList<CodeElement>(1);
		}
		codeList.add(ce);
	}
	
	public int getCodeCount() {
		return (codeList == null) ? 0 : codeList.size();
	}
	
	public CodeElement getCodeElement(int index) {
		CodeElement ce = null;
		if (codeList != null) {
			ce = codeList.get(index);
		}
		return ce;
	}
	
	public void queueErrorMessage(String message) {
		if (errorList == null) {
			errorList = new ArrayList<String>(4);
		}
		errorList.add(message);
	}
	
	public int getErrorCount() {
		return (errorList == null) ? 0 : errorList.size();
	}
	
	public String getErrorMessage(int index) {
		String msg = null;
		if (errorList != null) {
			msg = errorList.get(index);
		}
		return msg;
	}
	
	// CharSequence interface
	public int length() {
		return line.length() - getPosition();
	}
	public char charAt(int index) {
		return line.charAt(index + getPosition());
	}
	public CharSequence subSequence(int start, int end) {
		try {
			int pos = getPosition();
			return line.subSequence(start + pos, end + pos);
		} catch (RuntimeException ex) {
			System.out.println("start=" + start + ", end=" + end + ", pos=" + getPosition());
			throw ex;
		}
	}
	public String toString() {
		return line.substring(getPosition());
	}
}
