package election.g0;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class Player extends election.sim.Player {
    private Random random;
    private int numVoters, numParties, numDistricts;
    public void init(int numVoters, int numParties, int numDistricts, long seed) {
        this.numVoters = numVoters;
        this.numParties = numParties;
        this.numDistricts = numDistricts;
        random = new Random(seed);
    }

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters) {
        Point2D top = new Point2D.Double(500., 500. * Math.sqrt(3));
        double step = 1000.0 / numDistricts;
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        for (int i = 0; i < numDistricts; ++ i) {
            Polygon2D polygon = new Polygon2D();
            polygon.append(new Point2D.Double(step * i, 0.));
            polygon.append(new Point2D.Double(step * (i + 1), 0.));
            polygon.append(top);
            result.add(polygon);
        }
        System.out.println(result.size());
        return result;
    }
}