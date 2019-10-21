package election;

import election.Polygon2D;
import java.awt.geom.*;
import math.geom2d.polygon.*;

public class Subtraction {

    public static election.Polygon2D subtract(election.Polygon2D p1, election.Polygon2D p2) {
        math.geom2d.polygon.Polygon2D sp1 = new SimplePolygon2D();
        math.geom2d.polygon.Polygon2D sp2 = new SimplePolygon2D();
//        System.out.println("p1 length: " + p1.getPoints().size());
        for (Point2D p : p1.getPoints()) {
            sp1.addVertex(new math.geom2d.Point2D(p.getX(), p.getY()));
        }
//        System.out.println("p2 length: " + p1.getPoints().size());
        for (Point2D p : p2.getPoints()) {
            sp2.addVertex(new math.geom2d.Point2D(p.getX(), p.getY()));
        }
        math.geom2d.polygon.Polygon2D sresult = Polygons2D.difference(sp1, sp2);
        Polygon2D result = new election.Polygon2D();
        for (math.geom2d.Point2D p : sresult.vertices()) {
//            System.out.println("Subtraction: x: " + p.x() + " y: " + p.y());
            result.append(new Point2D.Double(p.x(), p.y()));
        }
        return result;
    }

}