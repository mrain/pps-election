package election.g3;

/******************************************************************************
 *  Compilation:  javac Voronoi.java
 *  Execution:    java Voronoi
 *  Dependencies: Draw.java DrawListener.java
 * 
 *  Plots the points that the user clicks, and draws the Voronoi diagram.
 *  We assume the points lie on an M-by-M grid and use a brute force
 *  discretized algorithm. Each insertion takes time proportional to M^2.
 *
 *  Limitations
 *  -----------
 *    - Running time scales (badly) with M
 *    - Fortune's algorithm can compute a Voronoi diagram on N 
 *      points in time proportional to N log N, but it is 
 *      subtantially more complicated than this program which is intended
 *      to demonstrate callbacks and GUI operations.
 *
 ******************************************************************************/

import java.awt.Color;
import java.awt.Point;

public class VoronoiDemo implements DrawListener {
    private static int SIZE = 512;
    private Point[][] nearest = new Point[SIZE][SIZE];  // which point is pixel (i, j) nearest?

    private Draw draw = new Draw();


    public VoronoiDemo() {
        draw.setCanvasSize(SIZE, SIZE);
        draw.setXscale(0, SIZE);
        draw.setYscale(0, SIZE);
        draw.addListener(this);
        draw.show(20);
    }

    public void mousePressed(double x, double y) {
        Point p = new Point((int) x, (int) y);
        System.out.println("Inserting: " + p);

        // compare each pixel (i, j) and find nearest point
        draw.setPenColor(Color.getHSBColor((float) Math.random(), .7f, .7f));
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                Point q = new Point(i, j);
                if ((nearest[i][j] == null) || (q.distance(p) < q.distance(nearest[i][j]))) {
                    nearest[i][j] = p;
                    draw.filledSquare(i + 0.5, j + 0.5, 0.5);
                }
            }
        }

        // draw the point afterwards
        draw.setPenColor(Color.BLACK);
        draw.filledCircle(x, y, 4);
        draw.show(20);
        System.out.println("Done processing: " + p);
    }


    // save the screen to a file
    public void keyTyped(char c) { draw.save("voronoi" + c + ".png"); }

    // must implement these since they're part of the interface
    public void keyPressed(int keycode)  { }
    public void keyReleased(int keycode) { }
    public void mouseDragged(double x, double y)  { }
    public void mouseReleased(double x, double y) { }


    // test client
    public static void main(String[] args) {
        new VoronoiDemo();
    }

	@Override
	public void mouseClicked(double x, double y) {}   
}