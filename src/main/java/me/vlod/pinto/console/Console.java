package me.vlod.pinto.console;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import me.vlod.pinto.Delegate;

public class Console {
	private boolean useWhiteText;
	private JFrame frame;
	private JTextPane txtContents;
	private JScrollPane spContents;
	private JPanel pInput;
	private JTextField txtInput;
	private JButton btnSubmit;
	public Delegate onClose;
	public Delegate onSubmit;

	public Console(boolean useWhiteText) {
		this.useWhiteText = useWhiteText;
	}
	
	private void init() {
		GridBagConstraints gridBagContraints = new GridBagConstraints();
		
		this.frame = new JFrame("Pinto Server - Console");
		this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (onClose != null) {
					onClose.call();
				}
			}
		});
		this.frame.setLayout(new GridBagLayout());
		this.frame.setSize(800, 480);
		this.frame.setLocationRelativeTo(null);
		
		this.txtContents = new JTextPane();
		this.spContents = new JScrollPane(this.txtContents, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.pInput = new JPanel(new GridBagLayout());
		this.txtInput = new JTextField();
		this.btnSubmit = new JButton("Submit");

		this.txtContents.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyTyped(KeyEvent e) {
	        	e.consume();
	        }
	        
	        @Override
	        public void keyPressed(KeyEvent e) {
	        	e.consume();
	        }
		});
		
		this.txtInput.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyPressed(KeyEvent e) {
	        	if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	        		btnSubmit.doClick();
	        	}
	        }
		});
		
		this.txtContents.setComponentPopupMenu(new ConsoleContextMenu(this.txtContents));
		this.txtInput.setComponentPopupMenu(new ConsoleInputContextMenu(this.txtInput));
		
		this.btnSubmit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (onSubmit != null) {
					onSubmit.call(txtInput.getText());
				}
				txtInput.setText(null);
			}
		});
		
		gridBagContraints.fill = GridBagConstraints.BOTH;
		gridBagContraints.gridx = 0;
		gridBagContraints.gridy = 0;
		gridBagContraints.weightx = 1;
		gridBagContraints.weighty = 1;
		this.frame.add(this.spContents, gridBagContraints);
		
		gridBagContraints.fill = GridBagConstraints.BOTH;
		gridBagContraints.gridx = 0;
		gridBagContraints.gridy = 1;
		gridBagContraints.weightx = 1;
		gridBagContraints.weighty = 0;
		this.frame.add(this.pInput, gridBagContraints);

		gridBagContraints.fill = GridBagConstraints.BOTH;
		gridBagContraints.gridx = 0;
		gridBagContraints.gridy = 0;
		gridBagContraints.weightx = 1;
		gridBagContraints.weighty = 0;
		this.pInput.add(this.txtInput, gridBagContraints);

		gridBagContraints.fill = GridBagConstraints.NONE;
		gridBagContraints.gridx = 1;
		gridBagContraints.gridy = 0;
		gridBagContraints.weightx = 0;
		gridBagContraints.weighty = 0;
		this.pInput.add(this.btnSubmit, gridBagContraints);
	}
	
	public void show() {
		this.init();
		this.frame.setVisible(true);
	}
	
	public void hide() {
		this.frame.setVisible(false);
		this.frame.dispose();
	}
	
	public void write(String str, Color color, boolean newLine) {
		if (this.useWhiteText && color == Color.black) {
			color = Color.white;
		}
		
        StyleContext styleContext = StyleContext.getDefaultStyleContext();
        AttributeSet attributeSet = styleContext.addAttribute(
        		SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
        
        attributeSet = styleContext.addAttribute(attributeSet, 
        		StyleConstants.FontFamily, "Lucida Console");
        attributeSet = styleContext.addAttribute(attributeSet, 
        		StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        
        this.txtContents.setCaretPosition(txtContents.getDocument().getLength());
        this.txtContents.setCharacterAttributes(attributeSet, false);
        this.txtContents.replaceSelection(str + (newLine ? "\n" : ""));
        this.txtContents.setCharacterAttributes(styleContext.addAttribute(
        		SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.black), false);
	}
	
	public void write(String str, Color color) {
		this.write(str, color, true);
	}
	
	public void write(String str) {
		this.write(str, Color.black, true);
	}

	public void clear() {
		this.txtContents.setText(null);
	}
}
