package edu.isu.mattga;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.regex.Pattern;

import edu.isu.mattga.BeachLineStatus.Arc;
import edu.isu.mattga.BeachLineStatus.BreakPoint;
import edu.isu.mattga.DCEL.HalfEdge;
import edu.isu.mattga.DCEL.Vertex;


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
				String points[] = line.replace(" ", "").split(Pattern.quote(")("));

				for(String p : points) {
					String[] coords = p.split(",");
					double x = Double.parseDouble(coords[0].replace("(",""));
					double y = Double.parseDouble(coords[1].replace(")",""));
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
				double d = ce.p.y - p.y;
				if (!circleContainsPoint(ce) && !Q.contains(ce) && d < 1e-5) {
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
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y - p.y < 1e-5) {
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
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y - e.p.y < 1e-5) {
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
				if (!circleContainsPoint(ce) && !Q.contains(ce) && ce.p.y - e.p.y < 1e-5) {
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

		public int compareTo(Event o) {
			return Double.compare(o.p.y, this.p.y);
		}
	}

	enum EventType { 
		SiteEvent,
		CircleEvent
	}
}
