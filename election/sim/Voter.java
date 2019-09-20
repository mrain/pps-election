package election.sim;

import java.awt.geom.Point2D;
import java.util.*;

public class Voter {
    public Voter(Point2D location, List<Double> preference) {
        this.location = location;
        this.preference = preference;
    }

    public Point2D getLocation() {
        return location;
    }

    public List<Double> getPreference() {
        return preference;
    }

    public String toString() {
        String ret = location.getX() + " " + location.getY();
        for (Double d : preference)
            ret += " " + d;
        return ret;
    }

    private Point2D location;
    private List<Double> preference;
}