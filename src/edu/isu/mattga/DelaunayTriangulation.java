package edu.isu.mattga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isu.mattga.DCEL.Face;
import edu.isu.mattga.DCEL.HalfEdge;
import edu.isu.mattga.DCEL.Vertex;

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
