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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents an expression in Imlac assembly language.  Each
 * instance has an operator and up to two operands, which may be references
 * to other Expression instances or to Symbol instances.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class Expression {

	private static final int ERROROP = -1;
	
	private static final int UNOP_CONST = 0;
	private static final int UNOP_SYMBOL = 1;
	private static final int UNOP_LITERAL = 2;
	private static final int UNOP_SAVE = 3;
	private static final int UNOP_INDIRECT = 4;
	private static final int UNOP_MINUS = 5;
	private static final int UNOP_CURLOC = 6;
	
	private static final int BINOP_PLUS = 10;
	private static final int BINOP_MINUS = 11;
	private static final int BINOP_MUL = 12;
	private static final int BINOP_DIV = 13;
	private static final int BINOP_AND = 14;
	private static final int BINOP_OR = 15;
	private static final int BINOP_ADDR11 = 16;
	private static final int BINOP_ADDR12 = 17;
	private static final int BINOP_MASK = 18;
	
	// Pattern for octal number terminated by quote, 'B', or 'b'
	private static final Pattern patExplicitOctal1 =
		Pattern.compile("([0-7]+)(['Bb])");
		
	// Pattern for octal number with a leading zero
	// Also matches plain old '0'
	private static final Pattern patExplicitOctal2 =
		Pattern.compile("(0[0-7]*)");
	
	// Pattern for octal number with no explicit marker
	private static final Pattern patImplicitOctal =
		Pattern.compile("([0-7]+)");
		
	// Pattern for decimal number terminated by '.'
	private static final Pattern patExplicitDecimal =
		Pattern.compile("([0-9]+)(\\.)");
	
	// Pattern for decimal number with no explicit marker
	private static final Pattern patImplicitDecimal =
		Pattern.compile("([0-9]+)");
	
	private static int numberValue;
	
	public static boolean parseNumber(int style, CharSource cs) {
		boolean ok = false;
		int len = 0;
		
		// Look for first explicit form of octal number
		Matcher matcher = patExplicitOctal1.matcher(cs);
		if (matcher.lookingAt()) {
			numberValue = Integer.parseInt(matcher.group(1), 8);
			len = matcher.end(2);
			ok = true;
		} else {
			// Do the explicit decimal first, because the second
			// explicit octal pattern is really not that explicit!
			matcher = patExplicitDecimal.matcher(cs);
			if (matcher.lookingAt()) {
				numberValue = Integer.parseInt(matcher.group(1), 10);
				len = matcher.end(2);
				ok = true;
			} else {
				matcher = patExplicitOctal2.matcher(cs);
				if (matcher.lookingAt()) {
					numberValue = Integer.parseInt(matcher.group(1), 8);
					len = matcher.end(1);
					ok = true;
				}
			}
		}
		
		// Did any of the explicit patterns match?
		if (!ok) {
			int base;
			
			// No, try the implicit pattern for the default base
			if ((style == Asm.STYLE_MIDAS) || (style == Asm.STYLE_NEWAMES)) {
				base = 8;
				matcher = patImplicitOctal.matcher(cs);
			} else {
				base = 10;
				matcher = patImplicitDecimal.matcher(cs);
			}
			if (matcher.lookingAt()) {
				numberValue = Integer.parseInt(matcher.group(1), base);
				len = matcher.end(1);
				ok = true;
			}
		}
		
		cs.advance(len);
		return ok;
	}
	
	public static int getLastNumber() {
		return numberValue;
	}
	
	public static Expression parseTerm(Parser parser, CharSource cs) {
		Expression exp = new Expression();
		int style = parser.getStyle();
		
		if (parseNumber(style, cs)) {
			exp.op = UNOP_CONST;
			exp.left = new Integer(numberValue);
		} else if (Label.parse(style, cs, true)) {
			Label label = parser.lookupLabel(Label.getLastLabel());
			exp.op = UNOP_SYMBOL;
			exp.left = Label.getLastLabel();
			exp.symref = (label == null) || !label.isConstant();
		} else {
			char ch = cs.peekChar();
			if (ch == '.') {
				exp.op = UNOP_CURLOC;
				exp.left = new Integer(parser.getCurrentLocation());
				cs.advance(1);
			} else if ((style == Asm.STYLE_MIDAS) && (ch == '[')) {
				exp.op = UNOP_LITERAL;
				exp.symref = true;
				cs.advance(1);
				int nestCount = 0;
				int len = 0;
				int rlen = 0;
				for (int i = 0; i < cs.length(); ++i) {
					char lch = cs.charAt(i);
					if (lch == '[') ++nestCount;
					else if (lch == ']') {
						if (--nestCount < 0) {
							rlen = i + 1;
							len = i;
							if (cs.charAt(i-1) == ',') {
								len = i - 1;
							}
							break;
						}
					}
				}
				if (nestCount >= 0) {
					parser.error("Unterminated literal");
					exp.op = ERROROP;
				} else {
					String s = cs.subSequence(0, len).toString();
					cs.advance(rlen);
					exp.left = parser.addLiteral(new Literal(s));
				}
// TODO:
//		Support save symbols, e.g. #TEMP
//			} else if (ch == '#') {
//				exp.op = UNOP_SAVE;
//				exp.symref = true;
//				if (parser.parseSave()) {
//					exp.arg[0] = parser.getLastSave();
//				} else {
//					parser.error("Save symbol syntax error");
//					exp.op = ERROROP;
//					return exp;
//				}
			} else if (ch == '-') {
				exp.op = UNOP_MINUS;
				Expression minus = null;
				cs.advance(1);
				if (cs.isMore()) {
					minus = Expression.parseTerm(parser, cs);
					if (minus != null) {
						if (minus.op == ERROROP) {
							return minus;
						}
						exp.left = minus;
					}
				}
				if (minus == null) {
					parser.error("Term missing after unary minus");
					exp.op = ERROROP;
				}
			} else {
				parser.error("Operand syntax error '" + ch + "'");
				exp.op = ERROROP;
			}
		}
		return exp;
	}
	public static Expression parse(Parser parser, CharSource cs) {
		Expression exp = parseTerm(parser, cs);
		int style = parser.getStyle();
		
		
		while ((exp.op != ERROROP) && cs.isMore()) {
			char ch = cs.peekChar();
			if ((ch == ' ') || (ch == '\t') || (ch == ';')) {
				return exp;
			}

			if (ch == '+') {
				exp = new Expression(BINOP_PLUS, exp);
				Expression plus = null;
				cs.advance(1);
				if (cs.isMore()) {
					plus = Expression.parseTerm(parser, cs);
					if (plus != null) {
						exp.right = plus;
					}
				}
				if (plus == null) {
					parser.error("Second operand for '+' operator is missing");
					exp.op = ERROROP;
				}
			} else if (ch == '-') {
				exp = new Expression(BINOP_MINUS, exp);
				Expression minus = null;
				cs.advance(1);
				if (cs.isMore()) {
					minus = Expression.parseTerm(parser, cs);
					if (minus != null) {
						exp.right = minus;
					}
				}
				if (minus == null) {
					parser.error("Second operand for '-' operator is missing");
					exp.op = ERROROP;
				}
			} else if (ch == '*') {
				exp = new Expression(BINOP_MUL, exp);
				Expression mult = null;
				cs.advance(1);
				if (cs.isMore()) {
					mult = Expression.parseTerm(parser, cs);
					if (mult != null) {
						exp.right = mult;
					}
				}
				if (mult == null) {
					parser.error("Second operand for '*' operator is missing");
					exp.op = ERROROP;
				}
			} else if (ch == '/') {
				exp = new Expression(BINOP_DIV, exp);
				Expression div = null;
				cs.advance(1);
				if (cs.isMore()) {
					div = Expression.parseTerm(parser, cs);
					if (div != null) {
						exp.right = div;
					}
				}
				if (div == null) {
					parser.error("Second operand for '/' operator is missing");
					exp.op = ERROROP;
				}
			} else {
				break;
			}
		}
		return exp;
	}
	
	public static Expression makeMaskedInstruction(int instruction, int operandMask, Expression operand) {
		Expression inst = new Expression(UNOP_CONST);
		inst.left = new Integer(instruction);
		Expression mask = new Expression(UNOP_CONST);
		mask.left = new Integer(operandMask);
		Expression and = new Expression(BINOP_MASK, operand, mask);
		Expression result = new Expression(BINOP_OR, inst, and);
		return result;
	}
	
	public static Expression makeAddr11Instruction(
		int location,
		int instruction,
		Expression operand) {
		Expression inst = new Expression(UNOP_CONST);
		inst.left = new Integer(instruction);
		Expression loc = new Expression(UNOP_CONST);
		loc.left = new Integer(location);
		Expression addr = new Expression(BINOP_ADDR11, operand, loc);
		Expression result = new Expression(BINOP_OR, inst, addr);
		return result;
	}
	
	public static Expression makeAddr12Instruction(
		int location,
		int instruction,
		Expression operand) {
		Expression inst = new Expression(UNOP_CONST);
		inst.left = new Integer(instruction);
		Expression loc = new Expression(UNOP_CONST);
		loc.left = new Integer(location);
		Expression addr = new Expression(BINOP_ADDR12, operand, loc);
		Expression result = new Expression(BINOP_OR, inst, addr);
		return result;
	}
	
	private int op;
	private Object left;
	private Object right;
	private boolean symref;
	
	protected Expression() {
		super();
		op = ERROROP;
		left = null;
		right = null;
		symref = false;
	}

	protected Expression(int op) {
		this();
		this.op = op;
	}
	
	protected Expression(int op, Expression left) {
		this();
		this.op = op;
		this.left = left;
		symref = left.symref;
	}
	
	protected Expression(int op, Expression left, Expression right) {
		this();
		this.op = op;
		this.left = left;
		this.right = right;
		symref = left.symref | right.symref;
	}
	
	public boolean hasSymRef() {
		return symref;
	}
	
	public Integer evaluate(Parser parser) {
		Integer result = null;
		int value = 0;
		Integer x, y;
		
		switch (op) {
			case UNOP_CONST:
			case UNOP_CURLOC:
				value = ((Integer) left).intValue();
				result = new Integer(value);
				break;
			case UNOP_LITERAL:
				Literal lit = (Literal) left;
				if (lit.isDefined()) {
					value = lit.intValue();
					result = new Integer(value);
				} else {
					parser.error("Reference to undefined literal");
				}
				break;
			case UNOP_MINUS:
				Integer ibox = ((Expression) left).evaluate(parser);
				if (ibox != null) {
					value = -ibox.intValue();
					result = new Integer(value);
				}
				break;
			case UNOP_SYMBOL:
				String sym = (String) left;
				Label label = parser.lookupLabel(sym);
				if (label != null) {
					if (label.isDefined()) {
						value = label.intValue();
						result = new Integer(value);
					} else {
						parser.error("Reference to undefined label " + label);
					}
				} else {
					parser.error("Label " + sym + " has not been defined");
				}
				break;
			case BINOP_PLUS:
				if ((left == null) || (right == null)) {
					parser.error("Error in '+' expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() + y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_MINUS:
				if ((left == null) || (right == null)) {
					parser.error("Error in '-' expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() - y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_MUL:
				if ((left == null) || (right == null)) {
					parser.error("Error in '*' expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() * y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_DIV:
				if ((left == null) || (right == null)) {
					parser.error("Error in '/' expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() / y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_AND:
				if ((left == null) || (right == null)) {
					parser.error("Error in AND expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() & y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_OR:
				if ((left == null) || (right == null)) {
					parser.error("Error in OR expression");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						value = x.intValue() | y.intValue();
						result = new Integer(value);
					}
				}
				break;
			case BINOP_MASK:
				// This is a checked AND operation, where the left is an
				// instruction operand and the right is a mask.  It checks
				// whether the operand exceeds the mask width.
				if ((left == null) || (right == null)) {
					parser.error("Missing or invalid operand");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						int xval = x.intValue();
						int yval = y.intValue();
						if ((xval & 0177777) > yval) {
							parser.error(
								"Warning: operand size exceeds corresponding instruction field size");
						}
						value = xval & yval;
						result = new Integer(value);
					}
				}
				break;
			case BINOP_ADDR11:
				// This operation checks an operand destined for an 11-bit address
				// field to see if it is referencing an address outside the current
				// 2K page.  Left is the operand, right is location of the instruction.
				if ((left == null) || (right == null)) {
					parser.error("Missing or invalid operand");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						int xval = x.intValue();
						int yval = y.intValue();
						if (((Expression) left).hasSymRef() && ((xval ^ yval) > 03777)) {
							parser.error("Warning: attempt to reference outside 2K page");
						}
						value = xval & 03777;
						result = new Integer(value);
					}
				}
				break;
			case BINOP_ADDR12:
				// This operation checks an operand destined for an 12-bit address
				// field to see if it is referencing an address outside the current
				// 4K page.  Left is the operand, right is location of the instruction.
				if ((left == null) || (right == null)) {
					parser.error("Missing or invalid operand");
				} else {
					x = ((Expression) left).evaluate(parser);
					y = ((Expression) right).evaluate(parser);
					if ((x != null) && (y != null)) {
						int xval = x.intValue();
						int yval = y.intValue();
						if (((Expression) left).hasSymRef() && ((xval ^ yval) > 07777)) {
							parser.error("Warning: attempt to reference outside 4K page");
						}
						value = xval & 07777;
						result = new Integer(value);
					}
				}
				break;
		}
		return result;
	}
}
