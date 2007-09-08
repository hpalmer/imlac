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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * This class implements the Imlac Maze Server as a thread.  It accepts incoming
 * connections, creates new <code>Player</code> instances, and relays messages
 * between them.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see java.lang.Thread
 */
public class Server extends Thread {
	
	protected final InetSocketAddress serverAddress;
	protected final ServerSocketChannel listenChannel;
	protected final Selector selector;
	protected final SelectionKey acceptKey;
	
	protected final ArrayList<Player> playerList = new ArrayList<Player>(8);
	
	protected int idMask = 0;
	protected volatile boolean quit = false;
	
	/**
	 * Create the server thread, listening for Maze client connections on a
	 * specified local TCP/IP address.
	 * 
	 * @param serverAddress		the local IP address and TCP port number
	 * @throws IOException		if an error occurs in binding the local socket
	 */
	public Server(InetSocketAddress serverAddress) throws IOException {
		super();
		setDaemon(true);
		this.serverAddress = serverAddress;
		selector = SelectorProvider.provider().openSelector();
		listenChannel = ServerSocketChannel.open();
		listenChannel.configureBlocking(false);
		listenChannel.socket().setReuseAddress(true);
		listenChannel.socket().bind(serverAddress);
		acceptKey = listenChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	/**
	 * Request the server thread to exit.
	 */
	public void shutdown() {
		quit = true;
		interrupt();
	}
	
	/**
	 * Allocate one of the eight Imlac ids.
	 * 
	 * @returns the allocated Imlac id (1-8)
	 */
	public int allocateId() {
		int id;
		for (id = 0; id < 8; ++id) {
			int bit = 1 << id;
			if ((bit & idMask) == 0) {
				idMask |= bit;
				return id + 1;
			}
		}
		return 0;
	}
	
	/**
	 * Free a previously allocated Imlac id.
	 * 
	 * @param id	the Imlac id (1-8)
	 */
	public void freeId(int id) {
		if (id != 0) {
			id -= 1;
			idMask &= ~(1 << id);
		}
	}
	
	/**
	 * Return the <code>Selector</code> used by the server thread.  This is used
	 * by a <code>Player</code> instance to add its own connection to the <code>Selector</code>.
	 * 
	 * @returns the server <code>Selector</code>
	 */
	public Selector getSelector() {
		return selector;
	}
	
	/**
	 * Find the <code>Player</code> instance for a given user name.
	 * 
	 * @param the user name, in ASCII (case-sensitive)
	 * @returns the corresponding <code>Player</code> if any, or else <code>null</code>
	 */
	public Player findUser(byte[] name) {
		
		for (Player player : playerList) {
			if (Arrays.equals(name, player.getName())) {
				return player;
			}
		}
		return null;
	}
	
	/**
	 * Find the <code>Player</code> instance for a given Imlac id.
	 * 
	 * @param the Imlac id (1-8)
	 * @returns the corresponding <code>Player</code> if any, or else <code>null</code>
	 */
	public Player findPlayer(int id) {
		for (Player player : playerList) {
			if (id == player.getId()) {
				return player;
			}
		}
		return null;
	}
	
	/**
	 * Queue a message from one player, identified by Imlac id, to all of the other
	 * players.
	 * 
	 * @param fromId	the Imlac id (1-8) of the message sender
	 * @param message	the message to be sent to the other players
	 */
	public void queueMessage(int fromId, byte[] message) {
		for (Player player : playerList) {
			int id = player.getId();
			if ((id != fromId) && (id != 0)) {
				player.queueMessage(message);
			}
		}
	}
	
	/**
	 * Add a new player to the list of active players.
	 * 
	 * @param player	<code>Player</code> instance representing the new player
	 */
	public void addPlayer(Player player) {
		playerList.add(player);
	}
	
	/**
	 * Terminate a given player and remove the <code>Player</code> instance from
	 * the list of active players.
	 * 
	 * @param player	<code>Player</code> instance to be terminated
	 */
	public void removePlayer(Player player) {
		int id = player.getId();
		freeId(id);
		player.close();
		playerList.remove(player);
	}
	
	/**
	 * Replace a player, identified by Imlac id, with a new player.  The new player
	 * may have the same or a different Imlac id.  This is used when a client stops
	 * or crashes and the server does not detect it until the client tries to enter
	 * the game again.
	 * 
	 * @param id			the Imlac id (1-8) of the player to be replaced
	 * @param newPlayer		the <code>Player</code> instance for the new player
	 */
	public void replacePlayer(int id, Player newPlayer) {
		Player oldPlayer = findPlayer(id);
		oldPlayer.close();
		playerList.remove(oldPlayer);
		playerList.add(newPlayer);
	}
	
	/**
	 * Send initial messages to a newly joined player.  The new player gets a
	 * "new player" and a "location" message for each of the other players.
	 * 
	 * @param player	the <code>Player</code> instance for the new player
	 */
	public void sendPlayerLocations(Player player) {
		for (Player otherPlayer : playerList) {
			if ((otherPlayer == player) || (otherPlayer.getId() == 0)) continue;
			byte[] newplayerMsg = otherPlayer.makeNewPlayerMessage();
			player.queueMessage(newplayerMsg);
			byte[] locationMsg = otherPlayer.makeLocationMessage();
			player.queueMessage(locationMsg);
		}
	}
	
	/**
	 * This contains the main loop of the server.
	 */
	public void run() {
		int readyCount = 0;
		System.out.println("Server thread started.");
		
		do {
			try {
				readyCount = selector.select();
			} catch (IOException iox) {
				System.out.println("I/O exception from select()");
				break;
			}
			if (quit) {
				System.out.println("Server shutting down.");
				break;
			} 
			if (readyCount > 0) {
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> iter = readyKeys.iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();
					SelectableChannel c = key.channel();
					if (c.equals(listenChannel)) {
						SocketChannel playerChan = null;
						try {
							playerChan = listenChannel.accept();
							// Have we reached the max number of players?
							if (playerList.size() >= 8) {
								playerChan.close();
								continue;
							}
							Player player = new Player(this, playerChan);
						} catch (ClosedChannelException ccx) {
							if (playerChan != null) {
								closeChannel(playerChan);
							}
							System.out.println(
								"Connection closed during accept in server");
							continue;
						} catch (IOException iox) {
							System.out.println("Accept failed in server");
							continue;
						}
					} else {
						if (key.isReadable()) {
							SocketChannel schan = (SocketChannel) c;
							Player player = (Player) key.attachment();
							try {
								player.read();
							} catch (IOException iox) {
								int id = player.getId();
								System.out.println(
									"I/O exception on read from player id "
										+ id);
								if (id > 0) {
									queueMessage(id, Player.makeLeaveMessage(id));
								}
								removePlayer(player);
							}
						}
					}
				}
				
				// After all players have read everything they can,
				// give them a chance to send.
				for (Player player : playerList) {
					try {
						player.write();
					} catch (IOException iox) {
						System.out.println(
							"I/O exception on write to player id"
								+ player.getId());
						removePlayer(player);
					}
				}
			}
		} while (readyCount > 0);
		
		try {
			acceptKey.cancel();
			listenChannel.socket().close();
			listenChannel.close();
			selector.close();
		} catch (IOException iox) {
			System.out.println("I/O exception while closing server socket");
		}
		
		for (Player player : playerList) {
			player.close();
		}
		System.out.println("Server thread exit");
	}
	
	private void closeChannel(SelectableChannel chan) {
		try {
			chan.close();
		} catch (IOException iox) {
		}
	}
	
	/**
	 * This entry point was used for testing purposes and may be obsolete.
	 * 
	 * @param args	unused and ignored
	 */
	public static void main(String[] args) {
		InetSocketAddress serverAddress = new InetSocketAddress(8082);
		Server server = null;
		try {
			server = new Server(serverAddress);
		} catch (IOException iox) {
			throw new RuntimeException(iox);
		}
		server.setDaemon(true);
		server.start();
		try {
			server.join();
		} catch (InterruptedException iex) {
			throw new RuntimeException(iex);
		}
	}
}
