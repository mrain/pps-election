package election.g5;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class TrapezoidDistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts, avgVotersPerDistrict;

    public boolean needsMoreVoters(double numVotersInDistrict)
    {
        // We allow 2% deviation from avg
        double threshold = 0.02 * avgVotersPerDistrict;

        if (avgVotersPerDistrict - numVotersInDistrict > threshold)
            return true;
        return false;
    }

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        random = new Random(seed);
        avgVotersPerDistrict = 333333 / numDistricts;

        // 81 Districts
        if (repPerDistrict == 3) {

            // Tracks total height of current districts drawn
            double currentHeight = 0;
            // 51 @ 0.5, 52 @ 0.2
            double step = 0.5;
            double tan60 = Math.tan(Math.PI/3);

            int votersCounter = 0;

            // Greedy algorithm to draw trapezoid shaped districts from bottom up
            for (int i = 0; i < 80; i++) {
                double height = 0;
                Polygon2D polygon;
                do {
                    polygon = new Polygon2D();
                    height += step;

                    double leftBotX = currentHeight / tan60;
                    double rightBotX = 1000 - leftBotX;
                    double botY = currentHeight;
                    double leftTopX = (currentHeight + height) / tan60;
                    double rightTopX = 1000 - leftTopX;
                    double topY = currentHeight + height;

                    polygon.append(leftBotX, botY);
                    polygon.append(rightBotX, botY);
                    polygon.append(rightTopX, topY);
                    polygon.append(leftTopX, topY);

                    // System.out.println(polygon);

                } while (needsMoreVoters(Run.countInclusion(voters, polygon)) &&
                         currentHeight + height < 999);

                result.add(polygon);
                currentHeight += height;

                int votersAllocated = Run.countInclusion(voters, polygon);
                votersCounter += votersAllocated;

                System.out.println(result.size());
                System.out.println(currentHeight);
                System.out.println(votersAllocated);
                System.out.println(votersCounter);
            }

            // Last district is a triangle
            Polygon2D lastPolygon = new Polygon2D();
            double lastLeftBotX = currentHeight / tan60;
            double lastRightBotX = 1000 - lastLeftBotX;
            double lastBotY = currentHeight;
            double lastTopX = 500;
            double lastTopY = Math.sqrt(1000*1000 - 500*500);
            lastPolygon.append(lastLeftBotX, lastBotY);
            lastPolygon.append(lastRightBotX, lastBotY);
            lastPolygon.append(lastTopX, lastTopY);
            result.add(lastPolygon);

            System.out.println(result.size());
            System.out.println(currentHeight);
            System.out.println(votersCounter);
        }

        return result;
    }
}
