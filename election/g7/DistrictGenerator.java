package election.g7;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private double eps = 1E-7;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        int numStripes = 9;
        //Can contribute deviation
        int peopleInBlock = numVoters / numDistricts;
        int blockEachStripe =  numDistricts / numStripes;
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return Double.compare(v2.getLocation().getY(), v1.getLocation().getY());
            }
        });
        // From top to bottom
        List<List<Voter>> votersInStripe = new ArrayList<>();
        // Number of blocks in each stripe
        List<Integer> numBlocks = new ArrayList<>();
        int from = 0;
        for (int i = 0; i < numStripes; i++) {
            int to = blockEachStripe*peopleInBlock*(i + 1) - 1;
            if (i == numStripes - 1) {
                blockEachStripe = numDistricts - blockEachStripe * (numStripes - 1);
                to = numVoters - 1;
            }
            while (to + 1 < numVoters && voters.get(to) == voters.get(to + 1))
                to++;
            votersInStripe.add(voters.subList(from, to + 1));
            from = to + 1;
            numBlocks.add(blockEachStripe);
        }
//        if (repPerDistrict == 3) {
//            // 81 Districts;
//            for (int i = 0; i < 9; ++ i) {
//                double top = height * (9 - i) / 9.0;
//                double btm = top - height / 9.0;
//                double left = scale / 2 - hstep / 2 * (i + 1);
//                for (int j = 0; j <= i; ++ j) {
//                    Polygon2D polygon = new Polygon2D();
//                    polygon.append(left + hstep * j, btm);
//                    polygon.append(left + hstep * j + hstep, btm);
//                    polygon.append(left + hstep * j + hstep / 2, top);
//                    result.add(polygon);
//                }
//                for (int j = 0; j < i; ++ j) {
//                    Polygon2D polygon = new Polygon2D();
//                    polygon.append(left + hstep * j + hstep / 2, top);
//                    polygon.append(left + hstep * j + hstep, btm);
//                    polygon.append(left + hstep * j + hstep * 3 / 2, top);
//                    result.add(polygon);
//                }
//            }
//        } else {
//            Point2D top = new Point2D.Double(500., height);
//            double step = scale / numDistricts;
//            for (int i = 0; i < numDistricts; ++ i) {
//                Polygon2D polygon = new Polygon2D();
//                polygon.append(new Point2D.Double(step * i, 0.));
//                polygon.append(new Point2D.Double(step * (i + 1), 0.));
//                polygon.append(top);
//                result.add(polygon);
//            }
//        }
        System.out.println(result.size());
        return result;
    }
}
