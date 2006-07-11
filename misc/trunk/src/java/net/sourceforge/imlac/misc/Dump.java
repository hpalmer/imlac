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
package net.sourceforge.imlac.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sourceforge.imlac.assembler.Asm;

/**
 * Program to dump out an Imlac object file.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class Dump {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: dump filename");
			System.exit(0);
		}
		
		String filename = args[0];
		File f = new File(filename);
		try {
			FileInputStream in = new FileInputStream(f);
			byte[] buf = new byte[(int)f.length()];
			int offset = 0;
			int len;
			do {
				len = in.read(buf, offset, buf.length - offset);
				if (len > 0) {
					offset += len;
				}
			} while (len > 0);
			in.close();
			int loc = 0;
			for (int i = 0; i < buf.length; i += 2) {
				if ((i % 16) == 0) {
					System.out.println("");
					System.out.print(Asm.addressToOctalString(loc) + "  ");
					loc += 8;
				}
				int word = buf[i] & 0377;
				word = (word << 8) | (buf[i+1] & 0377);
				System.out.print(Asm.wordToOctalString(word) + " ");
			}
			System.out.println("");
		} catch (FileNotFoundException fnf) {
		} catch (IOException ioc) {
		}
	}
}
