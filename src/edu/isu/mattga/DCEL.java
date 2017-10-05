package edu.isu.mattga;
import java.util.ArrayList;
import java.util.List;


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
