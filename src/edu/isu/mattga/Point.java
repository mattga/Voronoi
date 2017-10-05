package edu.isu.mattga;


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