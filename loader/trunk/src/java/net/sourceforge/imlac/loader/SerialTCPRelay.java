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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.comm.SerialPort;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * An instance of this class provides a relay between a SerialPort and a TCP
 * connection.  It provides methods to initiate, control, and terminate the
 * TCP connection.  Whenever the TCP connection is active, bytes are relayed
 * bidirectionally between the SerialPort and the TCP connection. 
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class SerialTCPRelay {

	public static final int STATUS_INACTIVE = 0;
	public static final int STATUS_STARTING = 1;
	public static final int STATUS_ACTIVE = 2;
	public static final int STATUS_STOPPING = 3;

	public static final int TRACE_SERIAL_IN = 1;
	public static final int TRACE_SERIAL_OUT = 2;
	
	protected static final String[] statusStrings = {
		"Inactive",
		"Starting",
		"Active",
		"Stopping"	
	};
	
	protected SerialPort serial;
	protected SerialPortSettings settings;
	protected InetSocketAddress localAddr;
	protected InetSocketAddress remoteAddr;
	protected SocketChannel socketChan;
	protected CommReader commReader;
	protected CommWriter commWriter;
	protected volatile boolean quit;
	protected volatile int status = STATUS_INACTIVE;
	protected volatile boolean commReaderExit = true;
	protected volatile boolean commWriterExit = true;
	protected ByteBuffer initialData = null;
	protected volatile long throttle = 0L;
	protected volatile int traceFlags = 0;
	
	private final EventListenerList listenerList = new EventListenerList();
	
	public SerialTCPRelay() {
		super();
	}

	public SerialTCPRelay(SerialPort port) {
		super();
		serial = port;
	}
	
	public SerialTCPRelay(SerialPort port, SerialPortSettings settings) {
		super();
		serial = port;
		this.settings = settings;
		settings.apply(port);
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getStatusString() {
		return statusStrings[status];
	}
	
	public void setSocketLocalAddress(InetSocketAddress localAddr) {
		this.localAddr = localAddr;
	}
	
	public void setSocketRemoteAddress(InetSocketAddress remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
	
	public void setInitialData(ByteBuffer buf) {
		initialData = buf;
	}
	
	public void setThrottle(int delayInMilliseconds) {
		throttle = delayInMilliseconds;
	}
	
	public void setTrace(int traceFlags) {
		this.traceFlags = traceFlags;
	}
	
	public boolean start() {
		boolean result = false;
		if ((status == STATUS_INACTIVE) && (remoteAddr != null)) {
			commReaderExit = false;
			setStatus(STATUS_STARTING);
			commReader = new CommReader();
			commReader.setDaemon(true);
			commReader.start();
			result = true;
		}
		return result;
	}
	
	public boolean start(InetSocketAddress remoteAddr) {
		this.remoteAddr = remoteAddr;
		return start();
	}
	
	public void close() {
		if (status != STATUS_INACTIVE) {
			quit = true;
			setStatus(STATUS_STOPPING);
			if (commWriter != null) {
				commWriter.interrupt();
			}
			if (commReader != null) {
				commReader.interrupt();
			}
		}
	}
	
	public void addCommRelayEventListener(CommRelayEventListener listener) {
		listenerList.add(CommRelayEventListener.class, listener);
	}
	
	public void removeCommRelayEventListener(CommRelayEventListener listener) {
		listenerList.remove(CommRelayEventListener.class, listener);
	}
	
	protected synchronized void fireCommRelayEventNotice(final CommRelayEvent event) {
		int type = event.getType();
		if (type == CommRelayEvent.EVENT_SERIALINEXIT) {
			commReaderExit = true;
		} else if (type == CommRelayEvent.EVENT_SERIALOUTEXIT) {
			commWriterExit = true;
		}
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CommRelayEventListener.class) {
				final CommRelayEventListener listener =
					(CommRelayEventListener) listeners[i + 1];
				// Notify on the Swing event thread, for convenience
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						listener.commRelayEventNotice(event);
					}
				});
			}
		}
		if ((type != CommRelayEvent.EVENT_STATUSCHANGE) && commReaderExit && commWriterExit) {
			// Note that setStatus() may call back into this method, but we
			// won't call setStatus() here if it called us.
			setStatus(STATUS_INACTIVE);
		}
	}
	
	private synchronized void setStatus(int status) {
		if (status != this.status) {
			this.status = status;
			fireCommRelayEventNotice(
				new CommRelayEvent(
					this,
					CommRelayEvent.EVENT_STATUSCHANGE,
					statusStrings[status]));
		}
	}
	
	private void startCommWriter() {
		commWriter = new CommWriter();
		commWriter.setDaemon(true);
		commWriterExit = false;
		commWriter.start();
	}
	
	private final class CommReader extends Thread {

		private InputStream commIn;
		private ByteBuffer iobuf;
		private int relayCount = 0;
		private boolean started = false;
		
		public int getRelayCount() {
			return relayCount;
		}
		
		public boolean isStarted() {
			return started;
		}
		
		public void run() {
			boolean result = false;
			
			// This thread is started first.  It connects the socket,
			// and then starts the CommWriter thread.
			try {
				socketChan = SocketChannel.open();
				Socket socket = socketChan.socket();
				if (localAddr != null) {
					socket.bind(localAddr);
				}
				result = socketChan.connect(remoteAddr);
				socketChan.socket().setTcpNoDelay(true);
			} catch (IOException iox) {
				if (getStatus() == STATUS_STARTING) {
					quit = true;
					fireCommRelayEventNotice(
						new CommRelayEvent(this, CommRelayEvent.EVENT_CONNFAILURE, iox.toString()));
				}
			}
			
			if (result) {
				try {
					commIn = serial.getInputStream();
				} catch (IOException iox) {
					result = false;
					if (getStatus() == STATUS_STARTING) {
						fireCommRelayEventNotice(
							new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALINFAILURE, iox.toString()));
					}
				}
			}
			
			if (result) {
				iobuf = ByteBuffer.allocateDirect(256);
				byte[] inbuf;
				boolean needCopy = false;
				if (iobuf.hasArray()) {
					inbuf = iobuf.array();
				} else {
					inbuf = new byte[256];
					needCopy = true;
				}
				
				startCommWriter();
				started = true;
				
				if (commWriter.isStarted()) {
					setStatus(STATUS_ACTIVE);
				}
				
				if (initialData != null) {
					try {
						while (initialData.hasRemaining()) {
							socketChan.write(initialData);
						}
					} catch (IOException iox) {
						if (getStatus() == STATUS_ACTIVE) {
							fireCommRelayEventNotice(
								new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALINFAILURE, iox.toString()));
						}
						quit = true;
					}
				}
								
				while (!quit) {
					iobuf.clear();
					try {
						int len = commIn.read(inbuf);
						if (len <= 0) {
							quit = true;
							break;
						}
//						System.out.println("Relay received " + len + " bytes from serial port");
						if (needCopy) {
							iobuf.put(inbuf);
						}
						iobuf.position(0);
						iobuf.limit(len);
//						System.out.println("iobuf.position=" + iobuf.position() +
//						", iobuf.limit=" + iobuf.limit());
						if ((traceFlags & TRACE_SERIAL_IN) != 0) {
							for (int i = 0; i < len; ++i) {
								System.out.print(
									Integer.toOctalString(iobuf.get(i) & 0377)
										+ "R  ");
							}
							System.out.println("");
						}
						while (iobuf.remaining() > 0) {
							socketChan.write(iobuf);
						}
						relayCount += len;
					} catch (IOException iox) {
						if (getStatus() == STATUS_ACTIVE) {
							fireCommRelayEventNotice(
								new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALINFAILURE, iox.toString()));
						}
						quit = true;
					}
				}
			}
			if (commWriter != null)
				commWriter.interrupt();
			fireCommRelayEventNotice(new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALINEXIT));
		}
	}
	
	private final class CommWriter extends Thread {
		
		private OutputStream commOut;
		private ByteBuffer iobuf;
		private int relayCount = 0;
		private boolean started = false;
		private boolean foundType4 = false;
		private Integer throttleInt = new Integer(1);
		
		public int getRelayCount() {
			return relayCount;
		}
		
		public boolean isStarted() {
			return started;
		}
		
		public void run() {
			boolean result = false;
			
			try {
				commOut = serial.getOutputStream();
				result = true;
			} catch (IOException iox) {
				if (getStatus() == STATUS_STARTING) {
					fireCommRelayEventNotice(
						new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALOUTFAILURE, iox.toString()));
				}
				quit = true;
			}
			
			if (result) {
				iobuf = ByteBuffer.allocateDirect(256);
				byte[] outbuf;
				boolean needCopy = false;
				if (iobuf.hasArray()) {
					outbuf = iobuf.array();
				} else {
					outbuf = new byte[256];
					needCopy = true;
				}
				
				started = true;
				if (commReader.isStarted()) {
					setStatus(STATUS_ACTIVE);
				}
				
				while (!quit) {
					try {
						iobuf.clear();
						int len = socketChan.read(iobuf);
//						if (len < 2) {
//							System.out.println("CommWriter gets " + len + " from socket read");
//						}
						if (len <= 0) {
							quit = true;
							break;
						}
						iobuf.flip();
						if (needCopy) {
							iobuf.get(outbuf, 0, len);
						}
//						System.out.println("Serial output of " + len + " bytes:");
						int offset = 0;
						if (!foundType4) {
							for (int i = 0; i < len; ++i) {
								if ((outbuf[i] & 0177) == 004) {
									foundType4 = true;
									break;
								}
								++offset;
							}
						}
						if (offset < len) {
							if ((traceFlags & TRACE_SERIAL_OUT) != 0) {
								for (int i = offset; i < len; ++i) {
									System.out.print(
										Integer.toOctalString(outbuf[i] & 0377)
											+ "X  ");
								}
								System.out.println("");
							}
							if (throttle <= 0) {
								commOut.write(outbuf, offset, len - offset);
								relayCount += (len - offset);
							} else {
								for (int i = offset; i < len; ++i) {
									long coTime = System.currentTimeMillis();
									commOut.write(outbuf[i]);
									coTime = System.currentTimeMillis() - coTime;
									if (coTime < throttle) {
										synchronized (throttleInt) {
											try {
												throttleInt.wait(throttle - coTime);
											} catch (InterruptedException iex) {
												if (quit) break;
											}
										}
									}
								}
							}
						}
					} catch (IOException iox) {
						if (getStatus() == STATUS_ACTIVE) {
							fireCommRelayEventNotice(
								new CommRelayEvent(
									this,
									CommRelayEvent.EVENT_SERIALOUTFAILURE,
									iox.toString()));
						}
						quit = true;
					}
				}
			}
			commReader.interrupt();
			fireCommRelayEventNotice(new CommRelayEvent(this, CommRelayEvent.EVENT_SERIALOUTEXIT));
		}
	}
}
