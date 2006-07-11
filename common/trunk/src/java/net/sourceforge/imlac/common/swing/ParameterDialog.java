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
package net.sourceforge.imlac.common.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This class provides a way to easily build a dialog window to read parameter values.
 * Each parameter can have a prompt string and an input <code>Component</code>.
 *
 * @author Howard Palmer
 * @version $Id: ParameterDialog.java 113 2005-10-29 07:36:12Z Howard $
 */
public class ParameterDialog extends JDialog {

	private static final long serialVersionUID = 2501615720137145069L;

	private static String[] options = { "Enter", "Cancel" };

	private Component parent;
	private JOptionPane optionPane;
	private JPanel pane;
	private boolean ok;

	/**
	 * Constructs a dialog with a specified parent frame and title.
	 * 
	 * @param parent	the parent frame.
	 * @param title		the title for the dialog box.
	 * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true. 
	 */
	public ParameterDialog(Frame parent, String title)
		throws HeadlessException {
		super(parent, title, true);
		this.parent = parent;
	}

	/**
	 * Adds a parameter to the dialog.  The caller specifies an input component
	 * that is used to enter the parameter value.  The caller is responsible
	 * for retrieving the parameter value from the component after the dialog
	 * is completed.
	 * 
	 * @param name		the parameter name (currently unused).
	 * @param prompt	a prompt string for the parameter, which is inserted
	 * 					into the dialog as a <code>JLabel</code> just before
	 * 					the input component.
	 * @param input		the input component for the parameter value.
	 */
	public void addParameter(String name, String prompt, Component input) {
		if (pane == null) {
			pane = new JPanel();
			pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		}
		JPanel parmPane = new JPanel();
		parmPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		parmPane.add(new JLabel(prompt));
		parmPane.add(input);
		parmPane.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
		Dimension size = parmPane.getPreferredSize();
		parmPane.setMinimumSize(size);
		parmPane.setMaximumSize(size);
		parmPane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		pane.add(parmPane);
	}

	/**
	 * Shows the parameter dialog and waits for the user to hit "Enter" or "Cancel".
	 * The input components specified in {@link #addParameter(String, String, Component)}
	 * calls must be interrogated by the caller to retrieve the parameter values.
	 * Whether a given parameter necessarily has a value depends on the type of
	 * input component that was used for it.
	 * 
	 * @return	<code>true</code> if the user hit "Enter" to complete the dialog,
	 * 			or <code>false</code> otherwise.
	 */
	public boolean getParameters() {
		buildDialog();
		setLocationRelativeTo(parent);
		setVisible(true);
		return ok;
	}

	/**
	 * Specifies a <code>JPanel</code> to be used to display the dialog.  This
	 * should be called before the first call to {@link #addParameter(String, String, Component)}.
	 * The main purpose of this is to allow the user to specify a different layout.
	 * Normally the <code>JPanel</code>s that are constructed for each parameter are
	 * added to a default <code>JPanel</code>, which uses a <code>BoxLayout</code> on
	 * the <code>PAGE_AXIS</code>.
	 * 
	 * @param pane	the panel to be used for the dialog.
	 */
	public void setPane(JPanel pane) {
		this.pane = pane;
	}
	
	private void buildDialog() {
		Dimension size = pane.getPreferredSize();
		pane.setMinimumSize(size);
		pane.setMaximumSize(size);
		optionPane =
			new JOptionPane(
				pane,
				JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION,
				null,
				options,
				options[0]);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String prop = e.getPropertyName();

				if (isVisible()
					&& (e.getSource() == optionPane)
					&& (JOptionPane.VALUE_PROPERTY.equals(prop))) {
					Object value = optionPane.getValue();

					if (value == JOptionPane.UNINITIALIZED_VALUE) {
						setVisible(false);
						ok = false;
						return;
					}

					optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					ok = (options[0].equals(value));
				} else {
					//			System.out.println("" + e.toString());
				}
			}			
		});
		getContentPane().add(optionPane);
		pack();
	}
}
