/**********************
 * Model for a point. Note overriding of hashCode for use in Hash tables.
 **********************/
public class Point {
	double x, y;
	
	Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double norm() {
		return Math.sqrt(x*x + y*y);
	}
	
	@Override
	public String toString() {
		return String.format("(%.2f, %.2f)", x, y);
	}
	
	@Override
	public int hashCode() {
		return 1367 * (int)x + 531 * (int)y;
	}
	
	public boolean xyEquals(Point p) {
		return x == p.x && y == p.y;
	}
}

/**********************
 * DCEL class...Vertex, HalfEdge, and Face subclasses are at the bottom (after method implementations)
 **********************/
public class DCEL {
	
	List<Vertex>	vertices;
	List<HalfEdge>	edges;
	List<Face>		faces;

	private int vertexCount = 0;
	
	DCEL() {
		vertices = new ArrayList<Vertex>();
		edges = new ArrayList<HalfEdge>();
		faces = new ArrayList<Face>();
	}
	
	public Vertex addVertex(Point p) {
		Vertex v = new Vertex();
		v.coord = p;
		vertices.add(v);
		v.name = "v" + ++vertexCount;
		v.id = vertexCount;
		
		return v;
	}

	public HalfEdge addEdge() {
		HalfEdge e1 = new HalfEdge();
		HalfEdge e2 = new HalfEdge();
		e1.twin = e2;
		e2.twin = e1;
		edges.add(e1);
		edges.add(e2);
		
		return e1;
	}
	
	public HalfEdge addEdge(Vertex v1, Vertex v2) {
		HalfEdge e1 = new HalfEdge();
		HalfEdge e2 = new HalfEdge();
		e1.twin = e2;
		e1.origin = v1;
		e1.name = String.format("e%d,%d", v1.id, v2.id);
		e2.twin = e1;
		e2.origin = v2;
		e2.name = String.format("e%d,%d", v2.id, v1.id);
		edges.add(e1);
		edges.add(e2);
		
		return e1;
	}
	
	public Face addFace() {
		Face f = new Face();
		f.innerComponents = new ArrayList<HalfEdge>();
		faces.add(f);
		
		return f;
	}
	
	public void setFaces() {
		
		for (HalfEdge e : edges) {
			if (e.incidentFace == null) {
				Face f = addFace();
				HalfEdge cur = e.next;
				e.incidentFace = f;
				// If e is an outer edge of the boundary, set as inner edge of unbounded face
				if (!e.origin.vorVertex && !e.next.origin.vorVertex && !f.innerComponents.contains(e) 
					&& e.origin.coord.x != e.twin.origin.coord.x) {
					f.innerComponents.add(e);
					f.name = "f0";
				}
				else // e is any inner edge (outer edge of a face)
					f.outerComponent = e;
				while (cur != e) {
					cur.incidentFace = f;
					cur = cur.next;
				}
			}
		}
	}

	// Intersect edge e with edge _e at point p
	public HalfEdge intersectEdge(HalfEdge e, HalfEdge _e, Point p) {
		Vertex v = addVertex(p);
		HalfEdge e_new = new HalfEdge();
		HalfEdge e_new_twin = new HalfEdge();
		e_new.twin = e_new_twin;
		e_new_twin.twin = e_new;
		e_new.origin = v; v.incidentEdge = e_new;
		e_new_twin.origin = e.twin.origin;
		e_new.next = e.next; e.next.prev = e_new;
		e.twin.prev.next = e_new_twin; e_new_twin.prev = e.twin.prev;
		e.twin.origin = v;
		e.next = e_new; e_new.prev = e;
		_e.twin.origin = v;
		_e.next = e.twin; e.twin.prev = _e;
		_e.twin.prev = e_new_twin; e_new_twin.next = _e.twin;
		
		e.name = String.format("e%d,%d", e.origin.id, e.twin.origin.id);
		e.twin.name = String.format("e%d,%d", e.twin.origin.id, e.origin.id);
		_e.name = "e" + (_e.origin == null ? "?" : _e.origin.id) + "," + (_e.twin.origin == null ? "?" : _e.twin.origin.id);
		_e.twin.name = "e" + (_e.twin.origin == null ? "?" : _e.twin.origin.id) + "," + (_e.origin == null ? "?" : _e.origin.id);
		e_new.name = String.format("e%d,%d", e_new.origin.id, e_new_twin.origin.id);
		e_new_twin.name = String.format("e%d,%d", e_new_twin.origin.id, e_new.origin.id);
		
		return e_new;
	}
	
	public void setEdgeOrigin(HalfEdge e, Vertex v) {
		
		if (e.origin == null) {
			e.origin = v;
			v.incidentEdge = e;
			
			if (e.twin.origin == null) {
				e.name = "e" + v.id + ",?";
				e.twin.name = "e?," + v.id;
			} else {
				e.name = "e" + v.id + "," + e.twin.origin.id;
				e.twin.name = "e" + e.twin.origin.id + "," + v.id;
			}
		} else {
			e.twin.origin = v;
			v.incidentEdge = e.twin;
			
			if (e.twin.origin == null) {
				e.name = "e?," + v.id;
				e.twin.name = "e" + v.id + ",?";
			} else {
				e.name = "e" + e.origin.id + "," + v.id;
				e.twin.name = "e" + v.id + "," + e.origin.id;
			}
		}
	}

	@Override
	public String toString() {
		String s = "";

		for (Vertex v : vertices) {
			s += v + "\n";
		}
		if (!vertices.isEmpty())
			s += "\n";
		
		for (Face f : faces) {
			s += f + "\n";
		}
		if (!faces.isEmpty())
			s += "\n";

		for (HalfEdge e : edges) {
			s += e + "\n";
		}
		
		return s;
	}
	
	public class Vertex {
		int			id;
		String 		name;
		Point		coord;
		HalfEdge	incidentEdge;

		// Non-DCEL variables
		boolean		vorVertex;
		
		@Override
		public String toString() {
			String s = String.format("%s  (%.1f, %.1f)  ", name, coord.x, coord.y);
			s += (incidentEdge != null ?	incidentEdge.name + "  "			: "nil");
					
			return s;
		}
	}

	public class HalfEdge {
		String		name;
		Vertex		origin;
		HalfEdge	twin, next, prev;
		Face		incidentFace;

		// Non-DCEL variables
		double 		slope;
		int			orientation; // Line lies to the 1: left, 2: right, or 0: unknown of origin

		@Override
		public String toString() {
			String s = name + "  ";
			s += (origin != null ? 			origin.name + "  " 			: "nil  ");
			s += (twin != null ? 			twin.name + "  " 			: "nil  ");
			s += (incidentFace != null ? 	incidentFace.name + "  " 	: "nil  ");
			s += (next != null ? 			next.name + "  " 			: "nil  ");
			s += (prev != null ? 			prev.name + "  " 			: "nil  ");
			
			return s;
		}
	}
	
	public class Face {
		int				id;
		String			name;
		HalfEdge		outerComponent;
		List<HalfEdge>	innerComponents;

		Face() {
			innerComponents = new ArrayList<HalfEdge>();
		}

		@Override
		public String toString() {
			String s = name + "  ";
			s += (outerComponent != null ? outerComponent.name + "  " : "nil  ");
			if (innerComponents == null || innerComponents.isEmpty()) {
				s += "nil";
			} else {
				s += "" + innerComponents.get(0).name;
				for (int i = 1; i < innerComponents.size(); i++) {
					s += "," + innerComponents.get(i).name;
				}
				s += "";
			}
			
			return s;
		}
	}
}


/**********************
 * This is the main class Voronoi Diagram class containing the sweep line algorithm and the
 * DCEL, sweep line, beachline, and query structure.
 **********************/
public class VoronoiDiagram {
	DCEL 					D;
	Queue<Event>			Q;
	BeachLineStatus 		T;
	List<Point>				sitePoints;
	Map<Point,HalfEdge>		siteEdgeAdj;
	Map<Point,List<Point>>	adjSites;
	boolean					collinear; 

	public VoronoiDiagram() {
		sitePoints = new ArrayList<Point>();
		siteEdgeAdj = new HashMap<Point,HalfEdge>();
		adjSites = new HashMap<Point,List<Point>>();
		collinear = false;
	}

	// Loads sites from input file. Checks for site points with the same y-coordinate
	// and adds a small randomized number if found.
	public void loadSites(String fileName) {

		try {
			BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			Random rgen = new Random();

			String line;
			while ((line = fin.readLine()) != null) {
				String points[] = line.split(Pattern.quote(") ("));

				for(String p : points) {
					String[] coords = p.split(",");
					double x = Double.parseDouble(coords[0].replace("(",""));
					double y = Double.parseDouble(coords[1].replace(" ", "").replace(")",""));
					boolean reject = false;

					for (Point pt : sitePoints)
						if (pt.y == y)
							if (pt.x == x)
								reject = true;
							else
								pt.y += rgen.nextDouble()*1e-5;
					
					if (!reject)
						sitePoints.add(new Point(x, y));
					reject = false;
				}
			}

			sitePoints.sort(new Comparator<Point>() {
				public int compare(Point o1, Point o2) {
					return (int)(1e4*(o2.y - o1.y));
				}
			});
			for (Point p : sitePoints)
				adjSites.put(p, new ArrayList<Point>());

			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}

	// Sweep line algorithm for constructing the Voronoi Diagram. Also sets variables for rendering
	// of the diagram using VDDrawing.
	public void constructVD() {

		Q = new PriorityQueue<Event>(getEvents(sitePoints));
		T = new BeachLineStatus();
		D = new DCEL();

		VDDrawing.addPoints(sitePoints);
		VDDrawing.beachLine = T.getBeachLine();
		VDDrawing.voronoiDiagram = D;
		VDDrawing.beachLines.put(Double.MAX_VALUE, T.copy().getBeachLine());
		try { // Wait a little to allow for OpenGL to propagate new projection settings
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		while (!Q.isEmpty()) {
			Event e = Q.peek();
			double y = e.p.y; 
			VDDrawing.sweepLinePos = (float)y;

			if (e.type == EventType.SiteEvent) {
				System.out.println("---- Processing site " + e.p + " ----\n");
				handleSiteEvent(e);
			} else {
				System.out.println("---- Processing circle at " + new Point(e.p.x, e.p.y) + " ----\n");
				handleCircleEvent(e);
			}
			Q.remove(e);

			VDDrawing.beachLines.put(e.p.y, T.copy().getBeachLine());
			VDDrawingSwing.glcanvas.display();

			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

		addBoundingBox();
		D.setFaces();
		mapFacesToVorCells();
		
		System.out.println("****** Voronoi Diagram ******\n");
		System.out.println(D);
	}

	private void handleSiteEvent(Event e) {

		Point p = e.p;
		if (T.isEmpty()) {
			T.initWithArc(p);
			return;
		}

		Arc arc = T.getArcAbove(p, p.y); // Sweep line is at p.y
		if (arc.circleEvent != null) {
			Q.remove(arc.circleEvent); // False alarm
		}

		BreakPoint[] bps = T.splitArc(arc, p);
		BreakPoint lbp = bps[0];
		BreakPoint rbp = bps[1];

		// Determine slope of the edge traced out by lbp and rbp
		HalfEdge e1 = D.addEdge();
		adjSites.get(lbp.leftArc.p).add(lbp.rightArc.p);
		adjSites.get(lbp.rightArc.p).add(lbp.leftArc.p);
		siteEdgeAdj.put(lbp.rightArc.p,e1);
		siteEdgeAdj.put(lbp.leftArc.p,e1);
		lbp.tracedEdge = e1;
		rbp.tracedEdge = e1;
		double dl = .5;
		double x = rbp.getPositionX(p.y - dl);
		Point p1 = new Point(p.x, rbp.rightArc.y(p.x, p.y));
		Point p2 = new Point(x, rbp.rightArc.y(x, p.y - dl));
		e1.twin.slope = e1.slope = (p2.y - p1.y) / (p2.x - p1.x);

		Arc _a;
		Point pi, pj, pk;
		_a = T.getRightArc(rbp.rightArc);
		pi = rbp.leftArc.p;
		pj = rbp.rightArc.p;
		if (_a != null) {
			pk = _a.p;
			Event ce = getCircleEvent(pi, pj, pk);
			if (ce != null) {
				ce.dArc = rbp.rightArc;
				ce.pi = pi; ce.pj = pj; ce.pk = pk;
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y < p.y) {
					rbp.rightArc.circleEvent = ce;
					Q.add(ce);
				}
			}
		}

		_a = T.getLeftArc(lbp.leftArc);
		pj = lbp.rightArc.p;
		pk = lbp.leftArc.p;
		if (_a != null) {
			pi = _a.p;
			Event ce = getCircleEvent(pi, pj, pk);
			if (ce != null) {
				ce.dArc = lbp.leftArc;
				ce.pi = pi; ce.pj = pj; ce.pk = pk;
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y < p.y) {
					lbp.leftArc.circleEvent = ce;
					Q.add(ce);
				}
			}
		}
	}

	private void handleCircleEvent(Event e) {
		BreakPoint[] bps = T.removeArc(e.p);
		BreakPoint lbp = bps[0], rbp = bps[1], newbp = bps[2];
		if (lbp.leftArc.circleEvent != null)
			Q.remove(lbp.leftArc.circleEvent);
		if (rbp.rightArc.circleEvent != null)
			Q.remove(rbp.rightArc.circleEvent);

		Point center = new Point(e.p.x, e.p.y + e.radius);
		Vertex v = D.addVertex(center);
		v.vorVertex = true;

		D.setEdgeOrigin(lbp.tracedEdge, v);
		double dl = .5;
		double x = lbp.getPositionX(e.p.y - dl);
		if (x > e.p.x) {
			if (lbp.tracedEdge.orientation == 0) {
				lbp.tracedEdge.orientation = 1;
			}
		} else {
			if (lbp.tracedEdge.orientation == 0) {
				lbp.tracedEdge.orientation = 2;
			}
		}

		D.setEdgeOrigin(rbp.tracedEdge, v);
		x = rbp.getPositionX(e.p.y - dl);
		if (x > e.p.x) {
			if (rbp.tracedEdge.orientation == 0) {
				rbp.tracedEdge.orientation = 1;
			}
		} else {
			if (rbp.tracedEdge.orientation == 0) {
				rbp.tracedEdge.orientation = 2;
			}
		}

		// new edge between points whose arcs now intersect at newbp
		HalfEdge _e = D.addEdge();
		D.setEdgeOrigin(_e, v);
		adjSites.get(newbp.rightArc.p).add(newbp.leftArc.p);
		adjSites.get(newbp.leftArc.p).add(newbp.rightArc.p);
		newbp.tracedEdge = _e;
		dl = .5;
		x = newbp.getPositionX(e.p.y - dl);
		Point p1 = new Point(e.p.x, e.p.y + e.radius);
		Point p2 = new Point(x, newbp.rightArc.y(x, e.p.y - dl));
		_e.twin.slope = _e.slope = (p2.y - p1.y) / (p2.x - p1.x);
		if (x - e.p.x > 0) {
			if (_e.orientation == 0)
				_e.orientation = 2;
		} else {
			if (_e.orientation == 0)
				_e.orientation = 1;
		}

		// Set next & prev pointers
		HalfEdge e1 = (lbp.tracedEdge.origin == v ? lbp.tracedEdge.twin : lbp.tracedEdge);
		HalfEdge e2 = (rbp.tracedEdge.origin == v ? rbp.tracedEdge : rbp.tracedEdge.twin);
		HalfEdge e3 = (_e.origin == v ? _e : _e.twin);
		e1.next = e2; e2.prev = e1;
		e2.twin.next = e3; e3.prev = e2.twin;
		e3.twin.next = e1.twin; e1.twin.prev = e3.twin;
		
		Arc _a;
		Point pi, pj, pk;
		_a = T.getLeftArc(newbp.leftArc);
		pj = newbp.leftArc.p;
		pk = newbp.rightArc.p;
		if (_a != null) {
			pi = _a.p;
			Event ce = getCircleEvent(pi, pj, pk);
			if (ce != null) {
				ce.dArc = newbp.leftArc;
				ce.pi = pi; ce.pj = pj; ce.pk = pk;
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y < e.p.y) {
					newbp.leftArc.circleEvent = ce;
					Q.add(ce);
				}
			}
		}

		_a = T.getRightArc(newbp.rightArc);
		pi = newbp.leftArc.p;
		pj = newbp.rightArc.p;
		if (_a != null) {
			pk = _a.p;
			Event ce = getCircleEvent(pi, pj, pk);
			if (ce != null) {
				ce.dArc = newbp.rightArc;
				ce.pi = pi; ce.pj = pj; ce.pk = pk;
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y < e.p.y) {
					newbp.rightArc.circleEvent = ce;
					Q.add(ce);
				}
			}
		}
	}

	// Get the circle event for the three points a, b, and c, including radius and origin.
	private Event getCircleEvent(Point a, Point b, Point c) {

		if (Math.abs(a.y - b.y) < 1e-5 && Math.abs(b.y - c.y) < 1e-5 && Math.abs(a.y - c.y) < 1e-5)
			return null; // Points are colinear
		
		// Calculate the circle
		double A = b.x - a.x;
		double B = b.y - a.y;
		double C = c.x - a.x;
		double _D = c.y - a.y;
		double E = A*(a.x+b.x) + B*(a.y+b.y);
		double F = C*(a.x+c.x) + _D*(a.y+c.y);
		double G = 2*(A*(c.y-b.y) - B*(c.x-b.x));

		// Circle with radius r and origin o
		Point o = new Point( (_D*E-B*F)/G, (A*F-C*E)/G );
		double r = Math.sqrt(Math.pow(o.x-a.x,2) + Math.pow(o.y-a.y,2));

		Event e = new Event();
		e.type = EventType.CircleEvent;
		e.p = new Point(o.x, o.y - r); // set lowest point of circle
		e.radius = r;

		return e;
	}

	// Checks if any site point lies inside a circle
	public boolean circleContainsPoint(Event ce) {

		for (Point p : sitePoints) {
			if (p == ce.pi || p == ce.pj || p == ce.pk)
				continue;
			
			Point o = new Point(ce.p.x, ce.p.y + ce.radius);
			double d = Math.pow(p.x - o.x, 2) + Math.pow(p.y - o.y, 2) - ce.radius*ce.radius;
			if (d < -1e-5)
				return true;
		}

		return false;
	}

	private void addBoundingBox() {
		
		// Determine the bounding box
		double left = 0, right = 0, top = 0, bottom = 0;
		for (Vertex v : D.vertices) {
			if (v.coord.x < left)
				left = v.coord.x;
			if (v.coord.x > right)
				right = v.coord.x;
			if (v.coord.y > top)
				top = v.coord.y;
			if (v.coord.y < bottom)
				bottom = v.coord.y;
		}
		for (Point p : sitePoints) {
			if (p.x < left)
				left = p.x;
			if (p.x > right)
				right = p.x;
			if (p.y > top)
				top = p.y;
			if (p.y < bottom)
				bottom = p.y;
		}
		// Add some buffer between outermost sites/vertices and the box
		left	*= 1.5;
		right	*= 1.5;
		top		*= 1.5;
		bottom	*= 1.5;
		Vertex v1 = D.addVertex(new Point(left, top));
		Vertex v2 = D.addVertex(new Point(right, top));
		Vertex v3 = D.addVertex(new Point(right, bottom));
		Vertex v4 = D.addVertex(new Point(left, bottom));
		HalfEdge e12 = D.addEdge(v1, v2); // top edge
		HalfEdge e23 = D.addEdge(v2, v3); // right edge
		HalfEdge e34 = D.addEdge(v3, v4); // bottom edge
		HalfEdge e41 = D.addEdge(v4, v1); // left edge
		v1.incidentEdge = e12; v1.vorVertex = false;
		v2.incidentEdge = e23; v2.vorVertex = false;
		v3.incidentEdge = e34; v3.vorVertex = false;
		v4.incidentEdge = e41; v4.vorVertex = false;
		e12.next = e23; e12.prev = e41;
		e23.next = e34; e23.prev = e12;
		e34.next = e41; e34.prev = e23;
		e41.next = e12; e41.prev = e34;
		e12.twin.next = e41.twin; e12.twin.prev = e23.twin;
		e41.twin.next = e34.twin; e41.twin.prev = e12.twin;
		e34.twin.next = e23.twin; e34.twin.prev = e41.twin;
		e23.twin.next = e12.twin; e23.twin.prev = e34.twin;
		// Initialize boundary lists and add outer edges of the bounding box (edges that traverse cw)
		List<HalfEdge> topEdges = new ArrayList<HalfEdge>(); topEdges.add(e12);
		List<HalfEdge> rightEdges = new ArrayList<HalfEdge>(); rightEdges.add(e23);
		List<HalfEdge> bottomEdges = new ArrayList<HalfEdge>(); bottomEdges.add(e34);
		List<HalfEdge> leftEdges = new ArrayList<HalfEdge>(); leftEdges.add(e41);
		// Add these edges at the end to avoid modifying the list we are iterating.
		List<HalfEdge> newEdges = new ArrayList<HalfEdge>(); 
		
		for (HalfEdge e : D.edges) {
			HalfEdge _e = null; // Determine edge whose origin will intersect with the bounding box
			if ( e.origin == null)
				_e = e.twin;
			else if (e.twin.origin == null)
				_e = e;

			if (_e != null) {
				if (_e.origin == null) {
					// No origin...Must be the case of collinear points with all vertical edges
					collinear = true;
					boundVerticalLines(top, topEdges, bottom, bottomEdges);
					return;
				}
				
				Point o = _e.origin.coord;
				if (_e.slope == 0.0)
					_e.slope += 1e-5;
				if (_e.slope > 0 && _e.orientation == 2) { // up and to the right
					double top_x = (top - o.y) / _e.slope + o.x;
					double right_y = _e.slope * (right - o.x) + o.y;
					
					if (top_x < right) {
						// Find top bounding edge intersected at (top_x, top)
						HalfEdge e_intersected = topEdges.get(0);
						for (int i = 0; top_x > e_intersected.twin.origin.coord.x ||
										top_x < e_intersected.origin.coord.x; i++) 
							e_intersected = topEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(top_x, top));
						topEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					} else {
						// Find right bounding edge intersected at (right, right_y)
						HalfEdge e_intersected = rightEdges.get(0);
						for (int i = 0; right_y < e_intersected.twin.origin.coord.y ||
										right_y > e_intersected.origin.coord.y; i++) 
							e_intersected = rightEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(right, right_y));
						rightEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					}
				} else if (_e.slope < 0 && _e.orientation == 2) { // down and to the right
					double right_y = _e.slope * (right - o.x) + o.y;
					double bottom_x = (bottom - o.y) / _e.slope + o.x;

					if (bottom_x < right) {
						// Find bottom bounding edge intersected at (bottom_x, bottom)
						HalfEdge e_intersected = bottomEdges.get(0);
						for (int i = 0; bottom_x < e_intersected.twin.origin.coord.x ||
										bottom_x > e_intersected.origin.coord.x; i++) 
							e_intersected = bottomEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(bottom_x, bottom));
						bottomEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					} else {
						// Find right bounding edge intersected at (right, right_y)
						HalfEdge e_intersected = rightEdges.get(0);
						for (int i = 0; right_y < e_intersected.twin.origin.coord.y ||
										right_y > e_intersected.origin.coord.y; i++) 
							e_intersected = rightEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(right, right_y));
						rightEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					}
				} else if (_e.slope > 0 && _e.orientation == 1) { // down and to the left
					double bottom_x = (bottom - o.y) / _e.slope + o.x;
					double left_y = _e.slope * (left - o.x) + o.y;

					if (bottom_x > left) {
						// Find bottom bounding edge intersected at (bottom_x, bottom)
						HalfEdge e_intersected = bottomEdges.get(0);
						for (int i = 0; bottom_x < e_intersected.twin.origin.coord.x ||
										bottom_x > e_intersected.origin.coord.x; i++) 
							e_intersected = bottomEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(bottom_x, bottom));
						bottomEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					} else {
						// Find left bounding edge intersected at (left, left_y)
						HalfEdge e_intersected = leftEdges.get(0);
						for (int i = 0; left_y > e_intersected.twin.origin.coord.y ||
										left_y < e_intersected.origin.coord.y; i++) 
							e_intersected = leftEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(left, left_y));
						leftEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					}
				} else if (_e.slope < 0 && _e.orientation == 1) { // up and to the left
					double top_x = (top - o.y) / _e.slope + o.x;
					double left_y = _e.slope * (left - o.x) + o.y;

					if (top_x > left) {
						// Find top bounding edge intersected at (top_x, top)
						HalfEdge e_intersected = topEdges.get(0);
						for (int i = 0; top_x > e_intersected.twin.origin.coord.x ||
										top_x < e_intersected.origin.coord.x; i++) 
							e_intersected = topEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(top_x, top));
						topEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					} else {
						// Find left bounding edge intersected at (left, left_y)
						HalfEdge e_intersected = leftEdges.get(0);
						for (int i = 0; left_y > e_intersected.twin.origin.coord.y ||
										left_y < e_intersected.origin.coord.y; i++) 
							e_intersected = leftEdges.get(i);
						HalfEdge e_new = D.intersectEdge(e_intersected, _e, new Point(left, left_y));
						leftEdges.add(e_new);
						newEdges.add(e_new); newEdges.add(e_new.twin);
					}
				}
//				System.out.println(D);
			}
		}
		D.edges.addAll(newEdges);
	}
	
	private void boundVerticalLines(double top, List<HalfEdge> tE, double bottom, List<HalfEdge> bE) {
		
		Collections.sort(sitePoints, new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
				return (int)(1e4*(o1.x - o2.x));
			}
		});
		HalfEdge e1, e2 = null;
		for (int i = 0; i < sitePoints.size()-1; i++) {
			double x_mid = sitePoints.get(i).x + (sitePoints.get(i+1).x - sitePoints.get(i).x) / 2;
			e1 = D.edges.get(2*i);
			e2 = D.edges.get(2*i+1);
			siteEdgeAdj.put(sitePoints.get(i), e1);
			e1.twin = e2; e2.twin = e1;
			
			HalfEdge e_intersected = tE.get(0);
			for (int j = 0; x_mid > e_intersected.twin.origin.coord.x ||
							x_mid < e_intersected.origin.coord.x; j++) 
				e_intersected = tE.get(j);
			HalfEdge e_new = D.intersectEdge(e_intersected, e1, new Point(x_mid, top));
			tE.add(e_new);
			D.edges.add(e_new); D.edges.add(e_new.twin);

			e_intersected = bE.get(0);
			for (int j = 0; x_mid < e_intersected.twin.origin.coord.x ||
							x_mid > e_intersected.origin.coord.x; j++) 
				e_intersected = bE.get(j);
			e_new = D.intersectEdge(e_intersected, e2, new Point(x_mid, bottom));
			bE.add(e_new);
			D.edges.add(e_new); D.edges.add(e_new.twin);
		}
		siteEdgeAdj.put(sitePoints.get(sitePoints.size()-1), e2);
	}
	
	private void mapFacesToVorCells() {
		for (Map.Entry<Point,HalfEdge> e : siteEdgeAdj.entrySet()) {
			Point site = e.getKey();
			HalfEdge edge = e.getValue();
			
			// Check what edge we want based on the turn it makes with the site
			Point a = new Point(edge.twin.origin.coord.x - edge.origin.coord.x, edge.twin.origin.coord.y - edge.origin.coord.y);
			Point b = new Point(site.x - edge.origin.coord.x, site.y - edge.origin.coord.y);
			double dir = a.x*b.y - a.y*b.x;
			if (dir < 0)
				edge = edge.twin;
			
			edge.incidentFace.id = sitePoints.indexOf(site) + 1;
			edge.incidentFace.name = "c" + edge.incidentFace.id;
		}
	}
	
	private List<Event> getEvents(List<Point> ps) {
		List<Event> es = new ArrayList<Event>();

		for (Point p : ps) {
			Event e = new Event();
			e.p = p;
			e.type = EventType.SiteEvent;
			es.add(e);
		}

		return es;
	}

	public class Event implements Comparable<Event> {
		Point p;
		EventType type;
		
		// Circle event variables
		Point pi, pj, pk;
		Arc dArc;
		double radius;

		@Override
		public boolean equals(Object o) {
			Event e = (Event)o;
			if (e.type == EventType.CircleEvent && type == EventType.CircleEvent)
				return	(pi == e.pi || pi == e.pj || pi == e.pk) &&
						(pj == e.pi || pj == e.pj || pj == e.pk) &&
						(pk == e.pi || pk == e.pj || pk == e.pk);
			else
				return Math.abs(e.p.y - this.p.y) < 1e-8;
		}

		@Override
		public int compareTo(Event o) {
			return Double.compare(o.p.y, this.p.y);
		}
	}

	enum EventType { 
		SiteEvent,
		CircleEvent
	}
}


/**********************
 * Delaunay Triangulation construction
 **********************/
 public class DelaunayTriangulation {
	
	DCEL	D;
	int		tCount;
	
	public DelaunayTriangulation() {
		D = new DCEL();
		tCount = 0;
	}

	public void constructDT(VoronoiDiagram vd) {

		Map<Point,Vertex> 			pv = new HashMap<Point,Vertex>();
		Map<Point,List<HalfEdge>>	pe = new HashMap<Point,List<HalfEdge>>();
		for  (Point p : vd.sitePoints)
			pe.put(p, new ArrayList<HalfEdge>());

		VDDrawing.delaunayTriangulation = D;
		
		// Here, we dualize a Voronoi Diagram that includes multiple voronoi vertices at circumcircles that
		// share very close centers (as a result of floating point imprecision). This works in that our favor 
		// for constructing the Delaunay Triangulation as it produces edges between sites that are corcircular. 
		// Effectively, cocircular sites are already triangulated.
		//
		// In other words, using floating point imprecision to ensure that no two points are collinear and no 
		// four or more points are cocircular, we process the Voronoi Diagram in general position. We then
		// construct the Delaunay Graph, which is also the Delaunay triangulation due to the VD being in
		// general position. The Vorononoi diagram then removes these multiple vertices in a post-processing
		// step so that the DCEL is cleaner (but not in general position).
		addEdgesFromVdDual(vd, pv, pe);
		
		if (vd.collinear) {
			D = null;
		} else {
			// Set next & previous pointers for inner edges of the unbounded face
			setPointersForOuterEdges(vd, pv, pe);

			// Set next & previous pointers for outer edges of the bounded faces (ccw traversal)
			setPointersForInnerEdges(vd, pv, pe);
			
			for (Vertex v : D.vertices) {
				v.id = vd.sitePoints.indexOf(v.coord) + 1;
				v.name = "p" + v.id;
			}
			for (HalfEdge e : D.edges)
				e.name = String.format("d%d,%d", e.origin.id, e.twin.origin.id);
		}
		
		System.out.println("\n\n****** Delaunay Triangulation ******\n");
		System.out.println(D);
		VDDrawingSwing.glcanvas.display();
	}

	private void addEdgesFromVdDual(VoronoiDiagram vd, Map<Point,Vertex> pv, Map<Point,List<HalfEdge>> pe) {
		// Add vertices and edges
				Point pi = null;
				Vertex vi, vj;
				for (Map.Entry<Point,List<Point>> e : vd.adjSites.entrySet()) {
					
					boolean edgeExists = false;
					pi = e.getKey();
					vi = pv.get(pi);		// O(1) with hashmap

					if (vi == null) {
						vi  = D.addVertex(pi);
						pv.put(pi, vi);		// O(1) with hashmap
					}
					
					for (Point pj : e.getValue()) {

						vj = pv.get(pj);  	// O(1) with hashmap
						if (vj == null) {
							vj  = D.addVertex(pj);
							pv.put(pj, vj);	// O(1) with hashmap
						}
						
						for (HalfEdge _e : pe.get(pj))
							if (_e.origin.coord.x == pj.x && _e.origin.coord.y == pj.y &&
								_e.twin.origin.coord.x == pi.x && _e.twin.origin.coord.y == pi.y)
								edgeExists = true;
						if (!edgeExists) {
							HalfEdge e1 = D.addEdge(vi,vj);
							pe.get(pi).add(e1);
							pe.get(pj).add(e1);
							vi.incidentEdge = e1;
							vj.incidentEdge = e1;
						}
						edgeExists = false;
					}
				}
	}

	private void setPointersForOuterEdges(VoronoiDiagram vd, Map<Point,Vertex> pv, Map<Point,List<HalfEdge>> pe) {
		
		Point minp = vd.sitePoints.get(0);
		Vertex vi, vj;
		double angle, maxAngle = -Double.MAX_VALUE;
		Face f = D.addFace();
		f.id = 0;
		f.name = "f0";
		
		for (Point p : vd.sitePoints)
			if (p.x < minp.x)
				minp = p;
		vi = pv.get(minp);
		HalfEdge e0 = pe.get(vi.coord).get(0);
		HalfEdge ei, ej = null;
		e0 = (e0.origin == vi ? e0.twin : e0);
		for (HalfEdge e : pe.get(vi.coord)) {
			HalfEdge _e1 = (e.origin == vi ? e.twin : e);
			angle = getAngle(new Point(1,0), vi.coord, _e1.origin.coord);
			if (angle > maxAngle) {
				maxAngle = angle;
				e0 = _e1.twin;
			}
		}
		ei = e0;
		f.innerComponents.add(ei);
		
		while (ej != e0) {
			vj = ei.twin.origin;
			maxAngle = 0;
			for (HalfEdge e : pe.get(vj.coord)) {
				HalfEdge _ej = (e.origin == vj ? e.twin : e);
				angle = getAngleCCW(vi.coord, vj.coord, _ej.origin.coord);
				if (angle > maxAngle) {
					maxAngle = angle;
					ej = _ej.twin;
				}
			}
			
			ei.next = ej; ej.prev = ei;
			vi = vj;
			ei = ej;
			ej.incidentFace = f;
		}
	}

	private void setPointersForInnerEdges(VoronoiDiagram vd, Map<Point,Vertex> pv, Map<Point,List<HalfEdge>> pe) {
		Vertex vi, vj;
		double angle, maxAngle;
		HalfEdge ei, ej = null;
		
		for (HalfEdge e : D.edges) {
			if (e.next == null) {
				Face f = D.addFace();
				f.id = ++tCount;
				f.name = "t" + f.id;

				// Connect the cycle of edges around face f
				ei = e;
				vi = e.origin;
				f.outerComponent = ei;
				while (ej != e) {
					vj = ei.twin.origin;
					maxAngle = 0;
					for (HalfEdge e_ : pe.get(vj.coord)) {
						HalfEdge _ej = (e_.origin == vj ? e_.twin : e_);
						angle = getAngleCCW(vi.coord, vj.coord, _ej.origin.coord);
						if (angle > maxAngle) {
							maxAngle = angle;
							ej = _ej.twin;
						}
					}
					
					ei.next = ej; ej.prev = ei;
					vi = vj;
					ei = ej;
					ej.incidentFace = f;
				}
			}
		}
	}

	private double getAngleCCW(Point p1, Point p2, Point p3) {
		Point a = new Point(p1.x - p2.x, p1.y - p2.y);
		Point b = new Point(p3.x - p2.x, p3.y - p2.y);
		
		double adotb = a.x*b.x + a.y*b.y;
		double axb = a.x*b.y - a.y*b.x;
		double angle = Math.atan2(axb, adotb);
		
		return (angle < 0 ? angle + 2*Math.PI : angle);
	}

	private double getAngle(Point p1, Point p2, Point p3) {
		Point a = new Point(p1.x - p2.x, p1.y - p2.y);
		Point b = new Point(p3.x - p2.x, p3.y - p2.y);
		
		double axb = a.x*b.y - a.y*b.x;
		double adotb = a.x*b.x + a.y*b.y;
		
		if (axb > 0)
			return Math.acos(adotb / (a.norm()*b.norm()));
		else
			return -Math.acos(adotb / (a.norm()*b.norm()));
	}
}


/**********************
 * Binary Search Tree Beach Line status structure
 **********************/
public class BeachLineStatusBST {

	private int size = 0;
	private Node root;

	public boolean removeArc(Arc a) {

		// a is the only arc
		if (root == a) {
			root = null;
			size = 0;
		}

		BreakPoint a_parent = a.parent;
		BreakPoint a_gparent = a_parent;
		Arc a_left = getLeftArc(a);
		Arc a_right = getRightArc(a);

		if (a_left.p == a_right.p) { // should never happen
			if (a_gparent == root) {
				root = a_left;
			} else {
				BreakPoint a_ggparent = a_gparent;
				if (a_gparent == a_ggparent.left) // is a_gparent the left or right child of a_ggparent?
					a_ggparent.left = a_left;
				else
					a_ggparent.right = a_left;
			}
		} else { // a_parent and a_gparent are merging
			BreakPoint newbp = new BreakPoint();
			newbp.leftArc = a_left;
			newbp.rightArc = a_right;

			BreakPoint lbp = a_left.parent;
			BreakPoint rbp = a_right.parent;
			while (lbp.leftArc != a_left && lbp.rightArc != a) // Find split node with <a_left, a>
				lbp = lbp.parent;
			while (rbp.leftArc != a && rbp.rightArc != a_right) // Find split node with <a, a_right>
				rbp = rbp.parent;

			if (lbp == rbp.left) {
				newbp.left = lbp.left;
				newbp.right = lbp.right;
				rbp.replace(newbp);
			} else if (rbp == lbp.right) {
				newbp.left = lbp.left;
				newbp.right = rbp.right;
				lbp.replace(newbp);
			} else { // lbp is > 1 level higher than rbp
				  newbp.left = lbp.left;
				  newbp.right = lbp.right;
				  rbp.replace(rbp.right); // rbp.left is the arc to disappear, so replace the breakpoint with the right arc
				  lbp.replace(newbp);
			}
		}

		return true;
	}

	public BreakPoint[] splitArc(Arc a, Point p) {

		/**
		 * 						newbp2
		 * 					   /	  \
		 * 		arc	 -->  	newbp1	  arc
		 * 				   /	  \
		 * 				 arc    newArc
		 */
		Arc newArc = new Arc(p);

		BreakPoint newbp1 = new BreakPoint();
		newbp1.left = newbp1.leftArc = a;
		newbp1.right = newbp1.rightArc = newArc;

		BreakPoint newbp2 = new BreakPoint();
		newbp2.right = newbp2.rightArc = a;
		newbp2.leftArc = newArc;
		newbp2.left = newbp1;

		a.replace(newbp2);

		++size;

		return new BreakPoint[]{newbp1, newbp2};
	}

	public Arc getArcAbove(Point p, double ly) {

		Node cur = root;

		if (root == null) {
			return null;
		} else {
			while (!cur.isLeaf) {
				BreakPoint _cur = (BreakPoint)cur;
				double bp_x = _cur.getPositionX(ly);
				if (p.x < bp_x)
					cur = _cur.left;
				else
					cur = _cur.right;
			}
		}

		return (Arc)cur;
	}

	public Arc getLeftArc(Arc a) {

		Arc res = null;
		BreakPoint a_parent = a.parent;
		if (a.parent.rightArc == a)
			res = a.parent.leftArc;
		else
			if (a_parent != root){ // Has no right arc otherwise
				if (a_parent.parent.right == a_parent) {
					res = a_parent.parent.leftArc;
				} else if (((BreakPoint)root).rightArc == a) {
					res =  ((BreakPoint)root).leftArc;
				}
			}

		return (Arc)res;
	}

	public Arc getRightArc(Arc a) {

		Arc res = null;
		BreakPoint a_parent = a.parent;
		if (a.parent.leftArc == a)
			res = a.parent.rightArc;
		else
			if (a_parent != root){ // Has no right arc otherwise
				if (a_parent.parent.left == a_parent) {
					res = a_parent.parent.rightArc;
				} else if (((BreakPoint)root).leftArc == a) {
					res =  ((BreakPoint)root).rightArc;
				}
			}

		return (Arc)res;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int getSize() {
		return size;
	}

	/**
	 * 	  BST Node Hierarchy:
	 * 		   	 Node
	 * 	      	/    \
	 * 	 ArcNode	  BreakPointNode
	 */
	public class Node {
		BreakPoint parent;
		Node left, right;
		boolean isLeaf;

		public void replace(Node v) {
			if (this == root) {
				root = v;
			} else {
				if (parent.left == this) {
					parent.left = v;
				} else {
					parent.right = v;
				}
			}
		}
	}

	public class Arc extends Node {

		Point p;
		Event circleEvent;

		public Arc() {
			this.isLeaf = true;
		}

		public Arc(Point p) {
			this.p = p;
			this.isLeaf = true;
		}

		// Constants for parabola with focus p=(p.x, p.y) and directrix l : y=ly
		public double y(double x, double ly) {
			return (x*x - 2*p.x*x + p.x*p.x + p.y*p.y - ly*ly) / (2 * (p.y - ly));
		}
	}

	public class BreakPoint extends Node {
		Arc leftArc;
		Arc rightArc;
		HalfEdge tracedEdge;

		public BreakPoint() {
			this.isLeaf = false;
		}

		public double getPositionX(double ly) {
			double res1;
			Point p1 = leftArc.p;
			Point p2 = rightArc.p;

			double d1 = 2 * (p1.y - ly);
			double d2 = 2 * (p2.y - ly);

			double a = 1/d1 - 1/d2;
			double b = (-2 * p1.x) / d1 - (-2 * p2.x) / d2;
			double c = (p1.x*p1.x + p1.y*p1.y - ly*ly) / d1 - (p2.x*p2.x + p2.y*p2.y - ly*ly) / d2;

			res1 = (-b + Math.sqrt(b*b - 4*a*c)) / (2*a);

			return res1;
		}
	}
}