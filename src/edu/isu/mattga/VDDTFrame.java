package edu.isu.mattga;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;

public class VDDTFrame {

	public static void init(int winWidth, int winHeight) {
		final JFrame frame = new JFrame("Voronoi Diagram / Delaunay Triangulation");
		JFrame buttons = new JFrame("Menu");
		
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent windowevent ) {
				frame.dispose();
				System.exit( 0 );
			}
		});
		
		VDDrawingSwing.init(frame);

		JButton toggleVD = new JButton("Show/Hide Voronoi");
		toggleVD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VDDrawing.showVD = (VDDrawing.showVD ? false : true);
				VDDrawingSwing.glcanvas.display();
			}
		});
		
		JButton toggleDT = new JButton("Show/Hide Delaunay");
		toggleDT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VDDrawing.showDT = (VDDrawing.showDT ? false : true);
				VDDrawingSwing.glcanvas.display();
			}
		});
		
		buttons.add(toggleVD, BorderLayout.NORTH);
		buttons.add(toggleDT, BorderLayout.SOUTH);
		
		buttons.setSize(200, 100);
		buttons.setVisible(true);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(winWidth, winHeight);
		frame.setResizable(false);
		frame.setVisible(true);
	}
}
