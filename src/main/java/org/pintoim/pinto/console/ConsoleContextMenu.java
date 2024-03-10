package org.pintoim.pinto.console;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;

public class ConsoleContextMenu extends JPopupMenu {
	private static final long serialVersionUID = 4149806726964272221L;
	
	public ConsoleContextMenu(JTextPane textPane) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clipboard.setContents(new StringSelection(textPane.getSelectedText()), null);
			}
		});
		
		JMenuItem selectAllMenu = new JMenuItem("Select All");
		selectAllMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textPane.selectAll();
			}
		});
		
		this.add(copyMenu);
		this.add(selectAllMenu);
	}
}
