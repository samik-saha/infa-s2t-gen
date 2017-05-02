package com.samiksaha.infa.automateds2t;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class AboutDialog extends JDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AboutDialog dialog = new AboutDialog();
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the dialog.
	 */
	public AboutDialog() {
		setBounds(100, 100, 450, 300);
		
		JLabel lblI = new JLabel("InfaS2T");
		getContentPane().add(lblI, BorderLayout.NORTH);

	}

}
