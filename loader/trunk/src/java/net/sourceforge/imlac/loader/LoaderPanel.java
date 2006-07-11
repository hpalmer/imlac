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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;

import javax.comm.SerialPort;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;

import net.sourceforge.imlac.common.swing.FSButton;
import net.sourceforge.imlac.common.swing.FSLabel;
import net.sourceforge.imlac.mazeserver.ServerPanel;

/**
 * This class creates a panel that allows files to be sent to the Imlac.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see javax.swing.JPanel
 * @see java.awt.event.ActionListener
 */
public class LoaderPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -6773207870317350518L;

	private static final String[] loaderStrings = {
		"Send binary file only"
	};
	
	private final SerialPortPanel portPanel;
	private final String heading;
	
	private String currentDirectory;
	private FSLabel portLabel;
	private FSLabel directoryLabel;
	private JTextField filenameText;
	private FSButton browseButton;
	private JFileChooser fileChooser;
	private JComboBox loaderCombo;
	private FSButton comboBrowse;
	private ImlacLoader loader;
	private byte[] blockLoader;
	private JProgressBar progressBar;
	private FSButton loadButton;
	private Timer loadTimer;
	
	public LoaderPanel(SerialPortPanel portPanel, String heading) {
		super();
		this.portPanel = portPanel;
		this.heading = heading;
		buildPanel();
	}
	
	public LoaderPanel(SerialPortPanel portPanel) {
		this(portPanel, "Imlac Program Loader");
	}
	
	private void buildPanel() {
		currentDirectory = System.getProperty("user.dir");
		if (currentDirectory == null) {
			File f = new File(".");
			currentDirectory = f.getAbsolutePath();
		}
		
		float savePointSize = FSLabel.setDefaultPointSize(16.0f);
		
		setBackground(Color.white);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		addCentered(this, new FSLabel(heading, 24.0f, Font.BOLD));
		add(Box.createVerticalStrut(20));
		
		JPanel hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));

		hPane.add(new FSLabel("Current Serial Port:", 18.0f, Font.PLAIN));
		hPane.add(Box.createHorizontalStrut(6));
		portLabel = new FSLabel();
		SerialPort port = portPanel.getSelectedPort();
		if (port != null) {
			portLabel.setText(port.getName(), 18.0f, Font.PLAIN);
		}
		hPane.add(portLabel);
		hPane.add(Box.createHorizontalGlue());
		fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(6));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Current Directory:", 18.0f, Font.PLAIN));
		hPane.add(Box.createHorizontalStrut(6));
		directoryLabel = new FSLabel(currentDirectory, 18.0f, Font.PLAIN);
		hPane.add(directoryLabel);
		hPane.add(Box.createHorizontalGlue());
		fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(10));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Binary File:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));
		
		filenameText = new JTextField(40);
		Font f = filenameText.getFont();
		f = f.deriveFont(16.0f);
		filenameText.setFont(f);
		filenameText.setActionCommand("name");
		filenameText.addActionListener(this);
		fixComponentHeight(filenameText);
		
		hPane.add(filenameText);
		hPane.add(Box.createHorizontalStrut(4));
		
		fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new BinFilter());
		
		browseButton = new FSButton("Browse...");
		browseButton.setActionCommand("browse");
		browseButton.addActionListener(this);
		
		hPane.add(browseButton);
		fixComponentHeight(hPane);
		
		add(hPane);	
		add(Box.createVerticalStrut(10));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Block Loader:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));
		
		loaderCombo = new JComboBox(loaderStrings);
		f = loaderCombo.getFont();
		f = f.deriveFont(16.0f);
		loaderCombo.setFont(f);
		loaderCombo.setActionCommand("lcomb");
		loaderCombo.addActionListener(this);
		loaderCombo.setEditable(true);
		fixComponentHeight(loaderCombo);
		
		hPane.add(loaderCombo);
		hPane.add(Box.createHorizontalStrut(4));
		
		comboBrowse = new FSButton("Browse...");
		comboBrowse.setActionCommand("brld");
		comboBrowse.addActionListener(this);
		
		hPane.add(comboBrowse);
		fixComponentHeight(hPane);
		
		add(hPane);
		add(Box.createVerticalStrut(20));
		
		hPane = new JPanel();
		hPane.setBackground(Color.white);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.X_AXIS));
		
		hPane.add(new FSLabel("Progress:", 16.0f, Font.BOLD));
		hPane.add(Box.createHorizontalStrut(6));
		
		progressBar = new JProgressBar();
		hPane.add(progressBar);
		
		fixComponentHeight(hPane);
		
		add(hPane);
				
		loadButton = new FSButton("Start Load");
		loadButton.setEnabled(false);
		loadButton.setActionCommand("load");
		loadButton.addActionListener(this);
		
		add(Box.createVerticalStrut(20));
		addCentered(this, loadButton);
		
		FSLabel.setDefaultPointSize(savePointSize);
	}
	
	private void error(String message) {
		JOptionPane.showMessageDialog(
			this,
			message,
			"Operation Aborted",
			JOptionPane.ERROR_MESSAGE);
	}
	
	private void addCentered(Container c, JComponent elem) {
		elem.setAlignmentX(Component.CENTER_ALIGNMENT);
		c.add(elem);
	}

	private Dimension fixComponentSize(JComponent c) {
		Dimension dim;
		dim = c.getPreferredSize();
		c.setMaximumSize(dim);
		return dim;
	}
	
	private Dimension fixComponentHeight(JComponent c) {
		Dimension pdim = c.getPreferredSize();
		Dimension mdim = c.getMaximumSize();
		mdim.height = pdim.height;
		c.setMaximumSize(mdim);
		return mdim;
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		File f = null;
		
		if ("browse".equals(cmd)) {
			fileChooser.setCurrentDirectory(new File(currentDirectory));
			int ret = fileChooser.showOpenDialog(this);
			if (ret == JFileChooser.APPROVE_OPTION) {
				f = fileChooser.getSelectedFile();
				filenameText.setText(f.getAbsolutePath());
				currentDirectory = fileChooser.getCurrentDirectory().getAbsolutePath();
				directoryLabel.setText(currentDirectory);
			}
		} else if ("name".equals(cmd)) {
			String name = filenameText.getText();
			f = new File(name);
		} else if ("brld".equals(cmd) || "lcomb".equals(cmd)) {
			int si = loaderCombo.getSelectedIndex();
			if ("brld".equals(cmd)) {
				fileChooser.setCurrentDirectory(new File(currentDirectory));
				int ret = fileChooser.showOpenDialog(this);
				if (ret == JFileChooser.APPROVE_OPTION) {
					f = fileChooser.getSelectedFile();
					loaderCombo.getEditor().setItem(f.getAbsolutePath());
					System.out.println("[brld] Selected index is " + si);
					si = -1;
					currentDirectory =
						fileChooser.getCurrentDirectory().getAbsolutePath();
					directoryLabel.setText(currentDirectory);
				}
			} else {
				System.out.println("[lcomb] Selected index is " + si);
				if (si == 0) {
					blockLoader = null;
				} else {
					String fname = (String) loaderCombo.getSelectedItem();
					f = new File(fname);
				}
			}
			if (f != null) {
				if (!f.canRead()) {
					error("Can't read " + f.getAbsolutePath());
					f = null;
				} else {
					FileInputStream in = null;
					try {
						in = new FileInputStream(f);
						blockLoader = new byte[(int) f.length()];
						int offset = 0;
						int len;
						do {
							len =
								in.read(
									blockLoader,
									offset,
									blockLoader.length - offset);
							if (len > 0) {
								offset += len;
							}
						} while (len > 0);
						System.out.println("Read " + offset + " bytes from loader file");
						if (si < 0) {
							String fname = f.getAbsolutePath();
							boolean found = false;
							for (int i = 1; i < loaderCombo.getItemCount(); ++i) {
								if (fname.equals(loaderCombo.getItemAt(i))) {
									found = true;
									break;
								}
							}
							if (!found) {
								loaderCombo.addItem(fname);
								loaderCombo.removeActionListener(this);
								loaderCombo.setSelectedIndex(loaderCombo.getItemCount() - 1);
								loaderCombo.addActionListener(this);
							}
						}
					} catch (FileNotFoundException fnf) {
						error("File not found: " + f.getAbsolutePath());
					} catch (IOException iox) {
						error("I/O error while reading: " + f.getAbsolutePath());
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException iox2) {
							}
						}
					}
					f = null;
				}
			}
		} else if ("load".equals(cmd)) {
			if (loader != null) {
				loadButton.setEnabled(false);
				progressBar.setValue(0);
				progressBar.setIndeterminate(true);
				int len = loader.doLoad(blockLoader);
				progressBar.setMaximum(len);
				loadTimer = new Timer(250, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int pos = loader.getLoaderPosition();
						if (pos >= 0) {
							if (progressBar.isIndeterminate()) {
								progressBar.setIndeterminate(false);
							}
							progressBar.setValue(pos);
							if (pos == progressBar.getMaximum()) {
								loadTimer.stop();
								String result = loader.getLoaderResult();
								if (result != null) {
									error(result);
								}
								loadButton.setEnabled(true);
							}
						}
					}
				});
				loadTimer.start();
			}
		}
		if (f != null) {
			if (!f.canRead()) {
				error("Can't read " + f.getAbsolutePath());
				f = null;
			} else {
				loader = new ImlacLoader(f, portPanel.getSelectedPort());
				loadButton.setEnabled(true);
				progressBar.setValue(0);
//				int fmt = loader.analyzeInput();
//				System.out.println("Format 0" + Integer.toOctalString(fmt));
			}
		}
	}

	private static class BinFilter extends FileFilter {
		public boolean accept(File f) {
			String ext = getExtension(f);
			return (f.isDirectory() || "bin".equals(getExtension(f)));
		}
		
		public String getDescription() {
			return "Bin Files";
		}
		
		/*
		 * Get the extension of a file.
		 */  
		private static String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i+1).toLowerCase();
			}
			return ext;
		}
	}
	
	public static void largerFonts(float scale) {
		Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value instanceof javax.swing.plaf.FontUIResource) {
				FontUIResource fui = (FontUIResource) value;
				System.out.println((String)key + " = " + fui.toString());
				Font f = fui.deriveFont(scale);
				UIManager.put(key, new FontUIResource(f));		
			}
		}
	}
	public static void main(String[] args) {
//		largerFonts((float)18.0);
		SerialPortPanel sp = new SerialPortPanel();
		LoaderPanel lp = new LoaderPanel(sp);
		ServerPanel svr = new ServerPanel();
		CommRelayPanel rp = new CommRelayPanel(sp, svr);
		SerialPortPanel lsp = new SerialPortPanel("Loop Test Serial Port Settings");
		LoopTest lt = new LoopTest(lsp, svr, rp);
		JTabbedPane tabPane = new JTabbedPane();
		tabPane.add("Serial Port", sp);
		tabPane.add("Imlac Loader", lp);
		tabPane.add("Serial Relay To TCP/IP", rp);
		tabPane.add("Maze Server Control", svr);
		tabPane.add("Loop Test Serial Port", lsp);
		tabPane.add("Loop Test Control", lt);
		JFrame frame = new JFrame("Imlac Utilities");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tabPane);
		frame.pack();
		frame.setVisible(true);
	}
}
