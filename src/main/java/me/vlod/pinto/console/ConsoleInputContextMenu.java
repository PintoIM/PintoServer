package me.vlod.pinto.console;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

public class ConsoleInputContextMenu extends JPopupMenu {
	private static final long serialVersionUID = 8686746525908854488L;

	public ConsoleInputContextMenu(JTextField textField) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clipboard.setContents(new StringSelection(textField.getSelectedText()), null);
			}
		});
		
		JMenuItem selectAllMenu = new JMenuItem("Select All");
		selectAllMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textField.selectAll();
			}
		});
		
		JMenuItem pasteMenu = new JMenuItem("Paste");
		pasteMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Transferable content = clipboard.getContents(null);
				
				if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					try {
						String contentStr = (String) content.getTransferData(DataFlavor.stringFlavor);
						textField.getDocument().insertString(textField.getCaretPosition(), contentStr, null);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		
		JMenuItem cutMenu = new JMenuItem("Cut");
		cutMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clipboard.setContents(new StringSelection(textField.getSelectedText()), null);
				textField.replaceSelection(null);
			}
		});
		
		this.add(copyMenu);
		this.add(selectAllMenu);
		this.add(pasteMenu);
		this.add(cutMenu);
	}
}
