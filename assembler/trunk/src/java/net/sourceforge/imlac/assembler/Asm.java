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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * This is the top-level class of the Imlac assembler, including the main entry point.
 * This assembler was thrown together very quickly to meet a deadline, and has only
 * been tested on a particular version of the MIT source for MazeWar.
 * 
 * There are many different styles of Imlac assembly language.  This assembler was
 * intended to take the style as a command-line argument, or alternatively to
 * automatically recognize it.  This feature is not fully implemented.
 * 
 * @author Howard Palmer
 * @version $Id$
 * 
 */
public final class Asm {
	
	public static final int STYLE_UNKNOWN = 0;
	public static final int STYLE_IMLACHS = 1;
	public static final int STYLE_OLDAMES = 2;
	public static final int STYLE_NEWAMES = 3;
	public static final int STYLE_MIDAS = 4;
	
	private static final String styles[] = {
		"unknown",
		"imlac",
		"oldames",
		"newames",
		"midas"
	};
	
	private static final String blankCode = "                     ";
	private static final String blankNumber = "           ";
	
	public static String addressToOctalString(int location) {
		String result = null;
		StringBuffer sb = new StringBuffer(Integer.toOctalString(location));
		if (sb.length() > 5) {
			result = sb.substring(sb.length() - 5);
		} else {
			while (sb.length() < 5) {
				sb.insert(0, '0');
			}
			result = sb.toString();
		}
		return result;
	}
	
	public static String wordToOctalString(int word) {
		String result = null;
		StringBuffer sb = new StringBuffer(Integer.toOctalString(word));
		if (sb.length() > 6) {
			result = sb.substring(sb.length() - 6);
		} else {
			while (sb.length() < 6) {
				sb.insert(0, '0');
			}
			result = sb.toString();
		}
		return result;
	}
	
	private SourceFile src;			// Top-level source file
	private PrintStream list;
	private int style = STYLE_MIDAS;
	private int loaderLength = 0;
	private byte[] loaderBytes = null;
	private int objFormat = ObjectFile.FORMAT_BLOCK;
	private String binName = null;
	private String lstName = null;
	
	// The generated object code is kept here in the form of
	// CodeElement entries.
	//private final ArrayList objCode = new ArrayList(8192);
	
	public Asm() {		
		super();
		Mnemonic.init();
	}
	
	public boolean isStyleUnknown() {
		return style == STYLE_UNKNOWN;
	}
	
	public boolean isStyleImlac() {
		return style == STYLE_IMLACHS;
	}
	
	public boolean isStyleOldAmes() {
		return style == STYLE_OLDAMES;
	}
	
	public boolean isStyleNewAmes() {
		return style == STYLE_NEWAMES;
	}
	
	public boolean isStyleMidas() {
		return style == STYLE_MIDAS;
	}
	
	public int getStyle() {
		return style;
	}
	
	public void setStyle(int style) {
		this.style = style;
	}
	
	public boolean setStyle(String styleName) {
		boolean ok = false;
		for (int i = 0; i < styles.length; ++i) {
			if (styles[i].compareToIgnoreCase(styleName) == 0) {
				this.style = i;
				ok = true;
				break;
			}
		}
		return ok;
	}
	
	public boolean setLoader(String filename) {
		boolean result = false;
		FileInputStream ldr = null;
//		System.out.println("setLoader(" + filename + ")");
		try {
			ldr = new FileInputStream(filename);
			loaderBytes = new byte[256];
			loaderLength = 0;
			int offset = 0;
			int len;
			do {
				len = ldr.read(loaderBytes, offset, loaderBytes.length - offset);
				if (len > 0) {
					offset += len;
				}
			} while (len > 0);
			loaderLength = offset;
			if (loaderLength > 0) {
				System.out.println(
					"Read "
						+ loaderLength
						+ " bytes from loader file "
						+ filename);
//				for (int i = 0; i < loaderLength; i += 2) {
//					int word = ((int)loaderBytes[i] << 8) | ((int)loaderBytes[i+1] & 0377);
//					System.out.println(Integer.toOctalString(word));
//				}
				result = true;
			}
		} catch (Exception ex) {
		} finally {
			if (ldr != null) {
				try {
					ldr.close();
				} catch (Exception ex) {
				}
			}
		}
		return result;
	}
	
	public ObjectFile openObjectFile() {
		ObjectFile obj = null;
		try {
			obj = new ObjectFile(binName);
			if ((loaderBytes != null) && (loaderLength > 0)) {
				try {
					obj.write(loaderBytes, 0, loaderLength);
				} catch (IOException iox) {
					System.err.println("Aborted: error writing to binary file");
					System.exit(-1);
				}
			}
			obj.setFormat(objFormat);
		} catch (FileNotFoundException fnf) {
			System.err.println("Aborted: unable to open binary file " + binName);
			System.exit(-1);
		}
		return obj;
	}
	
	public boolean assemble(String filename) {
		boolean ok = true;
		try {
			src = new SourceFile(filename);
		} catch (FileNotFoundException fnf) {
			System.err.println("Could not open the source file \"" + filename + "\"");
			ok = false;
		} catch (IOException iox) {
			System.err.println("Input error while reading file \"" + filename + "\"");
			ok = false;
		}
		
		Parser parser = new Parser(this, src);
		ok = parser.parse();
		
		if (ok) {
			System.out.println("Read " + src.size() + " lines from \"" + filename + "\"");
		}
		return ok;
	}
	
	public void error(String message) {
		list.println(message);
	}
	
	public void error(int lineNo, String message) {
		if (lineNo > 0) {
			list.println("Line " + lineNo + ": " + message);
		} else {
			list.println("             " + message);
		}
	}
	
	public String formatLineNumber(SourceLine line) {
		int lno = line.getLineNumber();
		String s = Integer.toString(lno);
		StringBuffer sb = new StringBuffer(blankNumber);
		sb.replace(sb.length() - s.length(), sb.length(), s);
		return sb.toString();		
	}
	
	private String formatCode(int location, int code) {
		StringBuffer sb = new StringBuffer(blankCode);
		sb.replace(3, 8, "00000");
		String s = Integer.toOctalString(location & 037777);
		sb.replace(8 - s.length(), 8, s);
		sb.replace(10, 16, "000000");
		s = Integer.toOctalString(code & 0177777);
		sb.replace(16 - s.length(), 16, s);
		return sb.toString();
	}
	
	public void list(SourceLine line, boolean showLineNumber) {
		String fline = StringUtil.expandTabs(line.toString(), 8);
		if (showLineNumber) {
			list.println(blankCode + formatLineNumber(line) + " " + fline);
		} else {
			list.println(blankCode + blankNumber + " " + fline);
		}
	}
	
	public void list(int location, int code, SourceLine line, boolean showLineNumber) {
		String fline = StringUtil.expandTabs(line.toString(), 8);
		if (showLineNumber) {
			list.println(formatCode(location, code) + formatLineNumber(line) + " " + fline);
		} else {
			list.println(formatCode(location, code) + blankNumber + "+" + fline);
		}
	}
	
	public void list(int location, int code) {
		list.println(formatCode(location, code));
	}
	
	private static void doHelp() {
		System.out.println("Asm [options] inputFilename");
		System.out.println("'options' can be any of:");
		System.out.println("/list=filename    - specify the name of the listing file");
		System.out.println("/bin=filename     - specify the name of the binary output file");
		System.out.println("/prefix=filename  - specify a loader file to prefix to");
		System.out.println("                    the binary output file");
		System.out.println("The list file name will default to the inputFilename,");
		System.out.println("with any file type removed, and with \".lst\" appended,");
		System.out.println("e.g. if the inputFilename is \"simple.asm\", then the");
		System.out.println("listing will be written to \"simple.lst\".  Likewise,");
		System.out.println("the default binary output file would be \"simple.bin\"");
		System.out.println("Note that both the listing and binary files are always");
		System.out.println("written, unless a fatal error occurs, and they will");
		System.out.println("replace any existing files with the same name(s).");
		System.exit(0);
	}
	public static void main(String[] args) {
		
//		for (int i = 0; i < args.length; ++i) {
//			System.out.println("Arg " + i + ": " + args[i]);
//		}
		if (args.length <= 0) {
			doHelp();
		}
		
		String srcname = args[args.length-1];
		Asm asm = new Asm();
		
		asm.list = System.out;
		asm.objFormat = ObjectFile.FORMAT_BLOCK;
		int dot = srcname.lastIndexOf('.');
		if (dot < 0) dot = srcname.length();
		String base = srcname.substring(0, dot);
		asm.binName = base + ".bin";
		asm.lstName = base + ".lst";
		
		for (int i = 0; i < (args.length - 1); ++i) {
			if (args[i].startsWith("/list=")) {
				asm.lstName = args[i].substring(6);
				if (srcname.equals(asm.lstName)) {
					System.err.println("Aborted: source and list files have the same name.");
					System.exit(-1);
				}
			} else if (args[i].startsWith("/bin=")) {
				asm.binName = args[i].substring(5);
				if (srcname.equals(asm.binName)) {
					System.err.println("Aborted: source and binary files have the same name");
				}
			} else if (args[i].startsWith("/format=")) {
				String fmt = args[i].substring(8);
				if ("loader".equals(fmt)) {
					asm.objFormat = ObjectFile.FORMAT_LOADER;
				} else if ("block".equals(fmt)) {
					asm.objFormat = ObjectFile.FORMAT_BLOCK;
				} else {
					System.out.println("Invalid object file format '" + fmt + "'");
					System.exit(-1);
				}
			} else if (args[i].startsWith("/prefix=")) {
				if (!asm.setLoader(args[i].substring(8))) {
					System.err.println("Failed to read loader file " + args[i].substring(8));
					System.exit(-1);
				}
			} else {
				System.err.println("Unsupported option: " + args[i]);
				doHelp();
			}
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(asm.lstName);
			PrintStream ps = new PrintStream(fos);
			asm.list = ps;
		} catch (FileNotFoundException fnf) {
			System.out.println("Could not open " + asm.lstName);
			System.exit(-1);
		}
		
		@SuppressWarnings("unused") boolean ok = asm.assemble(args[args.length-1]);
	}
}
