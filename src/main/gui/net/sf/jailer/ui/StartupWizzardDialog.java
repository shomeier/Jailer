/*
 * Copyright 2007 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jailer.ui;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Startup Wizzard.
 * 
 * @author Ralf Wisser
 */
public class StartupWizzardDialog extends javax.swing.JDialog {

private static final long serialVersionUID = -6737420167295938488L;
	/** Creates new form SqlErrorDialog */
	public StartupWizzardDialog(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initComponents();
		try {
			iconLabel.setText(null);
			   Icon errorIcon = UIManager.getIcon("OptionPane.questionIcon");
			iconLabel.setIcon(errorIcon);
		} catch (Throwable t) {
			// ignore
		}
		
		KeyListener keyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
					newModelButtonActionPerformed(null);
				}
				if (e.getKeyChar() == '\n') {
					if (e.getComponent() == newModelButton) {
						newModelButtonActionPerformed(null);
					}
					if (e.getComponent() == newModelWRjButton) {
						newModelWRjButtonActionPerformed(null);
					}
					if (e.getComponent() == loadButton) {
						loadButtonActionPerformed(null);
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		};
		newModelButton.addKeyListener(keyListener);
		newModelWRjButton.addKeyListener(keyListener);
		loadButton.addKeyListener(keyListener);
		
		pack();
		if (parent == null) {
			setLocation(200, 100);
		} else {
			setLocation(getParent().getX() + (getParent().getWidth() - getWidth()) / 2, getParent().getY() + (getParent().getHeight() - getHeight()) / 2);
		}
		UIUtil.fit(this);
		setVisible(true);
	}

	public boolean loadModel = false;
	public boolean newModelWithRestrictions = false;
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		iconLabel = new javax.swing.JLabel();
		messagePanel = new javax.swing.JPanel();
		newModelButton = new javax.swing.JButton();
		newModelWRjButton = new javax.swing.JButton();
		loadButton = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Create or Load Model");
		getContentPane().setLayout(new java.awt.GridBagLayout());

		iconLabel.setText("jLabel1");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
		gridBagConstraints.insets = new java.awt.Insets(12, 12, 12, 12);
		getContentPane().add(iconLabel, gridBagConstraints);

		messagePanel.setLayout(new java.awt.GridBagLayout());

		newModelButton.setText("New Model");
		newModelButton.setToolTipText("New Model without Restrictions");
		newModelButton.setMargin(new java.awt.Insets(6, 14, 6, 14));
		newModelButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				newModelButtonActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		messagePanel.add(newModelButton, gridBagConstraints);

		newModelWRjButton.setText("    New Model with Restrictions    ");
		newModelWRjButton.setToolTipText("New Model with disabled non-dependencies");
		newModelWRjButton.setMargin(new java.awt.Insets(6, 14, 6, 14));
		newModelWRjButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				newModelWRjButtonActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		messagePanel.add(newModelWRjButton, gridBagConstraints);

		loadButton.setText("Load Model");
		loadButton.setMargin(new java.awt.Insets(6, 14, 6, 14));
		loadButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadButtonActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		messagePanel.add(loadButton, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(12, 0, 12, 12);
		getContentPane().add(messagePanel, gridBagConstraints);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void newModelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newModelButtonActionPerformed
		setVisible(false);
	}//GEN-LAST:event_newModelButtonActionPerformed

	private void newModelWRjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newModelWRjButtonActionPerformed
		newModelWithRestrictions = true;
		setVisible(false);
	}//GEN-LAST:event_newModelWRjButtonActionPerformed

	private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
		loadModel = true;
		setVisible(false);
	}//GEN-LAST:event_loadButtonActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel iconLabel;
	private javax.swing.JButton loadButton;
	private javax.swing.JPanel messagePanel;
	private javax.swing.JButton newModelButton;
	private javax.swing.JButton newModelWRjButton;
	// End of variables declaration//GEN-END:variables

}
