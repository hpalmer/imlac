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
package net.sourceforge.imlac.loader;

import net.sourceforge.imlac.common.swing.SwingWorker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.comm.SerialPort;

/**
 * This class encapsulates an Imlac binary file and an output stream, and
 * provides functions to analyze the binary file and send it over the stream.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class ImlacLoader {

	public static final int FORMAT_READ_ERROR = -1;
	public static final int FORMAT_ERROR = 0;
	public static final int FORMAT_PREFIX_BLOCKLOADER = 1;
	public static final int FORMAT_PREFIX_HYBRIDLOADER = 2;
	public static final int FORMAT_PREFIX_FOURBITLOADER = 4;
	public static final int FORMAT_BODY_BLOCK = 010;
	public static final int FORMAT_BODY_HYBRID = 020;
	public static final int FORMAT_BODY_FOURBIT = 040;
	
	private final File binaryInput;
	private final OutputStream out;
	private final ArrayList<Block> blockList = new ArrayList<Block>(16);
	private final int[] words = new int[256];
	
	private byte[] buf;
	private int nextOffset;
	private byte[] loaderBuf;
	private LoadThread writer;
	private volatile int loaderPos;
	
	public ImlacLoader(File binaryInput, OutputStream out) {
		super();
		this.binaryInput = binaryInput;
		this.out = out;
	}
	
	public ImlacLoader(File binaryInput, SerialPort port) {
		super();
		this.binaryInput = binaryInput;
		OutputStream out = null;
		try {
			out = port.getOutputStream();
		} catch (IOException iox) {
		}
		this.out = out;
	}
	
	public synchronized int readInput() throws FileNotFoundException, IOException {
		FileInputStream in = null;
		int offset = 0;
		if (buf == null) {
			try {
				in = new FileInputStream(binaryInput);
				buf = new byte[(int) binaryInput.length()];
				int len;
				while (true) {
					len = in.read(buf, offset, buf.length - offset);
					if (len <= 0) break;
					offset += len;
				}
				System.out.println("Read " + offset + " bytes from " + binaryInput.getName());
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} else {
			offset = buf.length;
		}
		return offset;
	}
	
	public int doLoad(byte[] loaderBuf) {
		int result = (int) binaryInput.length();
		this.loaderBuf = loaderBuf;
		if (loaderBuf != null) result += loaderBuf.length;
		loaderPos = 0;
		writer = new LoadThread();
		writer.start();
		return result;
	}
	
	public int getLoaderPosition() {
		return loaderPos;
	}
	
	public String getLoaderResult() {
		return (String) writer.get();
	}
	
	public int analyzeInput() {
		int result = FORMAT_ERROR;
		int offset = -1;

		try {
			offset = readInput();
		} catch (FileNotFoundException fnf) {
		} catch (IOException iox) {
		}
		
		if ((buf == null) || (offset != buf.length)) {
			System.out.println("Error reading " + binaryInput.getName());
		} else {
			boolean fourBit = false;
			int fbCount = 0;
			for (int i = 0; i < buf.length; ++i) {
				if ((buf[i] & 0160) == 0100) {
					++fbCount;
				}
			}
			if (((double) fbCount / (double) offset) > 0.9) {
				fourBit = true;
				result |= checkForFourBitLoader();
				System.out.println("Four-bit file detected");
			} else {
				result |= checkForBlockLoader();
			}
		}
		
		int i = 0;
		for (Block block : blockList) {
			System.out.println("Block " + i++ + ": loadAddress="
			+ Integer.toOctalString(block.getLoadAddress())
			+ ", count="
			+ Integer.toOctalString(block.getCount()));
		}
		return result;
	}
	
	private int checkForFourBitLoader() {
		int result = FORMAT_ERROR;
		int blockOffset;

		if (decodeFourBitBlock(0)) {
			blockOffset = nextOffset;
			result |= FORMAT_BODY_FOURBIT;
		} else if (buf.length >= 0404) {
			result |= FORMAT_PREFIX_FOURBITLOADER;
			blockOffset = 0;
			for (int i = 0; i < 0404; ++i) {
				if (blockOffset >= buf.length) {
					result = FORMAT_ERROR;
					break;
				}
				if ((buf[blockOffset++] & 0160) != 0100) i -= 1;
			}
			nextOffset = blockOffset;
		}
		
		
		if (result != FORMAT_ERROR) {
			while (nextOffset < buf.length) {
				if ((buf[nextOffset] & 0160) != 0100) {
					++nextOffset;
					continue;
				}
				if (!decodeFourBitBlock(nextOffset)) {
					System.out.println("Error in four-bit file at offset " + nextOffset);
					result = FORMAT_ERROR;
					break;
				}
				result |= FORMAT_BODY_FOURBIT;
			}
		}
		return result;
	}
	
	private int checkForBlockLoader() {
		int result = 0;
		int blockOffset;
		
		if (decodeNormalBlock(0)) {
			result = FORMAT_BODY_BLOCK;
		} else if (decodeHybridBlock(0)) {
			result = FORMAT_BODY_HYBRID;
		} else {
			int offset = 0;
			while ((offset < buf.length) && (buf[offset] != 2)) {
				++offset;
			}
			if (decodeNormalBlock(offset + 0174)) {
				result = FORMAT_PREFIX_BLOCKLOADER | FORMAT_BODY_BLOCK;
			} else if (decodeHybridBlock(offset + 0174)) {
				result = FORMAT_PREFIX_HYBRIDLOADER | FORMAT_BODY_HYBRID;
			}
		}
		
		if (result != 0) {
			while (nextOffset < buf.length) {
				boolean ok;
				if ((result & FORMAT_BODY_BLOCK) != 0) {
					ok = decodeNormalBlock(nextOffset);
				} else {
					ok = decodeHybridBlock(nextOffset);
				}
				if (!ok) {
					result = FORMAT_ERROR;
					break;
				}
			}
		}
		
		if (result == FORMAT_ERROR) {
			System.out.println("Error in file at offset " + nextOffset);
		}
		return result;
	}
	
	private int checkForHybridLoader() {
		int result = FORMAT_ERROR;
		
		if (decodeHybridBlock(0)) {
			result |= FORMAT_BODY_HYBRID;
		} else if (buf.length > 0174) {
			nextOffset = 0174;
			if (decodeHybridBlock(nextOffset)) {
				result |= FORMAT_PREFIX_HYBRIDLOADER | FORMAT_BODY_HYBRID;
			}
		}
		
		if (result != FORMAT_ERROR) {
			while (nextOffset < buf.length) {
				if (!decodeHybridBlock(nextOffset)) {
					result = FORMAT_ERROR;
				}
			}
		}
		
		return result;
	}
	
	private boolean decodeNormalBlock(int offset) {
		boolean result = false;
		int count = 0;
		int loadAddress = getWord(buf, offset);
		
		if ((loadAddress & 0100000) != 0) {
			if (blockList.size() > 0) {
				nextOffset = offset + 2;
				if ((loadAddress & 0177777) != 0177777) {
					count = getWord(buf, offset+2);
					nextOffset = offset + 4;
				}
				Block block = new Block(null, loadAddress, count);
				blockList.add(block);
				result = true;
			}
		} else {
			count = getWord(buf, offset+2);
			if (count > 0) {
				count = (-count & 0177777);
				int[] words = new int[count];
				int cksm = getWord(buf, offset+4);
				cksm += loadAddress;
				cksm += -count;
				for (int i = 0; i < count; ++i) {
					int j = (i + 3) * 2;
					words[i] = getWord(buf, offset+j);
					cksm += words[i];
				}
				if ((cksm & 0177777) == 0) {
					nextOffset = offset + 2 * (count + 3);
					Block block = new Block(words, loadAddress, count);
					blockList.add(block);
					result = true;
				}
			}
		}
		return result;
	}
	
	private boolean decodeHybridBlock(int offset) {
		boolean result = false;
		int count = 0;
		int loadAddress = 0;
		int checksum = 0;
		
		while (offset < buf.length) {
			if (buf[offset] != 0) {
				count = buf[offset] & 0377;
				break;
			}
			++offset;
		}
		
		nextOffset = offset;
		
		if (count > 0) {
			loadAddress = getWord(buf, offset+1);
			if (loadAddress == 0177777) {
				if (blockList.size() > 0) {
					loadAddress = 0100040;
					Block block = new Block(null, loadAddress, count);
					blockList.add(block);
					nextOffset = offset + 3;
					result = true;
				}
			} else {
				offset += 3;
				for (int i = 0; i < count; ++i) {
					words[i] = getWord(buf, offset + 2*i);
					checksum += words[i];
					if ((checksum & 0200000) != 0) {
						checksum += 1;
					}
					checksum &= 0177777;
				}
				int cksm = getWord(buf, offset + 2 * count);
				if (cksm == checksum) {
					Block block = new Block(words, loadAddress, count);
					blockList.add(block);
					nextOffset = offset + 2 * (count + 1);
					result = true;
				}
			}
		}
		
		return result;
	}
	
	private boolean decodeFourBitBlock(int offset) {
		boolean result = false;
		boolean done = false;
		int state = 0;
		int count = 0;
		int loadAddress = 0;
		int checksum = 0;
		int nibble = 0;
		int word = 0;
		int wordIndex = 0;
		for ( ; !done && (offset < buf.length); ++offset) {
			if ((buf[offset] & 0160) != 0100) continue;
			switch (state) {
				case 0:
					checksum = 0;
					count = (count << 4) | (buf[offset] & 017);
					if (++nibble == 2) {
						nibble = 0;
						wordIndex = 0;
						state = 1;
					}
					break;
				case 1:
					loadAddress = (loadAddress << 4) | (buf[offset] & 017);
					if (++nibble == 4) {
						nibble = 0;
						word = 0;
						done = (count == 0);
						if (loadAddress == 0177777) {
							if (blockList.size() > 0) {
								result = true;
								done = true;
								Block block = new Block(null, loadAddress, 0);
								nextOffset = offset + 1;
							}
						}
						state = 2;
					}
					break;
				case 2:
					word = (word << 4) | (buf[offset] & 017);
					if (++nibble == 4) {
						nibble = 0;
						checksum += word;
						if ((checksum & 0200000) != 0) {
							checksum = (checksum + 1) & 0177777;
						}
						words[wordIndex++] = word;
						word = 0;
						if (wordIndex == count) {
							state = 3;
						}
					}
					break;
				case 3:
					word = (word << 4) | (buf[offset] & 017);
					if (++nibble == 4) {
						nibble = 0;
						if (word == checksum) {
							result = true;
							Block block =
								new Block(words, loadAddress, count);
							blockList.add(block);
							nextOffset = offset + 1;
						}
						done = true;
					}
					break;
			}
		}
		return result;
	}

	private int getWord(byte[] buf, int offset) {
		int word = -1;
		if ((buf.length - offset) >= 2) {
			word = (buf[offset] & 0377);
			word = (word << 8) | (buf[offset + 1] & 0377);
		}
		return word;
	}
		
	private final class LoadThread extends SwingWorker {

		private int pos;
		
		public Object construct() {
			String result = null;
			
			loaderPos = -1;
			
			int len = -1;
			try {
				len = readInput();
			} catch (FileNotFoundException fnf) {
				result = "File not found: " + binaryInput.getAbsolutePath();
			} catch (IOException iox) {
				result =
					"I/O error while reading: " + binaryInput.getAbsolutePath();
			}
			
			loaderPos = 0;
			
			if (len > 0) {
				try {
					int pos = 0;
					if (loaderBuf != null) {
						int i;
						for (i = 0; i < loaderBuf.length; i += len) {
							len = 512;
							if ((loaderBuf.length - i) < 512) len = loaderBuf.length - i;
							out.write(loaderBuf, i, len);
							pos += len;
							loaderPos = pos;
						}
					}
					if (pos > 0) {
						loaderPos = pos;
					}
					if (buf != null) {
						int i;
						for (i = 0; i < buf.length; i += len) {
							len = 512;
							if ((buf.length - i) < 512) len = buf.length - i;
							out.write(buf, i, len);
							pos += len;
							loaderPos = pos;
						}
					}
					out.flush();
				} catch (IOException iox) {
					result = "I/O error while sending data to Imlac";
				}
			}
			return result;
		}
	}
	
	public static class Block {
		final int loadAddress;
		final int count;
		final byte[] block;
		
		public Block(int[] word, int loadAddress, int count) {
			super();
			this.loadAddress = loadAddress;
			this.count = count;

			if ((loadAddress & 0100000) != 0) {
				if ((loadAddress & 0177777) == 0177777) {
					block = new byte[2];
					storeWord(block, 0, loadAddress);
				} else {
					block = new byte[4];
					storeWord(block, 0, loadAddress);
					storeWord(block, 2, count);
				}
			} else {
				block = new byte[2*(count + 3)];
				storeWord(block, 0, loadAddress);
				storeWord(block, 2, count);
				int cksm = 0;
				for (int i = 0; i < count; ++i) {
					cksm += word[i];
					storeWord(block, 2*(i + 3), word[i]);				
				}
				storeWord(block, 4, -cksm);
			}
		}
		
		public int getLoadAddress() {
			return loadAddress;		
		}
		
		public int getCount() {
			return count;
		}
		
		public byte[] getBuffer() {
			return block;
		}
		
		private int storeWord(byte[] outbuf, int offset, int word) {
			outbuf[offset] = (byte) (word >> 8);
			outbuf[offset+1] = (byte) word;
			return word;
		}
	}
}
