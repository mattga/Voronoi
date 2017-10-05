package edu.isu.mattga;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

public class VDDrawingSwing {
	public static GLCanvas glcanvas;

	public static void init(JFrame frame) {

			GLProfile glprofile = GLProfile.getDefault();
			GLCapabilities glcapabilities = new GLCapabilities( glprofile );
			glcanvas = new GLCanvas( glcapabilities );

			glcanvas.addGLEventListener( new GLEventListener() {

				public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
					VDDrawing.setup( glautodrawable.getGL().getGL2(), width, height );
				}

				public void init( GLAutoDrawable glautodrawable ) {
				}

				public void dispose( GLAutoDrawable glautodrawable ) {
				}

				public void display( GLAutoDrawable glautodrawable ) {
					VDDrawing.render( glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight() );
				}
			});
			
			glcanvas.addMouseMotionListener(new MouseMotionListener() {
				
				public void mouseMoved(MouseEvent e) {}
				
				public void mouseDragged(MouseEvent e) {
					VDDrawing.setSweepLine(e.getY());
					glcanvas.display();
				}
			});
			
			glcanvas.addMouseListener(new MouseListener() {
				
				public void mouseReleased(MouseEvent e) {}
				
				public void mousePressed(MouseEvent e) {
					VDDrawing.setSweepLine(e.getY());
					glcanvas.display();
				}
				
				public void mouseExited(MouseEvent e) {}
				
				public void mouseEntered(MouseEvent e) {}
				
				public void mouseClicked(MouseEvent e) {}
			});

			frame.getContentPane().add( glcanvas, BorderLayout.CENTER );
	}
}
