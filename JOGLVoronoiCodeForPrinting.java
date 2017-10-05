// OpenGL setup and drawing of site points, beach line, voronoi diagram, sweep line, and delaunay triangulation
public class VDDrawing {
	private static int 					WIN_WIDTH;
	private static int 					WIN_HEIGHT;
	private static final double			CURVE_INTERVAL = .01;		// Plot segments every .01 length when drawing curves
	private static final double 		WIN_MULTIPLIER = 3.;		// Multiplier for window size

	public static List<float[]>			points;						// Site points
	public static List<Point>			testPoints = new ArrayList<Point>();					
	public static DCEL					voronoiDiagram;						
	public static DCEL					delaunayTriangulation;				
	public static List<BreakPoint>		beachLine;
	public static double				sweepLinePos = Float.NaN;	// y-position of sweep line
	public static double				minX, minY, maxX, maxY;
	public static boolean				showDT = true, showVD = true;
	public static Map<Double,List<BreakPoint>>	beachLines = new TreeMap<Double,List<BreakPoint>>();
	
    protected static void setup( GL2 gl2, int width, int height ) {

    	WIN_WIDTH = width;
    	WIN_HEIGHT = height;
    	
    	gl2.glClearColor(.94f, .94f, .94f, .94f);
    	
        gl2.glMatrixMode( GL2.GL_PROJECTION );
        gl2.glLoadIdentity();

        // coordinate system oriented in window based on min and max x/y values of sites
        GLU glu = new GLU();
        glu.gluOrtho2D( minX * WIN_MULTIPLIER, maxX * WIN_MULTIPLIER, minY * WIN_MULTIPLIER, maxY * WIN_MULTIPLIER);

        gl2.glMatrixMode( GL2.GL_MODELVIEW );
        gl2.glLoadIdentity();

        gl2.glViewport( 0, 0, WIN_WIDTH, WIN_HEIGHT );
    }

    protected static void render( GL2 gl2, int width, int height ) {
    	
        gl2.glClear( GL.GL_COLOR_BUFFER_BIT );

    	gl2.glPointSize(2);
    	gl2.glBegin(GL.GL_POINTS);
    	gl2.glColor3f(0, 0, 0);
    	gl2.glVertex2f(0, 0);
    	gl2.glPointSize(4);
    	for (Point p : testPoints) {
    		gl2.glColor3f(0, 0, .8f);
    		gl2.glVertex2f((float)p.x, (float)p.y);
    	}
    	gl2.glEnd();
    	
        
    	// draw voronoi diagram
    	if (showVD && voronoiDiagram != null) {
    		gl2.glPointSize(4);
    		gl2.glBegin(GL.GL_POINTS);
    		for (Vertex v : voronoiDiagram.vertices) {
    			if (v.vorVertex)
    				gl2.glColor3f(0, .8f, 0);
    			else
    				gl2.glColor3f(0, .4f, 0);
    			gl2.glVertex2f((float)v.coord.x, (float)v.coord.y);
    		}
    		gl2.glEnd();

    		gl2.glBegin(GL.GL_LINES);
    		for (HalfEdge e : voronoiDiagram.edges) {
    			if (e.origin != null && e.twin.origin != null) {
    				gl2.glColor3f(.7f, .7f, .7f);
    				gl2.glVertex2f((float)e.origin.coord.x, (float)e.origin.coord.y);
    				gl2.glVertex2f((float)e.twin.origin.coord.x, (float)e.twin.origin.coord.y);
    			} else if (e.origin != null && e.twin.origin == null) {
    				if (e.orientation == 2) { // From origin to right bound
    					gl2.glColor3f(.7f, .7f, .7f);
    					double dy = e.slope * ((maxX * WIN_MULTIPLIER) - e.origin.coord.x);
    					if (Double.isFinite(dy)) {
    						gl2.glVertex2f((float)e.origin.coord.x, (float)e.origin.coord.y);
    						gl2.glVertex2f((float)(maxX * WIN_MULTIPLIER), (float)(e.origin.coord.y + dy));
    					}
    				} else if (e.orientation == 1) { // From origin to left bound
    					gl2.glColor3f(.7f, .7f, .7f);
    					double dy = e.slope * ((minX * WIN_MULTIPLIER) - e.origin.coord.x);
    					if (Double.isFinite(dy)) {
    						gl2.glVertex2f((float)e.origin.coord.x, (float)e.origin.coord.y);
    						gl2.glVertex2f((float)(minX * WIN_MULTIPLIER), (float)(e.origin.coord.y + dy));
    					}
    				}
    			} else if (e.origin == null && e.twin.origin != null) {
    				if (e.orientation == 2) { // From twin origin to right bound
    					gl2.glColor3f(.7f, .7f, .7f);
    					double dy = e.slope * ((maxX * WIN_MULTIPLIER) - e.twin.origin.coord.x);
    					if (Double.isFinite(dy)) {
    						gl2.glVertex2f((float)e.twin.origin.coord.x, (float)e.twin.origin.coord.y);
    						gl2.glVertex2f((float)(maxX * WIN_MULTIPLIER), (float)(e.twin.origin.coord.y + dy));
    					}
    				} else if (e.orientation == 1) { // From twin origin to left bound
    					gl2.glColor3f(.7f, .7f, .7f);
    					double dy = e.slope * ((minX * WIN_MULTIPLIER) - e.twin.origin.coord.x);
    					if (Double.isFinite(dy)) {
    						gl2.glVertex2f((float)e.twin.origin.coord.x, (float)e.twin.origin.coord.y);
    						gl2.glVertex2f((float)(minX * WIN_MULTIPLIER), (float)(e.twin.origin.coord.y + dy));
    					}
    				}
    			}
    		}
        	gl2.glEnd();
        }
    	
    	// draw delaunay triangulation
    	if (showDT && delaunayTriangulation != null) {
    		gl2.glBegin(GL.GL_LINES);
    		for (HalfEdge e : delaunayTriangulation.edges) {
    			if (e.origin != null && e.twin.origin != null) {
    				gl2.glColor3f(0, 0, .8f);
    				gl2.glVertex2f((float)e.origin.coord.x, (float)e.origin.coord.y);
    				gl2.glVertex2f((float)e.twin.origin.coord.x, (float)e.twin.origin.coord.y);
    			}
    		}
        	gl2.glEnd();
        }
        
        // draw site points
        if (points != null) {
        	gl2.glPointSize(4);
        	gl2.glBegin(GL.GL_POINTS);
        	for (float[] p : points) {
        		gl2.glColor3f(1, 0, 0);
        		gl2.glVertex2f((float)p[0], (float)p[1]);
        	}
        	gl2.glEnd();
        }
        
        // draw sweep line
        if (sweepLinePos != Float.NaN) {  
        	gl2.glBegin(GL.GL_LINES);
    		gl2.glColor3f(0, 0, .8f);
        	gl2.glVertex2f((float)(minX * WIN_MULTIPLIER), (float)sweepLinePos);
        	gl2.glVertex2f((float)(maxX * WIN_MULTIPLIER), (float)sweepLinePos);
        	gl2.glEnd();
        }
        
        // draw beach line
        gl2.glBegin(GL.GL_LINES);
    	gl2.glColor3f(.3f, .3f, .3f);
        if (beachLine != null && beachLine.size() > 1) {
        	double x, y, bp_i_x, bp_i1_x;
        	
        	// draw first arc
        	BreakPoint bp_i1, bp_i = beachLine.get(0);
        	bp_i_x = bp_i.getPositionX(sweepLinePos);
        	if (!Double.isFinite(bp_i_x)) // One of the arcs adjacent to bp_i is a vertical line
        		if (bp_i.rightArc.p.y == sweepLinePos)
        			bp_i_x = bp_i.rightArc.p.x; // It is the right arc
        		else
        			bp_i_x = bp_i.leftArc.p.x; // It is the left arc
        	
        	x = minX * WIN_MULTIPLIER;
        	y = bp_i.leftArc.y(x, sweepLinePos);
    		gl2.glVertex2f((float)x, (float)y); // draw initial line start point
        	for(; x < bp_i_x; x+= CURVE_INTERVAL) {
        		y = bp_i.leftArc.y(x, sweepLinePos);
        		gl2.glVertex2f((float)x, (float)y); // draw line end point
        		gl2.glVertex2f((float)x, (float)y); // draw next line start point
        	}
        	x = bp_i_x;
        	y = bp_i.leftArc.y(x, sweepLinePos);
        	gl2.glVertex2f((float)x, (float)y); // draw final line end point
        		

        	// draw the arcs in between
        	for (int i = 0; i < beachLine.size()-1; i++) {
        		bp_i = beachLine.get(i);
        		bp_i1 = beachLine.get(i+1);
        		bp_i_x = bp_i.getPositionX(sweepLinePos);
            	if (!Double.isFinite(bp_i_x)) // One of the arcs adjacent to bp_i is a vertical line
            		if (bp_i.rightArc.p.y == sweepLinePos)
            			bp_i_x = bp_i.rightArc.p.x; // It is the right arc
            		else
            			bp_i_x = bp_i.leftArc.p.x; // It is the left arc
        		bp_i1_x = bp_i1.getPositionX(sweepLinePos);
            	if (!Double.isFinite(bp_i1_x)) // One of the arcs adjacent to bp_i is a vertical line
            		if (bp_i1.rightArc.p.y == sweepLinePos)
            			bp_i1_x = bp_i1.rightArc.p.x; // It is the right arc
            		else
            			bp_i1_x = bp_i1.leftArc.p.x; // It is the left arc
            	
            	if (bp_i_x == bp_i1_x) { // arc is a vertical line
        			gl2.glVertex2f((float)bp_i_x, (float)sweepLinePos);
        			gl2.glVertex2f((float)bp_i_x, (float)bp_i.leftArc.y(bp_i_x, sweepLinePos));
            	} else {
            		x = bp_i_x;
            		y = bp_i.rightArc.y(x, sweepLinePos);
            		gl2.glVertex2f((float)x, (float)y); // draw initial line start point
            		for(; x < bp_i1_x; x += CURVE_INTERVAL) {
            			y = bp_i.rightArc.y(x, sweepLinePos);
            			gl2.glVertex2f((float)x, (float)y); // draw line end point
            			gl2.glVertex2f((float)x, (float)y); // draw next line start point
            		}
            		x = bp_i1_x;
            		y = bp_i.rightArc.y(x, sweepLinePos);
            		gl2.glVertex2f((float)x, (float)y); // draw initial line start point
            	}
        	}

        	// draw last arc
        	bp_i = beachLine.get(beachLine.size()-1);
        	bp_i_x = bp_i.getPositionX(sweepLinePos);
        	if (!Double.isFinite(bp_i_x)) // One of the arcs adjacent to bp_i is a vertical line
        		if (bp_i.rightArc.p.y == sweepLinePos)
        			bp_i_x = bp_i.rightArc.p.x; // It is the right arc
        		else
        			bp_i_x = bp_i.leftArc.p.x; // It is the left arc
        	
        	x = bp_i_x;
        	y = bp_i.leftArc.y(x, sweepLinePos);
    		gl2.glVertex2f((float)x, (float)y); // draw initial line start point
        	for(; x < maxX * WIN_MULTIPLIER; x += CURVE_INTERVAL) {
        		y = bp_i.rightArc.y(x, sweepLinePos);
        		gl2.glVertex2f((float)x, (float)y); // draw line end point
        		gl2.glVertex2f((float)x, (float)y); // draw next line start point
        	}
        	x = maxX * WIN_MULTIPLIER;
        	y = bp_i.leftArc.y(x, sweepLinePos);
    		gl2.glVertex2f((float)x, (float)y); // draw initial line start point
    		
        } else if (beachLine != null && beachLine.size() == 1) {
        	// draw the only arc
        	double y;
        	BreakPoint bp_i = beachLine.get(0);
        	if (bp_i.leftArc.p.y != sweepLinePos)
        		for(double x = minX * WIN_MULTIPLIER; x < maxX * WIN_MULTIPLIER; x = x + CURVE_INTERVAL) {
        			y = bp_i.leftArc.y(x, sweepLinePos);
        			gl2.glVertex2f((float)x, (float)y);
        		}
        }
        
        gl2.glEnd();
    }
    
	private static void setBeachLine(double y) {
    	Iterator<Entry<Double, List<BreakPoint>>> it = beachLines.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<Double,List<BreakPoint>> bl = it.next();
    		beachLine = bl.getValue();
    		if (y < bl.getKey())
    			return;
    	}
    }
    
    protected static void addPoints(List<Point> ps) {
    	points = new ArrayList<float[]>();
    	for (Point p : ps) {
    		points.add(new float[]{(float)p.x, (float)p.y});

    		if (p.x > maxX)
    			maxX = p.x;
    		if (p.y > maxY)
    			maxY = p.y;
    		if (p.x < minX)
    			minX = p.x;
    		if (p.y < minY)
    			minY = p.y;
    	}
    }
    
    public static void setSweepLine(int y) {
    	double a = 1 - ((double)y / WIN_HEIGHT); 
    	a *= Math.abs(maxY*WIN_MULTIPLIER) + Math.abs(minY*WIN_MULTIPLIER);
    	a -= Math.abs(minY*WIN_MULTIPLIER);
    	
    	sweepLinePos = a;
    	//System.out.println(sweepLinePos);
    	setBeachLine(sweepLinePos);
    }
}

// Bridge between OpenGL and GUI
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

// Builds the GUI
public class VDDTFrame {

    public static void init(int winWidth, int winHeight) {
        JFrame frame = new JFrame("Voronoi Diagram / Delaunay Triangulation");
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