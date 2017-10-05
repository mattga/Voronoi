package edu.isu.mattga;

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
