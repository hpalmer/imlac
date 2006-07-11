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
package net.sourceforge.imlac.mazeserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * This class represents a player in the maze server.  Each player has a
 * socket connection to the server, an id, a name or handle, and score
 * information.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class Player {

	private static final byte BC_IGNORE = 0;
	private static final byte BC_LEAVE = 1;
	private static final byte BC_MOVE = 2;
	private static final byte BC_KILL = 3;
	private static final byte BC_NEW = 4;
	private static final byte BC_ECHO = 5;
	private static final byte BC_NEWRIGHT = 6;
	private static final byte BC_NEWLEFT = 7;
	private static final byte BC_NEWAROUND = 8;
	private static final byte BC_NEWFWD = 9;
	private static final byte BC_NEWBACK = 10;
	
	private static byte[] byteClass = {
		BC_IGNORE, BC_LEAVE, BC_MOVE, BC_KILL,									// 000-003
		BC_NEW, BC_IGNORE, BC_IGNORE, BC_IGNORE,								// 004-007
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 010-017
		BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT,						// 020-023
		BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT,						// 024-027
		BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT,							// 030-033
		BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT,							// 034-037
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 040-047
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 050-057
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 060-067
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 070-077
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 100-107
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 110-117
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 120-127
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 130-137
		BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND,					// 140-143
		BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND,					// 144-147
		BC_NEWFWD, BC_NEWFWD, BC_NEWFWD, BC_NEWFWD,								// 150-153
		BC_NEWFWD, BC_NEWFWD, BC_NEWFWD, BC_NEWFWD,								// 154-157
		BC_NEWBACK, BC_NEWBACK, BC_NEWBACK, BC_NEWBACK,							// 160-163
		BC_NEWBACK, BC_NEWBACK, BC_NEWBACK, BC_NEWBACK,							// 164-167
		BC_IGNORE, BC_IGNORE, BC_IGNORE, BC_IGNORE,								// 170-173
		BC_IGNORE, BC_IGNORE, BC_IGNORE, BC_IGNORE								// 174-177
	};
	
	protected final Server server;			// Reference to the server
	protected final SocketChannel schan;	// Connection to player
	protected final SelectionKey key;
	protected final byte[] name;					// Player's name or handle (max. 6 chars)
	protected final ByteBuffer inBuf;				// Input buffer
	protected final LinkedList<byte[]> outList;		// Output message list
	protected final ByteBuffer outBuf;				// Output buffer
	protected boolean outputBusy;
	protected boolean partialMessage;
	protected boolean readingCmdLine;		// Reading initial command line
	protected final ByteArrayOutputStream cmdLine;
	protected int nameLen;
	protected int id;						// The assigned id (1-8)
	protected int dir;						// Direction
	protected int dx;						// X position
	protected int dy;						// Y position
	protected int hits;						// Number of hits on other players
	protected int deaths;					// How many times this one died
	
	/**
	 * Create a new player instance.  The server invokes this when it has accepted
	 * a client connection.  The <code>Player</code> registers its <code>SocketChannel</code>
	 * for read/write events on the server's selector.  The server subsequently calls
	 * methods in the <code>Player</code> to service those events.
	 * 
	 * @param server		the <code>Server</code> instance that is creating this
	 * 						<code>Player</code>
	 * @param schan			the <code>SocketChannel</code> representing this player's
	 * 						connection to its client
	 * @throws IOException	if an error occurs in registering with the server's selector
	 */
	public Player(Server server, SocketChannel schan) throws IOException {
		super();
		this.server = server;
		this.schan = schan;
		
		// Initially we only want to read
		schan.configureBlocking(false);
		schan.socket().setTcpNoDelay(true);
		key = schan.register(server.getSelector(), SelectionKey.OP_READ);
		key.attach(this);
		name = new byte[6];
		outList = new LinkedList<byte[]>();
		outBuf = ByteBuffer.allocateDirect(256);
		inBuf = ByteBuffer.allocateDirect(256);
		nameLen = 0;
		id = 0;
		outputBusy = false;
		partialMessage = false;
		readingCmdLine = true;
		cmdLine = new ByteArrayOutputStream(128);
		System.out.println("Player created.");
	}

	/**
	 * Return the Imlac id associated with this player.
	 * 
	 * @return	the Imlac id (1-8)
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Return the current direction that this player is facing.
	 * 
	 * @return	the direction: 0=north, 1=east, 2=south, 3=west
	 */
	public int getDirection() {
		return dir & 3;
	}
	
	/**
	 * Return the current X coodinate of this player.
	 * 
	 * @return	the X coordinate
	 */
	public int getX() {
		return dx;
	}
	
	/**
	 * Return the current Y coordinate of this player.
	 * 
	 * @return	the Y coordinate
	 */
	public int getY() {
		return dy;
	}
	
	/**
	 * Return the number of times this player has killed another player.
	 * 
	 * @return	the hit count
	 */
	public int getHits() {
		return hits;
	}
	
	/**
	 * Return the number of times this player has died.
	 * 
	 * @return	the death count
	 */
	public int getDeaths() {
		return deaths;
	}
	
	/**
	 * Increment the number of times this player has died.
	 *
	 */
	public void killed() {
		++deaths;
	}
	
	/**
	 * Read and process a message from the client connection associated with this player.
	 * 
	 * @throws IOException	if an I/O error occurs
	 */
	public void read() throws IOException {
//		System.out.println("Player " + id + " read() entered");
		if (key.isReadable()) {
//			System.out.println("Key is readable");
			if (inBuf.hasRemaining()) {
				int len = schan.read(inBuf);
				if (len < 0) {
					byte[] msg = makeLeaveMessage(id);
					server.queueMessage(id, msg);
					server.removePlayer(this);
					return;
				}
//				System.out.println("Read from socket: position=" + inBuf.position()
//				+ ", limit=" + inBuf.limit());
			}
			if (inBuf.position() > 0) {
				inBuf.flip();
				while (!partialMessage && inBuf.hasRemaining()) {
					byte b = inBuf.get();
					b &= (byte)0177;
//					System.out.println("Read byte: " + Integer.toOctalString(b));
					if (readingCmdLine) {
						if (b == 012) {
							readingCmdLine = false;
							byte[] cmd = cmdLine.toByteArray();
							nameLen = (name.length < cmd.length) ? name.length : cmd.length;
							for (int n = 0; n < nameLen; ++n) {
								name[n] = cmd[n];
							}
							while (nameLen < name.length) {
								name[nameLen++] = 040;
							}
							System.out.println(
								"Server command line: \""
									+ cmdLine.toString()
									+ "\"");
						} else {
							cmdLine.write(b);
							continue;
						}
					}
					if (id == 0) {
						// If the user name is the same as an existing player,
						// then make that player leave, and reenter with the
						// same id here.
						Player me = server.findUser(name);
						if (me != null) {
							id = me.getId();
							hits = me.getHits();
							deaths = me.getDeaths();
							byte[] leave = makeLeaveMessage(id);
							server.queueMessage(id, leave);
							server.replacePlayer(id, this);
						} else {
							id = server.allocateId();
							if (id == 0) {
								// Could not get an id
								close();
								return;
							}
							server.addPlayer(this);
						}
						// Send the new player message to everyone
						byte[] msg = makeNewPlayerMessage();
						System.out.println("Queuing new player message of " + msg.length
						+ " bytes");
						queueMessage(msg);
						server.queueMessage(id, msg);
						// TODO:
						// Need to send type 4 messages for the other players
						// to this new player
						server.sendPlayerLocations(this);
						inBuf.clear();
						return;
					}
					
					byte bc = byteClass[b];
					byte[] msg = null;
					switch (bc) {
						case BC_IGNORE:
							break;
						case BC_LEAVE:
							msg = extractLeaveMessage();
							if (msg != null) {
								server.queueMessage(id, msg);
								server.removePlayer(this);
							}
							break;
						case BC_MOVE:
							msg = extractLocationMessage();
							if (msg != null) {
								dir = msg[2] & 3;
								dx = msg[3] & 077;
								dy = msg[4] & 077;
//								System.out.println("Player " + id + ": move id="
//								+ msg[1] + ", dir=" + dir + ", dx=" + dx + ", dy" + dy);
								server.queueMessage(id, msg);
							}
							break;
						case BC_KILL:
							msg = extractKillMessage();
							if (msg != null) {
								++hits;
								Player otherPlayer = server.findPlayer(msg[2]);
								if (otherPlayer != null) {
									otherPlayer.killed();
								}
								server.queueMessage(msg[1], msg);
							}
							break;
						case BC_NEW:
							msg = extractNewPlayerMessage();
							if (msg != null) {
								System.out.println("Server received new player message");
							}
							break;
						case BC_ECHO:
							msg = new byte[1];
							msg[0] = b;
							queueMessage(msg);
							server.queueMessage(id, msg);
							break;
						case BC_NEWRIGHT:
							dir = (dir + 1) & 3;
							break;
						case BC_NEWLEFT:
							dir = (dir - 1) & 3;
							break;
						case BC_NEWAROUND:
							dir = (dir + 2) & 3;
							break;
						case BC_NEWFWD:
//							System.out.println("Player " + id + ": newfwd");
							switch (dir & 3) {
								case 0:
									dy -= 1;
									break;
								case 1:
									dx += 1;
									break;
								case 2:
									dy += 1;
									break;
								case 3:
									dx -= 1;
									break;
							}
							break;
						case BC_NEWBACK:
							switch (dir & 3) {
								case 0:
									dy += 1;
									break;
								case 1:
									dx -= 1;
									break;
								case 2:
									dy -= 1;
									break;
								case 3:
									dx += 1;
									break;
							}
							break;
					}
					if (bc > BC_ECHO) {
						msg = new byte[1];
						msg[0] = b;
						server.queueMessage(id, msg);
					}
				}
				
				if (partialMessage) {
					System.out.println("Partial message");
					// A partial message remains at the end of the input
					// buffer.  Copy it to the beginning and prepare the
					// buffer for more input.
					int pos = inBuf.position();
					int lim = inBuf.limit();
					inBuf.clear();
					for (int i = pos; i < lim; ++i) {
						inBuf.put(inBuf.get(i));
					}
					partialMessage = false;
				} else {
					inBuf.clear();
				}
			}
		}
	}
	
	/**
	 * Queue a message for output on the client connection associated with this player.
	 * 
	 * @param message	the message to be sent
	 */
	public void queueMessage(byte[] message) {
		outList.add(message);
		key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
	
	/**
	 * Dequeue pending output data and write to the client connection associated with
	 * this player.  This method is called by the server to service the <code>OP_WRITE</code>
	 * event on this connection.
	 * 
	 * @throws IOException	if an I/O error occurs
	 */
	public void write() throws IOException {
//		System.out.println("Entered Player " + id + " write()");
		if (key.isWritable()) {
//			System.out.println("Key is writable");
			if (outputBusy) {
//				System.out.println("Output is busy.");
				// Potentially only some of the data in outBuf may have written
				if (outBuf.hasRemaining()) {
					// Keep pushing those bytes out
					schan.write(outBuf);
				} else {
					// Otherwise get ready to fill outBuf again
					outBuf.clear();
					outputBusy = false;
				}
			}
			if (!outputBusy) {
//				System.out.println("Output is not busy");
				// While there are messages on the output queue...
				while (!outList.isEmpty()) {
					byte[] msg = outList.getFirst();
//					if (msg.length == 1)
//						System.out.println(
//							"Dequeuing message of " + msg.length + " bytes");
					// If there is enough space in outBuf for the next message
					// dequeue it and append it to outBuf
					if (msg.length <= outBuf.remaining()) {
						outList.removeFirst();
						outBuf.put(msg);
					}
				}
				// If there is any data in outBuf now, start a new write
				if (outBuf.position() > 0) {
					outBuf.flip();
//					if (outBuf.limit() == 1)
//						System.out.println(
//							"Starting output: position="
//								+ outBuf.position()
//								+ ", limit="
//								+ outBuf.limit());
					schan.write(outBuf);
					outputBusy = true;
				} else {
					// Otherwise there is nothing to send right now
					key.interestOps(SelectionKey.OP_READ);
				}
			}
		}
	}
	
	/**
	 * Terminate the client connection associated with this player.
	 *
	 */
	public void close() {
		key.cancel();
		try {
			schan.socket().close();
			schan.close();
		} catch (IOException iox) {
		}
	}
	
	/**
	 * Return the user name associated with this player.
	 * 
	 * @return	the user name, in ASCII
	 */
	public byte[] getName() {
		return (nameLen == 6) ? name : null;
	}

	/**
	 * Return <code>true</code> if this player has finished initializing.  Currently
	 * that simply means the player has been assigned an Imlac id.
	 * 
	 * @return	<code>true</code> if an Imlac id has been assigned
	 */
	public boolean isInitialized() {
		return id != 0;
	}
	
	private byte[] extractLocationMessage() {
		byte[] msg = null;
		if (inBuf.remaining() >= 4) {
			msg = new byte[5];
			msg[0] = 2;
			msg[1] = inBuf.get();
			msg[2] = inBuf.get();
			msg[3] = inBuf.get();
			msg[4] = inBuf.get();
		} else {
			partialMessage = true;
		}
		return msg;
	}
	
	/**
	 * Construct a location message containing location information for this player.
	 * 
	 * @return	the location message
	 */
	public byte[] makeLocationMessage() {
		byte[] msg = new byte[5];
		msg[0] = 2;
		msg[1] = (byte) id;
		msg[2] = (byte) ((dir & 3) | 0100);
		msg[3] = (byte) (dx | 0100);
		msg[4] = (byte) (dy | 0100);
		return msg;
	}
	
	private byte[] extractNewPlayerMessage() {
		byte[] msg = null;
		if (inBuf.remaining() >= 11) {
			msg = new byte[12];
			msg[0] = 4;
			for (int i = 1; i < 12; ++i) {
				msg[i] = inBuf.get();
			}
		} else {
			partialMessage = true;
		}
		return msg;
	}
	
	/**
	 * Construct a "new player" message for this player.  This message is used to
	 * provide identification and statistics information about this player to the
	 * other players.
	 *  
	 * @return	the "new player" message
	 */
	public byte[] makeNewPlayerMessage() {
		byte[] msg = new byte[12];
		msg[0] = 4;
		msg[1] = (byte) id;
		msg[2] = name[0];
		msg[3] = name[1];
		msg[4] = name[2];
		msg[5] = name[3];
		msg[6] = name[4];
		msg[7] = name[5];
		msg[8] = (byte) (((hits >> 6) & 077) | 0100);
		msg[9] = (byte) ((hits & 077) | 0100);
		msg[10] = (byte) (((deaths >> 6) & 077) | 0100);
		msg[11] = (byte) ((deaths & 077) | 0100);
		return msg;
	}
	
	private byte[] extractKillMessage() {
		byte[] msg = null;
		if (inBuf.remaining() >= 2) {
			msg = new byte[3];
			msg[0] = 3;
			msg[1] = inBuf.get();
			msg[2] = inBuf.get();
		} else {
			partialMessage = true;
		}
		return msg;
	}
	
	private byte[] extractLeaveMessage() {
		byte[] msg = null;
		if (inBuf.remaining() > 0) {
			msg = new byte[2];
			msg[0] = 1;
			msg[1] = inBuf.get();
		} else {
			partialMessage = true;
		}
		return msg;
	}
	
	/**
	 * Construct a "leave" message for a player with a given Imlac id.  This message
	 * is used to notify the other players that the identified player is leaving the game.
	 * 
	 * @param id	the Imlac id (1-8) of the player leaving the game
	 * @return		the "leave" message
	 */
	public static byte[] makeLeaveMessage(int id) {
		byte[] msg = new byte[2];
		msg[0] = 1;
		msg[1] = (byte) id;
		return msg;
	}
}

