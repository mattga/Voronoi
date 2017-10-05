package edu.isu.mattga;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
