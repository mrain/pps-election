package election.g9;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class TriangleUtil {

    /* lrcoords
     * returns a list that contains two entries, each a coordinate
     * the first coordinate is the left, the second is the right
     */
    public ArrayList<ArrayList<Double>> lrcoords (double y, double xmax){
        ArrayList<ArrayList<Double>> lrcoords = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> lcoord = new ArrayList<>();
        lcoord.add(Math.sqrt(3) * y);
        lcoord.add(y);

        ArrayList<Double> rcoord = new ArrayList<>();
        rcoord.add(xmax - (y * Math.sqrt(3)));
        rcoord.add(y);

        return lrcoords;
    }

}
