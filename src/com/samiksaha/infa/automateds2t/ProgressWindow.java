package com.samiksaha.infa.automateds2t;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JProgressBar;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import java.awt.event.ActionListener;
import javax.swing.Box;
import java.awt.Dimension;

public class ProgressWindow extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JProgressBar progressBar;
	private JLabel progressLabel;
	private boolean isCanceled = false;
	private Component rigidArea;
	private Component rigidArea_1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ProgressWindow dialog = new ProgressWindow();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public ProgressWindow() {
		setModal(true);
		setBounds(100, 100, 331, 133);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		{
			progressLabel = new JLabel("Progress");
			progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(progressLabel);
		}
		{
			rigidArea_1 = Box.createRigidArea(new Dimension(20, 20));
			rigidArea_1.setPreferredSize(new Dimension(20, 6));
			contentPanel.add(rigidArea_1);
		}
		{
			progressBar = new JProgressBar();
			contentPanel.add(progressBar);
		}
		{
			rigidArea = Box.createRigidArea(new Dimension(20, 20));
			rigidArea.setPreferredSize(new Dimension(20, 15));
			contentPanel.add(rigidArea);
		}
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel);
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						isCanceled=true;
						progressLabel.setText("Cancelling. Please wait.");
					}
				});
				panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
				panel.add(cancelButton);
			}
		}
	}
	
	public void setProgress(int n){
		progressBar.setValue(n);
	}

	public void setNote(String s){
		progressLabel.setText(s);
	}
	
	public boolean isCanceled(){
		return isCanceled;
	}
}
