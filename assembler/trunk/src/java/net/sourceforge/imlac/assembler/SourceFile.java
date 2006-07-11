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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

/**
 * This class encapsulates an Imlac assembly language source file.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class SourceFile {
	private String filename;
	private ArrayList<SourceLine> lines;
	
	/**
	 * This reads a specified input file and represents it as a list of SourceLine.
	 * 
	 * @param filename   a string identifying the source file
	 * @throws FileNotFoundException If the input file cannot be opened
	 * @throws IOException           If an input error occurs while reading the file
	 */
	public SourceFile(String filename) throws FileNotFoundException, IOException {
		super();
		this.filename = filename;
		FileReader reader = new FileReader(filename);
		LineNumberReader in = new LineNumberReader(reader);
		lines = new ArrayList<SourceLine>();
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			SourceLine sl = new SourceLine(line, in.getLineNumber());
			lines.add(sl);
		}
		in.close();
	}

	/**
	 * Retrieve the SourceLine for a specified line number of the source file.
	 * 
	 * @param index  the line number to be retrieved (1..size())
	 * @returns a SourceLine representing the specified line
	 */
	public SourceLine get(int index) {
		return lines.get(index-1);
	}
	
	/**
	 * Get the source filename.
	 * 
	 * @return the filename string used to construct this SourceFile
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Return the number of lines read from the source file.
	 * 
	 * @return A count of the number of lines
	 */
	public int size() {
		return lines.size();
	}
}
