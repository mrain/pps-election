package election.g7;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private double eps = 1E-7;

    public List<Voter> sortByXCoordinate(List<Voter>voters){
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return Double.compare(v1.getLocation().getX(), v2.getLocation().getX());
            }
        });
        return voters;
    }

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        int numStripes = 81;
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
        int from = 0;
        double btm = 500*Math.sqrt(3);
        for (int i = 0; i < numStripes; i++) {
            int to = blockEachStripe*peopleInBlock*(i + 1) - 1;
            if (i == numStripes - 1) {
                blockEachStripe = numDistricts - blockEachStripe * (numStripes - 1);
                to = numVoters - 1;
            }
            while (to + 1 < numVoters && voters.get(to) == voters.get(to + 1))
                to++;
            List<Voter> voter_by_y = voters.subList(from, to + 1);
            from = to + 1;
            double top = btm;
            btm = (i == numStripes - 1) ? 0 : voter_by_y.get(voter_by_y.size() - 1).getLocation().getY() - eps;
            double preX = btm / Math.sqrt(3);
            double btmWidth = 1000 - 2*preX;
            if (i == 0) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(500., 500*Math.sqrt(3));
                polygon.append(preX, btm);
                polygon.append(btmWidth + preX, btm);
                result.add(polygon);
            }
            else {
                double preX1 = top / Math.sqrt(3);
                double topWidth = 1000 - 2*preX1;
                Polygon2D polygon = new Polygon2D();
                polygon.append(preX1, top);
                polygon.append(topWidth + preX1, top);
                polygon.append(btmWidth + preX, btm);
                polygon.append(preX, btm);
                result.add(polygon);
            }
        }

            System.out.println(result.size());
            return result;
        }
}

        /*            //sort voters by x-coordinate
            List<Voter> voter_by_x = sortByXCoordinate(voter_by_y);
            List<Double> x_coordinates = new ArrayList<>();
            //draw vertical lines in each stripe area
            double top = btm;
            btm = Math.max(voter_by_x.get(voter_by_x.size() - 1).getLocation().getY() - eps, 0);
            double preX = btm / Math.sqrt(3);
            for (int j = 1; j <= blockEachStripe; j++) {
                if (j == 1) {
                    double x = voter_by_x.get(peopleInBlock*(j)).getLocation().getX() + eps;
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(preX, btm);
                    polygon.append(x, btm);
                    polygon.append(x, (x - preX)*Math.sqrt(3) + btm);
                    result.add(polygon);
                    if (j == blockEachStripe) {

                    }
                    preX += x;
                    preY +=
                }
                if (j == blockEachStripe) {
                    double x = voter_by_x.get(peopleInBlock*(j)).getLocation().getX() + eps;
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(preX, btm);
                    polygon.append(x, btm);
                    polygon.append(x, (x - preX)*Math.sqrt(3) + btm);
                    result.add(polygon);
                }*/