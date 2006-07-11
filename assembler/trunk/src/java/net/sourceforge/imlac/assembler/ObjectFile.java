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
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains code for generating Imlac object files.  It supports
 * two output formats, loader and block.  Loader format is just a zero word
 * followed by 076 instruction words, which are assumed to load at x7700.
 * Block format consists of a sequence of blocks.  Each block starts with a
 * word specifying its load address, a word specifying a negative word count,
 * and a checksum word, followed by the number of words specified by the
 * count.  The (word) sum of all the words in the block, including the first
 * three, should be zero.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see java.io.FileOutputStream
 */
public class ObjectFile extends FileOutputStream {
	public static final int FORMAT_LOADER = 1;
	public static final int FORMAT_BLOCK = 2;
	
	private int format = 0;
	private boolean blockOpen = false;
	private int loadAddress = 0;
	private int wordCount = 0;
	private int checksum = 0;
	private int blockLen = 0;
	private byte[] block = null;
	private boolean haveStartAddress = false;
	private int startAddress;
	
	/**
	 * @param filename
	 * @throws java.io.FileNotFoundException
	 */
	public ObjectFile(String filename) throws FileNotFoundException {
		super(filename);
	}

	/**
	 * @param filename
	 * @param append
	 * @throws java.io.FileNotFoundException
	 */
	public ObjectFile(String filename, boolean append) throws FileNotFoundException {
		super(filename, append);
	}

	public void close() {
		flushBlock();
		if (haveStartAddress && (format == FORMAT_BLOCK)) {
			int negStart = startAddress | 0100000;
			block[0] = (byte) (negStart >> 8);
			block[1] = (byte) negStart;
			block[2] = 0;
			block[3] = 0;
			blockLen = 4;
		} else {
			block[0] = (byte) 0377;
			block[1] = (byte) 0377;
			blockLen = 2;
		}
		try {
			write(block, 0, blockLen);
			super.close();
		} catch (IOException iox) {
			throw new RuntimeException("Output error on object file");
		}
	}
	
	public void setFormat(int format) {
		this.format = format;
	}
	
	public void setStartAddress(int location) {
		startAddress = location;
		haveStartAddress = true;
	}
	
	public int putWord(int location, int word) {
//		System.out.println("putWord(" + Integer.toOctalString(location) + ", " +
//		Integer.toOctalString(word));
		if (location != (loadAddress + wordCount)) {
			flushBlock();
		}
		if (!blockOpen) {
			startBlock(location);
		}
		if ((block.length - blockLen) < 2) {
			byte[] newblock = new byte[block.length * 2];
			for (int i = 0; i < blockLen; ++i) {
				newblock[i] = block[i];		
			}
			block = newblock;
		}
		block[blockLen++] = (byte) (word >> 8);
		block[blockLen++] = (byte) word;
		checksum += word;
		return loadAddress + ++wordCount;
	}
	
	private void startBlock(int loadAddress) {
		if (block == null) {
			block = new byte[1024];
		}
		if (format == FORMAT_LOADER) {
			if ((loadAddress & 07777) != 07700) {
				System.out.println(
					"Warning: loader starts at non-standard address "
						+ loadAddress);
			}
			block[0] = 0;
			block[1] = 0;
			blockLen = 2;
		} else {
			blockLen = 6;
		}
		this.loadAddress = loadAddress;
		wordCount = 0;
		checksum = 0;
		blockOpen = true;
	}

	private void flushBlock() {
		if (blockLen > 0) {
			if (format == FORMAT_LOADER) {
				while (wordCount < 076) {
					putWord(loadAddress + wordCount, 0);
				}
			} else {
				checksum += (loadAddress - wordCount);
				block[0] = (byte) (loadAddress >> 8);
				block[1] = (byte) loadAddress;
				block[2] = (byte) (-wordCount >> 8);
				block[3] = (byte) (-wordCount);
				block[4] = (byte) (-checksum >> 8);
				block[5] = (byte) (-checksum);
			}
			try {
				write(block, 0, blockLen);
			} catch (IOException iox) {
				throw new RuntimeException("Output error on object file");
			}
			blockLen = 0;
			wordCount = 0;
			checksum = 0;
			loadAddress = 0;
			blockOpen = false;
		}
	}
}
