/*
* A wrapper for Path2D.Double
*/
package election.sim;

import java.awt.geom.*;
import java.util.*;
import java.io.*;

public class Polygon2D {
    public Polygon2D() {
        points = new ArrayList<Point2D>();
    }

    public boolean append(double x, double y) {
        return append(new Point2D.Double(x, y));
    }

    public boolean append(Point2D point) {
        // If self-intersecting, return false
        if (points.size() > 1) {
            Point2D head = points.get(0);
            Point2D last = points.get(points.size() - 1);
            Line2D newLine = new Line2D.Double(point, last);
            Line2D headLine = new Line2D.Double(point, head);
            for (int i = 0; i < points.size() - 2; ++i) {
                Line2D curLine = new Line2D.Double(points.get(i), points.get(i + 1));
                if (newLine.intersectsLine(curLine) || headLine.intersectsLine(curLine))
                    return false;
            }
        }
        points.add((Point2D)point.clone());
        return true;
    }

    public List<Point2D> getPoints() {
        return points;
    }

    public int size() {
        return points.size();
    }

    public double area() {
        double answer = crossProduct(points.get(points.size() - 1), points.get(0));
        for (int i = 0; i < points.size() - 1; ++ i)
            answer += crossProduct(points.get(i), points.get(i + 1));
        return Math.abs(answer);
    }

    public boolean contains(Point2D point) {
        Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
        if (closeLine.contains(point))
            return true;
        for (int i = 0; i < points.size() - 1; ++ i) {
            Line2D line = new Line2D.Double(points.get(i), points.get(i + 1));
            if (line.contains(point))
                return true;
        }
        Random random = new Random();
        double theta = random.nextDouble() * Math.PI;
        double r = 10000.0;
        Point2D far = new Point2D.Double(r * Math.cos(theta), r * Math.sin(theta));
        int numIntersections = countIntersections(new Line2D.Double(point, far));
        return numIntersections % 2 == 1;
    }

    public boolean strictlyContinas(Point2D point) {
        Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
        if (closeLine.contains(point))
            return false;
        for (int i = 0; i < points.size() - 1; ++ i) {
            Line2D line = new Line2D.Double(points.get(i), points.get(i + 1));
            if (line.contains(point))
                return false;
        }
        Random random = new Random();
        double theta = random.nextDouble() * Math.PI;
        double r = 10000.0;
        Point2D far = new Point2D.Double(r * Math.cos(theta), r * Math.sin(theta));
        int numIntersections = countIntersections(new Line2D.Double(point, far));
        return numIntersections % 2 == 1;
    }

    public boolean intersectsLine(Line2D line) {
        if (line.intersectsLine(new Line2D.Double(points.get(points.size() - 1), points.get(0))))
            return true;
        for (int i = 0; i < points.size() - 1; ++ i) {
            if (line.intersectsLine(new Line2D.Double(points.get(i), points.get(i + 1))))
                return true;
        }
        return false;
    }

    public boolean overlap(Polygon2D polygon) {
        Area area = toArea();
        area.intersect(polygon.toArea());
        Rectangle2D rec = area.getBounds2D();
        double tmp = rec.getHeight() * rec.getWidth();
        // return Math.abs(tmp) > 1e-7;
        // TODO: will implement a sweep line algorithm
        return false;
    }

    public boolean intersect(Polygon2D polygon) {
        for (Point2D point : points) {
            if (polygon.contains(point))
                return true;
        }
        if (polygon.intersectsLine(new Line2D.Double(points.get(points.size() - 1), points.get(0))))
            return true;
        for (int i = 0; i < points.size() - 1; ++ i) {
            if (polygon.intersectsLine(new Line2D.Double(points.get(i), points.get(i + 1))))
                return true;
        }
        return false;
    }

    public Area toArea() {
        Path2D path = new Path2D.Double();
        path.moveTo(points.get(0).getX(), points.get(0).getY());
        for (int i = 1; i < points.size(); ++ i)
            path.lineTo(points.get(i).getX(), points.get(i).getY());
        path.closePath();
        return new Area(path);
    }

    public String toString() {
        String ret = Integer.toString(points.size());
        for (Point2D point : points)
            ret += " " + point.getX() + " " + point.getY();
        return ret;
    }

    private int countIntersections(Line2D line) {
        Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
        int result = line.intersectsLine(closeLine) ? 1 : 0;
        for (int i = 0; i < points.size() - 1; ++ i) {
            result += line.intersectsLine(new Line2D.Double(points.get(i), points.get(i + 1))) ? 1 : 0;
        }
        return result;
    }

    private double crossProduct(Point2D a, Point2D b) {
        double x1 = a.getX(), y1 = a.getY();
        double x2 = b.getX(), y2 = b.getY();
        return x1 * y2 - x2 * y1;
    }

    private List<Point2D> points;
}