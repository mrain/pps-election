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
                if ((i < points.size() - 2 && newLine.intersectsLine(curLine))
                    || (i > 0 && headLine.intersectsLine(curLine)))
                    return false;
                }
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

    public boolean contains(Polygon2D polygon) {
        for (Point2D p : polygon.getPoints())
            if (!contains(p)) {
//                System.err.println(p.getX() + " " + p.getY() + " " + this.toString());
//                Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
//                System.err.println(ptSegDist(p, points.get(points.size() - 1), points.get(0)));
//                for (int i = 0; i < points.size() - 1; ++ i) {
//                    Line2D line = new Line2D.Double(points.get(i), points.get(i + 1));
//                    System.err.println(ptSegDist(p, points.get(i), points.get(i + 1)));
//                }
                return false;
            }
        return true;
    }

    public boolean contains(Point2D point) {
//        Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
//        if (closeLine.ptSegDist(point) < 1e-8)
//            return true;
        for (int i = 0; i < points.size(); ++ i) {
//            Line2D line = new Line2D.Double(points.get(i), points.get(i + 1));
            if (ptSegDist(point, points.get(i), points.get((i + 1) % points.size())) < 1e-8)
                return true;
        }
        Random random = new Random();
        double theta = random.nextDouble() * Math.PI;
        double r = 5135.0;
        Point2D far = new Point2D.Double(r * Math.cos(theta), r * Math.sin(theta));
        int numIntersections = countIntersections(new Line2D.Double(point, far));
        return numIntersections % 2 == 1;
    }

    public boolean strictlyContains(Point2D point) {
        for (int i = 0; i < points.size(); ++ i) {
//            Line2D line = new Line2D.Double(points.get(i), points.get(i + 1));
            if (ptSegDist(point, points.get(i), points.get((i + 1) % points.size())) < 1e-8)
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

    private Point2D flutterTowards(Point2D a, Point2D b) {
        return add(a, multiply(toUnit(subtract(b, a)), 1e-4));
    }

    public boolean overlap(Polygon2D polygon) {
        for (Point2D p : points)
            if (polygon.strictlyContains(p))
                return true;
        for (Point2D p : polygon.getPoints())
            if (strictlyContains(p))
                return true;
        for (int i = 0; i < points.size(); ++ i) {
            for (int j = 0; j < polygon.getPoints().size(); ++ j) {
                Point2D u1 = points.get(i), u2 = points.get((i + 1) % points.size());
                Point2D v1 = polygon.getPoints().get(j), v2 = polygon.getPoints().get((j + 1) % polygon.getPoints().size());
                Line2D l1 = new Line2D.Double(u1, u2), l2 = new Line2D.Double(v1, v2);
                if (!parallel(l1, l2) && l1.intersectsLine(l2)) {
                    Point2D x = lineIntersection(l1, l2);
                    if (strictlyContains(flutterTowards(x, v1)))
                        return true;
                    if (strictlyContains(flutterTowards(x, v2)))
                        return true;
                    if (polygon.strictlyContains(flutterTowards(x, u1)))
                        return true;
                    if (polygon.strictlyContains(flutterTowards(x, u2)))
                        return true;
                }
            }
        }
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

    public String toString() {
        String ret = Integer.toString(points.size());
        for (Point2D point : points)
            ret += " " + point.getX() + " " + point.getY();
        return ret;
    }

    public boolean parallel(Line2D l1, Line2D l2) {
        double x1 = l1.getX1(), y1 = l1.getY1(), x2 = l1.getX2(), y2 = l1.getY2();
        double x3 = l2.getX1(), y3 = l2.getY1(), x4 = l2.getX2(), y4 = l2.getY2();
        return Math.abs(cross(x2 - x1, y2 - y1, x4 - x3, y4 - y3)) < 1e-8;
    }

    public Point2D lineIntersection(Line2D l1, Line2D l2) {
        if (!l1.intersectsLine(l2)) return null;
        double x1 = l1.getX1(), y1 = l1.getY1(), x2 = l1.getX2(), y2 = l1.getY2();
        double x3 = l2.getX1(), y3 = l2.getY1(), x4 = l2.getX2(), y4 = l2.getY2();
        double t = cross(x1 - x3, y1 - y3, x3 - x4, y3 - y4) / cross(x1 - x2, y1 - y2, x3 - x4, y3 - y4);
        return add(l1.getP1(), multiply(subtract(l1.getP2(), l1.getP1()), t));
    }

    public Point2D toUnit(Point2D a) {
        double l = length(a.getX(), a.getY());
        if (l < 1e-8) l = 1;
        return new Point2D.Double(a.getX() / l, a.getY() / l);
    }

    public Point2D add(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public Point2D subtract(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public Point2D multiply(Point2D a, double b) {
        return new Point2D.Double(a.getX() * b, a.getY() * b);
    }

    public double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public double cross(double x1, double y1, double x2, double y2) {
        return x1 * y2 - x2 * y1;
    }

    public double crossProduct(Point2D a, Point2D b) {
        double x1 = a.getX(), y1 = a.getY();
        double x2 = b.getX(), y2 = b.getY();
        return x1 * y2 - x2 * y1;
    }

    public int countIntersections(Line2D line) {
        Line2D closeLine = new Line2D.Double(points.get(points.size() - 1), points.get(0));
        int result = line.intersectsLine(closeLine) ? 1 : 0;
        for (int i = 0; i < points.size() - 1; ++ i) {
            result += line.intersectsLine(new Line2D.Double(points.get(i), points.get(i + 1))) ? 1 : 0;
        }
        return result;
    }

    public int sign(double a) {
        return a > 1e-8 ? 1 : a < -1e-8 ? -1 : 0;
    }

    public double ptDist(Point2D a, Point2D b) {
        return length(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public double dot(double x1, double y1, double x2, double y2) {
        return x1 * x2 + y1 * y2;
    }

    public double dot(Point2D a, Point2D b) {
        return dot(a.getX(), a.getY(), b.getX(), b.getY());
    }

    public double ptSegDist(Point2D p, Point2D u, Point2D v) {
//        System.err.println(u.getX() + " " + u.getY() + " " + v.getX() + " " + v.getY());
        Point2D dir = toUnit(subtract(v, u));
        Point2D x = add(u, multiply(dir, dot(dir, subtract(p, u))));
//        System.err.println(x.getX() + " " + x.getY() + " " );
        if (sign(crossProduct(subtract(v, p), subtract(x, p))) * sign(crossProduct(subtract(x, p), subtract(u, p))) == -1) {
            return Math.min(ptDist(u, x), ptDist(v, x));
        } else return ptDist(p, x);
    }

    private List<Point2D> points;
}
