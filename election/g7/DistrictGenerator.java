package election.g7;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private double eps = 1E-7;
    private List<Map<Polygon2D, List<Voter>>> polyganList = new ArrayList<>();
    private Map<Integer, Polygon2D> polygonMap = new HashMap<>();
    private Map<Integer, List<Voter>> voterMap = new HashMap<>();
    private Map<Integer, Boolean> checkMap = new HashMap<>();
    private int partyToWin = 1;

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
        int index = 0;
        for (Map<Polygon2D, List<Voter>> map : polyganList) {
            for (Map.Entry<Polygon2D, List<Voter>> entry: map.entrySet()){
                polygonMap.put(index, entry.getKey());
                voterMap.put(index++, entry.getValue());
                checkMap.put(index, false);
            }
        }

        for (Map.Entry<Integer, Polygon2D> entry : polygonMap.entrySet()) {
            int id = entry.getKey();
            if (!checkMap.get(id) && !isSwingState(id)) {
                Map<Integer, double[]> adjacentDistricts = getAdjacentDistricts(id);
                Polygon2D swing = entry.getValue();
                for (Map.Entry<Integer, double[]> adjacentDistrict : adjacentDistricts.entrySet()) {
                    int otherId = adjacentDistrict.getKey();
                    double[] edge = adjacentDistrict.getValue();
                    double x1 = edge[0], y1 = edge[1], x2 = edge[2], y2 = edge[3];
                    double len = Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
                    double width = getWidth(len);
                    List<Point2D> swingPoints = swing.getPoints();
                    List<Point2D> adjacentPoints = polygonMap.get(otherId).getPoints();
                    //vertical line
                    if (isEqual(x1, x2)) {
                        double start = Math.max(y1, y2);
                        double end = Math.min(y1, y2);
                        for (double i = start; i - end >= width; i -= width) {
                            Polygon2D concaveSwing = new Polygon2D();
                            Polygon2D convexAdjacent = new Polygon2D();
                            Polygon2D concaveAdjacent = new Polygon2D();
                            Polygon2D convexSwing = new Polygon2D();
                            buildPolygonByY(swingPoints, concaveSwing, convexSwing, x1, i, width);
                            buildPolygonByY(adjacentPoints, concaveAdjacent, convexAdjacent, x1, i, width);
                            if (setNewPolygon(id, otherId, convexSwing, concaveAdjacent, concaveSwing, convexAdjacent))
                                break;
                        }
                    }
                    //horizontal line
                    else if (isEqual(y1, y2)) {
                        double start = Math.min(x1, x2);
                        double end = Math.max(x1, x2);
                        for (double i = start; end - i >= width; i += width) {
                            Polygon2D concaveSwing = new Polygon2D();
                            Polygon2D convexAdjacent = new Polygon2D();
                            Polygon2D concaveAdjacent = new Polygon2D();
                            Polygon2D convexSwing = new Polygon2D();
                            buildPolygonByX(swingPoints, concaveSwing, convexSwing, y1, i, width);
                            buildPolygonByX(adjacentPoints, concaveAdjacent, convexAdjacent, y1, i, width);
                            if (setNewPolygon(id, otherId, convexSwing, concaveAdjacent, concaveSwing, convexAdjacent))
                                break;
                        }
                    }
                    else
                        System.out.println("Adjacent edge is not vertical or horizontal!");
                }
            }
        }

        result = new ArrayList<Polygon2D>(polygonMap.values());
        return result;
    }

        private double getWidth(double len) {
            return 0.0;
        }

        //Return the index of the matrix with the a double array representting the vertex for the edge.
        // [0] x1, [1] y1, [2] x2, [3] y2
        private Map<Integer, double[]> getAdjacentDistricts(int id) {
            Map<Integer, double[]> list = new HashMap<>();
            return list;
        }

        // partyToWin is global variable now
        private boolean isSwingState(int id) {
            return false;
        }

        //Check population is valid for two polygon2 and if how beneficial it is for digging.
        private boolean isValidGerrymander(int swingId, int otherId, Polygon2D swing, Polygon2D other) {
            return false;
        }

        //Vertical adjacent edge.
        private void buildPolygonByY(List<Point2D> point2Ds, Polygon2D concave, Polygon2D convex, double x1, double y1,
                                  double width) {
            for (int j = 0; j < point2Ds.size(); j++) {
                Point2D point2D = point2Ds.get(j);
                concave.append(point2D);
                convex.append(point2D);
                // Build the triangle when bulding the overlapping edge.
                if (isEqual(point2D.getX(), x1) && j < point2Ds.size() - 1 &&
                        isEqual(point2Ds.get(j + 1).getX(), x1)) {
                    double y3 = 0, y4 = 0;
                    if (point2Ds.get(j + 1).getY() > point2D.getY()) {
                        y3 = y1 - width;
                        y4 = y1;
                    }
                    else {
                        y3 = y1;
                        y4 = y1 - width;
                    }
                    concave.append(x1, y3);
                    concave.append(x1 - Math.sqrt(3)/2*width, (y4 + y3)/2);
                    concave.append(x1, y4);
                    convex.append(x1, y3);
                    convex.append(x1 + Math.sqrt(3)/2*width, (y4 + y3)/2);
                    convex.append(x1, y4);
                }
            }
        }

        //Horizontal adjacent edge.
        private void buildPolygonByX(List<Point2D> point2Ds, Polygon2D concave, Polygon2D convex, double y1, double x1,
                                     double width) {
            for (int j = 0; j < point2Ds.size(); j++) {
                Point2D point2D = point2Ds.get(j);
                concave.append(point2D);
                convex.append(point2D);
                // Build the triangle when bulding the overlapping edge.
                if (isEqual(point2D.getY(), y1) && j < point2Ds.size() - 1 &&
                        isEqual(point2Ds.get(j + 1).getY(), y1)) {
                    double x3 = 0, x4 = 0;
                    if (point2Ds.get(j + 1).getX() > point2D.getX()) {
                        x3 = x1;
                        x4 = x1 + width;
                    }
                    else {
                        x3 = x1 + width;
                        x4 = x1;
                    }
                    concave.append(x3, y1);
                    concave.append((x3 + x4)/2, y1 + Math.sqrt(3)/2*width);
                    concave.append(x4, y1);
                    convex.append(x3, y1);
                    convex.append((x3 + x4)/2, y1 - Math.sqrt(3)/2*width);
                    convex.append(x4, y1);
                }
            }
        }

        private boolean setNewPolygon(int id, int otherId, Polygon2D convexSwing, Polygon2D concaveAdjacent,
                                      Polygon2D concaveSwing, Polygon2D convexAdjacent) {
            if (isValidGerrymander(id, otherId, convexSwing, concaveAdjacent)) {
                checkMap.put(id, true);
                checkMap.put(otherId, true);
                polygonMap.put(id, convexSwing);
                polygonMap.put(otherId, concaveAdjacent);
                return true;
            }
            if (isValidGerrymander(id, otherId, concaveSwing, convexAdjacent)) {
                checkMap.put(id, true);
                checkMap.put(otherId, true);
                polygonMap.put(id, concaveSwing);
                polygonMap.put(otherId, convexAdjacent);
                return true;
            }
            return false;
        }

        private boolean isEqual(double a, double b) {
            return Math.abs(a - b) <= eps;
        }

}
