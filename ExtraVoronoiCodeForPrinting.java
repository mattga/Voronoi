/**********************
 * Main method
 **********************/
public class Main {
	
	public static void main(String[] args) {

			VoronoiDiagram vd = new VoronoiDiagram();
			vd.loadSites("input.txt");
			
			DelaunayTriangulation dt = new DelaunayTriangulation();

			VDDTFrame.init(800, 800);
			vd.constructVD();
			dt.constructDT(vd);
			VDDTPrintWriter.printVoronoi(vd, "voronoi.txt");
			VDDTPrintWriter.printDelaunay(dt, "voronoi.txt");
	}
}


/**********************
 * For outputting to file
 **********************/
public class VDDTPrintWriter {
	
	private static BufferedWriter 	bw;
	private static PrintWriter 		pw;
	
	public static void printVoronoi(VoronoiDiagram vd, String fileName) {
		try {
			bw = new BufferedWriter(new FileWriter(fileName, true));
			pw = new PrintWriter(bw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int pCount = 0;
		String s = "****** Voronoi Diagram ******\n\n";
		
		for (Point p : vd.sitePoints)
			s += "p" + ++pCount + "  " + p + "\n";
		s += "\n";
		
		s += vd.D;
		s += "\n\n";
		
		pw.append(s);
		pw.close();
	}
	
	public static void printDelaunay(DelaunayTriangulation dt, String fileName) {
		try {
			bw = new BufferedWriter(new FileWriter(fileName, true));
			pw = new PrintWriter(bw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String s = "****** Delaunay Triangulation ******\n\n";
		
		if (dt.D == null)
			s += "Points collinear...no triangulation exists.";
		else
			s += dt.D;
		s += "\n\n";
		
		pw.append(s);
		pw.close();
	}
}


/**********************
 * Linear implementation of beach line status.
 **********************/
public class BeachLineStatus {

	private List<BreakPoint> l;

	public BeachLineStatus() {
		l = new ArrayList<BreakPoint>();
	}
	
	public BreakPoint[] removeArc(Point p) {

		int i = 0;
		BreakPoint rbp = null, lbp = null;
		double bp_x;
		for (; i < l.size(); i++) {
			bp_x = l.get(i).getPositionX(p.y);
			if (p.x - bp_x < 1e-8) {
				assert(Math.abs(l.get(i+1).getPositionX(p.y) - p.x) < 1e-8);
				lbp = l.get(i);
				rbp = l.get(i+1);
				break;
			}
		}

		if (lbp == null)
			return null;

		BreakPoint newbp = new BreakPoint();
		newbp.leftArc = lbp.leftArc;
		newbp.rightArc = rbp.rightArc;
		l.remove(i); // remove lbp
		l.remove(i); // remove rbp
		l.add(i, newbp);

		return new BreakPoint[]{lbp, rbp, newbp};
	}
	
	public void initWithArc(Point p) {
		
		// Add imaginary breakpoint on the right
		Arc a = new Arc(p);
		BreakPoint bp = new BreakPoint();
		bp.leftArc = a;
		l.add(bp);
	
	}
	
	// Arc a split by the new arc of site point p
	public BreakPoint[] splitArc(Arc a, Point p) {
		
		Arc newArc = new Arc(p);
		
		BreakPoint newbp1 = new BreakPoint();
		newbp1.rightArc = newArc;
		
		BreakPoint newbp2 = new BreakPoint();
		newbp2.leftArc = newArc;
		
		if (l.size() == 1) {
			newbp1.leftArc = a.copy();	// these can't have a circle event
			newbp2.rightArc = a.copy();	// so copy is ok
			l.remove(0);
			l.add(newbp1);
			l.add(newbp2);
		} else {
			BreakPoint bp = l.get(0);
			if (bp.leftArc == a) {
				newbp1.leftArc = a;
				newbp2.rightArc = bp.leftArc = a.copy();
				l.add(0, newbp2);
				l.add(0, newbp1);
			} else {
				int i = 0;
				for (; i < l.size(); i++) {
					bp = l.get(i);
					if (bp.rightArc == a)
						break;
				}
				newbp1.leftArc = bp.rightArc = a.copy();
				newbp2.rightArc = a;
				l.add(i+1, newbp2);
				l.add(i+1, newbp1);
			}
		}
		
		return new BreakPoint[]{newbp1, newbp2};
	}
	
	public Arc getArcAbove(Point p, double ly) {
 
		if (l.size() == 1) {
			return l.get(0).leftArc;
		}
		
		for (BreakPoint bp : l) {
			double bp_x = bp.getPositionX(ly);
			if (p.x > bp_x) {
				continue;
			} else {
				return bp.leftArc;
			}
		}
		
		return l.get(l.size()-1).rightArc;
	}

	public Arc getLeftArc(Arc a) {

		if (l.size() > 1) {
			for (BreakPoint bp : l)
				if (bp.rightArc == a)
					return bp.leftArc;
		}

		return null; // a is the first arc
	}

	public Arc getRightArc(Arc a) {

		if (l.size() > 1) {
			for (BreakPoint bp : l)
				if (bp.leftArc == a)
					return bp.rightArc;
		}

		return null; // a is the last arc
	}

	public boolean isEmpty() {
		return l.isEmpty();
	}
	
	public List<BreakPoint> getBeachLine() {
		return l;
	}
	
	public BeachLineStatus copy() {
		BeachLineStatus r = new BeachLineStatus();
		
		if (l.size() == 1) {
			BreakPoint _bp = new BreakPoint();
			_bp.leftArc = new Arc(l.get(0).leftArc.p);
			r.l.add(_bp);
		} else {
			for (BreakPoint bp : l) {
				BreakPoint _bp = new BreakPoint();
				_bp.leftArc = new Arc(bp.leftArc.p);
				_bp.rightArc = new Arc(bp.rightArc.p);
				r.l.add(_bp);
			}
		}
		
		return r;
	}
	
	public class Arc {
		
		Point p;
		Event circleEvent;
		
		public Arc(Point p) {
			this.p = p;
		}
		
		public Arc copy() {
			Arc a = new Arc(p);
			return a;
		}
		
		// Constants for parabola with focus p=(p.x, p.y) and directrix l : y=ly
		public double y(double x, double ly) {
			return (x*x - 2*p.x*x + p.x*p.x + p.y*p.y - ly*ly) / (2 * (p.y - ly));
		}
	}
	
	public class BreakPoint {
		Arc leftArc;
		Arc rightArc;
		HalfEdge tracedEdge;
		
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

		@Override
		public String toString() {
			return "<" + leftArc.p + ", " + rightArc.p + ">";
		}
	}
}