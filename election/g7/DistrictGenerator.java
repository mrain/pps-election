package election.g7;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        double hstep = scale / 9.0;
        if (repPerDistrict == 3) {
            // 81 Districts;
            for (int i = 0; i < 9; ++ i) {
                double top = height * (9 - i) / 9.0;
                double btm = top - height / 9.0;
                double left = scale / 2 - hstep / 2 * (i + 1);
                for (int j = 0; j <= i; ++ j) {
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(left + hstep * j, btm);
                    polygon.append(left + hstep * j + hstep, btm);
                    polygon.append(left + hstep * j + hstep / 2, top);
                    result.add(polygon);
                }
                for (int j = 0; j < i; ++ j) {
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(left + hstep * j + hstep / 2, top);
                    polygon.append(left + hstep * j + hstep, btm);
                    polygon.append(left + hstep * j + hstep * 3 / 2, top);
                    result.add(polygon);
                }
            }
        } else {
            Point2D top = new Point2D.Double(500., height);
            double step = scale / numDistricts;
            for (int i = 0; i < numDistricts; ++ i) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(new Point2D.Double(step * i, 0.));
                polygon.append(new Point2D.Double(step * (i + 1), 0.));
                polygon.append(top);
                result.add(polygon);
            }
        }
        System.out.println(result.size());
        return result;
    }
}
