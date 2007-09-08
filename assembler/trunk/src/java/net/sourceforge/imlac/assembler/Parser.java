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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses the Imlac assembly language source from a SourceFile.
 * It constructs various internal data structures that are used for code
 * generation.
 * 
 * @author Howard Palmer
 * @version $Id$
 * 
 */
public class Parser {
	// Pattern for an indirect or display indicator before the first mnemonic,
	// e.g. "  I JMP FOO" or "  D JMS C0" or even "  *LAC BAR"
	// Lower case also allowed.  Not legal in Midas style
	private static final Pattern patLeadingIndirect =
		Pattern.compile(
			"([DdIi*])([\\p{Blank}]+)");
	
	
	private static final Pattern patMnemonic =
		Pattern.compile(
			"([\\p{Alpha}]+)(\\p{Blank}|$|\\*|\\||\\\\)");
			
	private final Asm asm;
	private final SourceFile src;
	private final ArrayList<SourceLine> extraSrc = new ArrayList<SourceLine>(256);
	private final ArrayList<CodeElement> codeList = new ArrayList<CodeElement>(1024);
//	private final ArrayList errorList = new ArrayList(4);
	private boolean endFlag = false;
	private Expression endExp = null;
	
	private int location;			// Current location counter
	private boolean relocatable = false;
	
	// Symbol table of ordinary labels
	private final HashMap<String, Label> labelTable = new HashMap<String, Label>(513);
	
	// Table of literals that have been assigned locations
	private final HashMap<String, Literal> definedLiteralTable = new HashMap<String, Literal>(513);
	
	// Table of literals that have not been assigned locations
	private final HashMap<String, Literal> undefinedLiteralTable = new HashMap<String, Literal>(513);
	
	private ObjectFile objFile = null;
	
	// Current line information
	private int lineNumber;
	private SourceLine line;
	private String indirectTag;
	private final ArrayList<Object> oplist = new ArrayList<Object>(4);
	
	// Comment state information
	private boolean slashComments = false;
	private boolean semiComments = false;
	private boolean starComments = false;
	private boolean notFirstColComment = false;
	private int fullLineCommentCount = 0;
	
	// Miscellaneous state information
	private boolean defaultOctal = true;
	private int numberValue;
	private Mnemonic lastMnemonic;
	
	/**
	 * 
	 */
	public Parser(Asm asm, SourceFile src) {
		super();
		this.asm = asm;
		this.src = src;
	}

	public Asm getAsm() {
		return asm;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public void error(String message) {
		line.queueErrorMessage("###################    " + message);
	}
	
	public void listAccumulatedErrors(SourceLine line) {
		for (int i = 0; i < line.getErrorCount(); ++i) {
			String msg = line.getErrorMessage(i);
			asm.error(msg);
		}
	}
	
	public int getStyle() {
		return asm.getStyle();
	}
	
	public Label lookupLabel(String name) {
		Label label = labelTable.get(name);
		return label;
	}
	
	public Literal addLiteral(Literal lit) {
		Literal result = definedLiteralTable.get(lit.toString());
		if (result != null) {
			// Even if the literal is already defined, it may be useless in
			// this context because of the Imlac addressing model.  It needs
			// to be in the same 2K page as the current (referencing) location.
			int litAddr = result.intValue();
			if ((litAddr ^ location) > 03777) {
				result = null;
			}
		}
		
		if (result == null) {
			// We hope that the literal will get assigned to an address
			// that can be referenced from the current location.
			result = undefinedLiteralTable.get(lit.toString());
			if (result == null) {
				undefinedLiteralTable.put(lit.toString(), lit);
				result = lit;
			}
		}
		return result;
	}
	
	public int getCurrentLocation() {
		return location;
	}
	
	public void setCurrentLocation(int newloc) {
		location = newloc;
	}
	
	public void end(Expression exp) {
		endExp = exp;
		endFlag = true;
	}
	
	public int generateWord(int value) {
		CodeElement ce = new CodeElement(lineNumber, location, value);
		codeList.add(ce);
		line.queueCodeElement(ce);
		return location++;
	}
	
	public int generateWord(Expression exp) {
		CodeElement ce = new CodeElement(lineNumber, location, exp);
		codeList.add(ce);
		line.queueCodeElement(ce);
		return location++;
	}
	
	public int generateLiterals() {
		SourceLine saveLine = line;
		int saveLineNumber = lineNumber;
		int firstExtra = extraSrc.size() + 1;
		int extraCount = 0;
		
		for (Iterator<Literal> iter = undefinedLiteralTable.values().iterator();
			iter.hasNext();
			) {
			Literal lit = iter.next();
			iter.remove();
			int saveLocation = location;
			lineNumber = extraSrc.size() + 1;
			line = new SourceLine(lit.toString(), lineNumber);
			extraSrc.add(line);
			parseStatement(line);
			if (location == saveLocation) {
				generateWord(0);
				error("Literal did not produce a value: " + lit);
			}
			lit.define(saveLocation, relocatable, false);
			definedLiteralTable.put(lit.toString(), lit);
			++extraCount;
		}
		lineNumber = saveLineNumber;
		line = saveLine;

		// Generate signal to switch to line firstExtra in extraSrc for extraCount lines
		CodeElement ce = new CodeElement(0, firstExtra, extraCount);
		codeList.add(ce);
		line.queueCodeElement(ce);
		
		return location;
	}
	
	public boolean parseComment(CharSource cs) {
		boolean ok = false;
		int i = cs.getPosition();
		char ch = cs.peekChar();

		// Consider several possible full-line comment styles
		if ((asm.isStyleImlac() || asm.isStyleUnknown()) && (ch == '/')) {
			// Native Imlac style
			slashComments = true;
			notFirstColComment |= (i != 0);
			++fullLineCommentCount;
			ok = true;
		} else if (
			(asm.isStyleMidas() || asm.isStyleUnknown()) && (ch == ';')) {
			// MIT MIDAS style
			semiComments = true;
			notFirstColComment |= (i != 0);
			++fullLineCommentCount;
			ok = true;
		} else if (
			!(asm.isStyleImlac() || asm.isStyleMidas())
				&& (ch == '*')
				&& (i == 0)) {
			// NASA Ames style
			starComments = true;
			++fullLineCommentCount;
			ok = true;
		}

		if (ok) {
			cs.advance(cs.length());
		}
		return ok;
	}
	
	public boolean parseMnemonic(CharSource cs) {
		boolean ok = false;
		
		Matcher matcher = patMnemonic.matcher(cs);
		if (matcher.lookingAt()) {
			String s = matcher.group(1);
			if (indirectTag != null) {
				if (indirectTag.compareToIgnoreCase("D") == 0) {
					s = "D" + s;
					indirectTag = null;
				}
			}
			lastMnemonic = Mnemonic.lookup(s);
			if (lastMnemonic != null) {
				cs.advance(matcher.end(1));
				ok = true;
			}
		}
		return ok;
	}
	
	public Mnemonic getLastMnemonic() {
		return lastMnemonic;
	}
	
	public boolean parseLeadingIndirect(CharSource cs) {
		boolean ok = false;
		
		indirectTag = null;
		if (!asm.isStyleMidas()) {
			Matcher mIndirect = patLeadingIndirect.matcher(cs);
			if (mIndirect.lookingAt()) {
				indirectTag = mIndirect.group(1);
				cs.advance(mIndirect.end(2));
				ok = true;
			}
		}
		return ok;
	}
	
	public boolean parseOperandIndirect(CharSource cs) {
		boolean ok = false;
		
		cs.skipBlanks();
		
		// This only applies to styles other than Imlac
		if (!asm.isStyleImlac()) {
			char ch = cs.peekChar();
			if ((asm.isStyleMidas() && (ch == '@')) ||
				(ch == '*')) {
				cs.advance(1);
				ok = true;
			}
		}
		return ok;
	}
	
	public String getLeadingIndirectTag() {
		return indirectTag;
	}
	
	public boolean parseDefinition(CharSource cs) {
		boolean ok = false;
		Label label = null;
		
		cs.push();
		
		if (Label.parse(asm.getStyle(), cs, true)) {
			String lname = Label.getLastLabel();
			label = new Label(lname);
			if (cs.peekChar() == '=') {
				cs.advance(1);
				if (cs.peekChar() == '=') {
					cs.advance(1);
				}
				
				// We could still get an error, but commit to returning true
				// because this is a definition statement.
				ok = true;
				Expression exp = Expression.parse(this, cs);
				if (exp == null) {
					error("Invalid assignment of symbol \"" + label + "\"");
				} else {
					Integer ibox = exp.evaluate(this);
					if (ibox != null) {
						// TODO:
						// Handle relocatable correctly
						label.define(ibox.intValue(), false, !exp.hasSymRef());
						labelTable.put(lname, label);
					} else {
						error("Assignment to undefined value");
					}
				}
			}
		}
		
		cs.pop(ok);
		return ok;
	}
	
	private int statementType;
	
	public boolean parseStatement(CharSource cs) {
		boolean ok = false;
		boolean notDone = false;
		
		do {
			ok = false;
			Label label = null;
			int statementType = SourceLine.TYPE_UNCLASSIFIED;
			boolean seeBlanks = asm.isStyleMidas();

			// Look for a label
			while (Label.parse(asm.getStyle(), cs, false)) {
				String lname = Label.getLastLabel();
				label = new Label(lname, location, relocatable);
				label = labelTable.put(lname, label);
				if (label != null) {
					error("Duplicate label: " + label);
				} else {
					//				error("Defined label " + lname + " at " + Integer.toOctalString(location));
				}

				// Only Midas style (colon-terminated labels) can have more
				// than one label per statement.
				if (!asm.isStyleMidas())
					break;
				cs.skipBlanks();
			}

			// For styles other than Midas, make sure there is space before the
			// mnemonic, if there is a mnemonic.
			if (!seeBlanks) {
				seeBlanks = cs.skipBlanks();
			} else {
				// For Midas, skip any blanks that might be there, but we don't
				// care whether they are or not.
				cs.skipBlanks();
			}
			if (!cs.isMore() || !seeBlanks) {
				if (!cs.isMore()) {
					// That's it, just a label
					statementType =
						(label == null)
							? SourceLine.TYPE_BLANK
							: SourceLine.TYPE_LABELONLY;
				} else {
					if (label != null) {
						error(
							"Label \"" + label + "\" not terminated by space");
					} else {
						error("Something other than a label in the first column");
					}
				}
				ok = true;
				return ok;
			}

			parseLeadingIndirect(cs);

			if (parseMnemonic(cs)) {
				ok = true;
				Mnemonic m = lastMnemonic;
				if (m.parse(this, cs)) {
				} else {
					error("Mnemonic " + m + " failed to parse its operand(s)");
				}
			} else {
				statementType = SourceLine.TYPE_DATA;
				ok = true;
				Expression exp = Expression.parse(this, cs);
				if (exp != null) {
					generateWord(exp);
				}
			}

			cs.skipBlanks();
			if (cs.isMore()) {
				if (cs.peekChar() == '?') {
					cs.advance(1);
					notDone = true;
				}
			}
		} while (notDone);
		
		return ok;
	}
	
	public int parseLine(CharSource cs) {
		int type = SourceLine.TYPE_UNCLASSIFIED;
		
		cs.push();
		if (cs.isMore()) {
			cs.skipBlanks();
		}
		if (!cs.isMore()) {
			type = SourceLine.TYPE_BLANK;
			cs.pop(true);
		} else if (cs.peekChar() == '\f') {
			cs.advance(1);
			type = SourceLine.TYPE_PAGEBREAK;
			if (cs.isMore()) {
				error("Text after form feed ignored: " + cs);
			}
			cs.pop(true);
		} else if (parseComment(cs)) {
			type = SourceLine.TYPE_COMMENT;
			cs.pop(true);
		} else if (parseDefinition(cs)) {
			type = SourceLine.TYPE_DEFINITION;
			cs.pop(true);
		} else {
			// Restore to beginning of line
			cs.pop(false);
			if (parseStatement(cs)) {
				type = statementType;
			}
		}
		return type;
	}
	
	public void generateCode(Object source, int first, int count, boolean showLineNumber) {
		SourceFile fileSource = null;
		ArrayList<SourceLine> arraySource = null;
		int last = first + count;
		if (source instanceof SourceFile) {
			fileSource = (SourceFile) source;
		} else if (source instanceof ArrayList) {
			arraySource = (ArrayList<SourceLine>)source;
		} else {
			throw new RuntimeException("invalid source type in generateCode()");
		}
		for (int lineNumber = first; lineNumber < last; ++lineNumber) {
			line =
				(fileSource == null)
					? (SourceLine) arraySource.get(lineNumber-1)
					: fileSource.get(lineNumber);
			line.reset();
			int ceCount = line.getCodeCount();
			if (ceCount == 0) {
				asm.list(line, showLineNumber);
			} else {
				for (int i = 0; i < ceCount; ++i) {
					CodeElement ce = line.getCodeElement(i);
					int nextLine = ce.getLineNumber();
					Integer ibox;
			
					// A line number of zero is an indication to switch to
					// extraSrc for a given line number and count
					if (nextLine == 0) {
						asm.list(line, showLineNumber);
						listAccumulatedErrors(line);
						int saveLineNumber = lineNumber;
						SourceLine saveLine = line;
						int extraCount = ((Integer) ce.getValue(this)).intValue();
						generateCode(extraSrc, ce.getLocation(), extraCount, false);
						lineNumber = saveLineNumber;
						line = saveLine;
						continue;
					}
			
					ibox = ce.getValue(this);
					if (ibox != null) {
						if (i == 0) {
							asm.list(ce.getLocation(), ibox.intValue(), line, showLineNumber);
						} else {
							asm.list(ce.getLocation(), ibox.intValue());
						}
						objFile.putWord(ce.getLocation(), ibox.intValue());
					} else {
						// Generate a value
						asm.list(ce.getLocation(), 0, line, showLineNumber);
						objFile.putWord(ce.getLocation(), 0);
						error("Operand failed to evaluate");
					}
				}
			}
			listAccumulatedErrors(line);
		}
	}
	
	public boolean parse() {
		boolean ok = true;
		boolean confused = false;
		
		location = 0;
		
		// This outer loop allows us to abort the inner loop and start over
		// by setting 'confused' true.
		do {
			confused = false;
			for (lineNumber = 1; lineNumber <= src.size(); ++lineNumber) {
				line = src.get(lineNumber);
				indirectTag = null;
				oplist.clear();
				
				int type = parseLine(line);
				line.setType(type);
				if (type == SourceLine.TYPE_UNCLASSIFIED) {
					line.reset();
					error("Failed to parse: " + line);								
				}
				if (endFlag) break;
			}
		} while (confused);
		
		generateLiterals();
		
		objFile = asm.openObjectFile();
		
		generateCode(src, 1, src.size(), true);
		
		if (endExp != null) {
			Integer ibox = endExp.evaluate(this);
			if (ibox != null) {
				objFile.setStartAddress(ibox.intValue());
			} else {
				error("Failed to evaluate starting address on END statement");
				listAccumulatedErrors(line);
			}
		}
		objFile.close();
		
		System.out.println("Comments: " + fullLineCommentCount);
		return ok;
	}
	
	public static void main(String[] args) {
		Asm asm = new Asm();
		Parser parser = new Parser(asm, null);
		InputStreamReader rdr = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(rdr);
		
		while (true) {
			try {
				String line = in.readLine();
				if ("".equals(line)) break;
				if (line.startsWith("style")) {
					asm.setStyle(line.substring(6));
					System.out.println("Style is " + asm.getStyle());
				}
				if (line.startsWith("label")) {
					SourceLine cs = new SourceLine(line.substring(6), 1);
					if (Label.parse(Asm.STYLE_IMLACHS, cs, false)) {
						System.out.println(
							"Valid Imlac label: \"" + Label.getLastLabel() + "\"");
					}
					cs.reset();
					if (Label.parse(Asm.STYLE_NEWAMES, cs, false)) {
						System.out.println(
							"Valid Ames label: \""
								+ Label.getLastLabel()
								+ "\"");
					}
					cs.reset();
					if (Label.parse(Asm.STYLE_MIDAS, cs, false)) {
						System.out.println(
							"Valid Midas label: \""
								+ Label.getLastLabel()
								+ "\"");
					}
				} else if (line.startsWith("number")) {
					SourceLine cs = new SourceLine(line.substring(7), 1);
					if (Expression.parseNumber(asm.getStyle(), cs)) {
						System.out.println("Parsed number successfully");
					} else {
						System.out.println("Parse failed: " + cs);
					}
				} else {
					System.out.println("Huh?");
				}
				
			} catch (Exception ex) {
				System.out.println(ex);
			}
			
		}
	}
}
