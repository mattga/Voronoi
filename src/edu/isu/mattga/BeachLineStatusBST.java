package edu.isu.mattga;

import edu.isu.mattga.DCEL.HalfEdge;
import edu.isu.mattga.VoronoiDiagram.Event;


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
