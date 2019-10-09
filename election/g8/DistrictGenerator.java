package election.g8;

import java.awt.*;
import java.util.*;
import election.sim.*;
import java.awt.geom.*;
import java.util.List;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;

    public String constructLineKey(Point2D p1, Point2D p2) {
        // Takes line segment and constructs String key for comparison
        // Order invariant: treats (a, b) the same as (b, a)
        String p1k = Math.round(p1.getX()) + "," + Math.round(p1.getY());
        String p2k = Math.round(p2.getX()) + "," +  Math.round(p2.getY());
        boolean p1Bigger = p1k.hashCode() > p2k.hashCode();
        String biggerKey = p1Bigger ?  p1k : p2k;
        String smallerKey = p1Bigger ? p2k : p1k;
        return smallerKey + "-" + biggerKey;
    }

    public boolean districtContainsEdge(Polygon2D district, Point2D p1, Point2D p2) {
        boolean containsP1 = false;
        boolean containsP2 = false;

        String p1_key = p1.getX() + "_" + p1.getY();
        String p2_key = p2.getX() + "_" + p2.getY();

        for(Point2D point : district.getPoints()) {
            String pKey = point.getX() + "_" + point.getY();
            if(pKey == p1_key) {
                containsP1 = true;
            } else if (pKey == p2_key) {
                containsP2 = true;
            }
        }

        return containsP1 && containsP2;
    }

    public ArrayList<Point2D> insertInto(List<Point2D>a, List<Point2D>b, Integer spliceIdx) {
        ArrayList<Point2D>expandedArray = new ArrayList<Point2D>();

        for(int i = 0; i < a.size(); i++) {
            expandedArray.add(a.get(i));
            if(i == spliceIdx) {
                for (Point2D p: b) {
                    expandedArray.add(p);
                }
            }
        }

        return expandedArray;
    }

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        repPerDistrict = 3; // TODO this is hardcoded now
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        double hstep = scale / 9.0;

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

        System.out.println(result.size());

        sampleDistricts(result);

        return result;
    }

    public HashMap<String, ArrayList<Integer>>getDistrictsByBorder(List<Polygon2D> districts) {
        HashMap<String, ArrayList<Integer>>edgeMap = new HashMap<String, ArrayList<Integer>>();
        for(int didx = 0; didx < districts.size(); didx++) {
            Polygon2D district = districts.get(didx);
            for(int i = 0; i < district.getPoints().size(); i++) {
                Point2D startPoint = district.getPoints().get(i);
                int nextIdx = i + 1;
                if(i + 1 >= district.getPoints().size())  {
                    nextIdx = 0;
                }
                Point2D endPoint = district.getPoints().get(nextIdx);
                String key = constructLineKey(startPoint, endPoint);
                if(edgeMap.containsKey(key)) {
                    edgeMap.get(key).add(didx);
                } else {
                    ArrayList<Integer>l = new ArrayList<>();
                    l.add(didx);
                    edgeMap.put(key, l);
                }
            }
        }

        return edgeMap;
    }

    public void sampleDistricts(List<Polygon2D> initialDistricts) {
        double probSwitch = 0.1;
        double maxChunkArea = 100;
        double targetSides = Math.sqrt(maxChunkArea * 4.0 / Math.sqrt(3.0));
        double targetTriangleHeight = targetSides * Math.sqrt(3.0) / 2.0;

        // a border is a line shared by >1 district (will have value of 2 in this dictionary)
        // other lines border the edge of the country and aren't flippable.
        // districtsByBorder['3,2-4,5'] = [0, 2] means the 0th and 2nd district contain the line (3, 2) - (4, 5)
        HashMap<String, ArrayList<Integer>>districtsByBorder = getDistrictsByBorder(initialDistricts);

        for(int didx=0; didx < initialDistricts.size(); didx++) {
            Polygon2D district = initialDistricts.get(didx);
            if(didx > 0) { // Math.random() > probSwitch || district.getPoints().size() > 6) {
                continue;
            }

            // Get all polygon line segments (2 points) with a district border
            ArrayList<Integer>possiblePoints = new ArrayList<>();
            for(int i = 0; i < district.getPoints().size(); i++) {
                Point2D startPoint = district.getPoints().get(i);
                int nextIdx = i + 1;
                if(i + 1 >= district.getPoints().size())  {
                    nextIdx = 0;
                }
                Point2D endPoint = district.getPoints().get(nextIdx);
                String key = constructLineKey(startPoint, endPoint);
                if (districtsByBorder.get(key).size() > 1) {
                    possiblePoints.add(i);
                }
            }

            // Pick a random border line
            int startIdx = possiblePoints.get(new Random().nextInt(possiblePoints.size()));
            int endIdx = (startIdx + 1 >= district.getPoints().size()) ? 0: startIdx + 1;
            Point2D startPoint = district.getPoints().get(startIdx);
            Point2D endPoint = district.getPoints().get(endIdx);
            Point2D midPoint = new Point2D.Double((startPoint.getX() + endPoint.getX()) / 2.0,
                    (startPoint.getY() + endPoint.getY()) / 2.0);
            Line2D edge = new Line2D.Double(startPoint, endPoint);
            double edgeLength = Math.sqrt(Math.pow(edge.getX2() - edge.getX1(), 2) + Math.pow(edge.getY2() - edge.getY1(), 2));
            double percentageDisruptLine = Math.min(targetSides / edgeLength, 1.0);
            double percentageDown = 0.5 - percentageDisruptLine / 2.0;
            double percentageUp = 0.5 + percentageDisruptLine / 2.0;

//            if(Math.random() <= 0.5) { //  TODO - test this out
//                targetAngle += Math.PI;
//                if (targetAngle > 2 * Math.PI) {
//                    targetAngle -= 2 * Math.PI;
//                }
//            }

            // look up neighboring district to the randomly selected border
            ArrayList<Integer> districtsWhichShareBorder = districtsByBorder.get(constructLineKey(startPoint, endPoint));
            int adjacentDistrictIdx = (districtsWhichShareBorder.get(0) == didx)
                    ? districtsWhichShareBorder.get(1)
                    : districtsWhichShareBorder.get(0);
            Polygon2D adjacentDistrict = initialDistricts.get(adjacentDistrictIdx);

            // If we have 7+ edges, we can't add another equilateral triangle without first merging edges
            if(adjacentDistrict.getPoints().size() > 6) {
                continue;
            }

            // Pick bases of equilateral triangle based on equivalent distance from center of border
            Point2D newPointCloserToStart = new Point2D.Double(percentageUp * startPoint.getX() + (1.0 - percentageUp) * endPoint.getX(),
                    percentageUp * startPoint.getY() + (1.0 - percentageUp) * endPoint.getY());
            Point2D newPointCloserToEnd = new Point2D.Double(percentageDown * startPoint.getX() + (1.0 - percentageDown) * endPoint.getX(),
                    percentageDown * startPoint.getY() + (1.0 - percentageDown) * endPoint.getY());

            // Where do we need to put the triangle top to make it a projection from its edge (90 degrees)
            double angle = Math.atan((newPointCloserToEnd.getY() - newPointCloserToStart.getY()) /
                    (newPointCloserToEnd.getX() - newPointCloserToStart.getX()));
            double targetAngle = angle + Math.PI / 2.0;
            double deltaX = Math.cos(targetAngle) * targetTriangleHeight;
            double deltaY = Math.sin(targetAngle) * targetTriangleHeight;
            Point2D triangleTop = new Point2D.Double(midPoint.getX() + deltaX, midPoint.getY() + deltaY);

            // Insert these new points into the polygon in the right index (startIdx)
            ArrayList<Point2D> insertPoints = new ArrayList<>();
            insertPoints.add(newPointCloserToStart);
            insertPoints.add(triangleTop);
            insertPoints.add(newPointCloserToEnd);
            ArrayList<Point2D>newPoints = insertInto(district.getPoints(), insertPoints, startIdx);
            Polygon2D newPolygon = new election.sim.Polygon2D();
            for (Point2D p : newPoints) {
                newPolygon.append(p);
            }
            initialDistricts.set(didx, newPolygon);

            // Find out where to insert this new triangle into the adjacent district
            int adjacentEndIdx = 9999;
            int adjacentStartIdx = -1;
            for(int z = 0; z < adjacentDistrict.getPoints().size(); z++) {
                if(adjacentDistrict.getPoints().get(z) == endPoint) {
                    adjacentEndIdx = z;
                } else {
                    adjacentStartIdx = z;
                }
            }
            int adjacentSpliceIdx = -1;
            if(adjacentEndIdx == 0 && adjacentStartIdx == adjacentDistrict.getPoints().size() - 1) {
                adjacentSpliceIdx = adjacentDistrict.getPoints().size() - 1;
            } else if(adjacentStartIdx == 0 && adjacentEndIdx == adjacentDistrict.getPoints().size() - 1) {
                adjacentSpliceIdx = adjacentDistrict.getPoints().size() - 1;
            } else {
                adjacentSpliceIdx = Math.min(adjacentStartIdx, adjacentEndIdx);
            }

            // Decide which order is proper (depends which triangle base point is closer to adjacentSpliceIdx)
            ArrayList<Point2D> adjacentInsertPoints = new ArrayList<>();
            if(adjacentSpliceIdx == adjacentEndIdx) {
                adjacentInsertPoints.add(newPointCloserToEnd);
                adjacentInsertPoints.add(triangleTop);
                adjacentInsertPoints.add(newPointCloserToStart);
            } else {
                adjacentInsertPoints.add(newPointCloserToStart);
                adjacentInsertPoints.add(triangleTop);
                adjacentInsertPoints.add(newPointCloserToEnd);
            }

            // Add these new points to our counts map so that they get recognized as border points (count = 2)
            String newEdgeKey1 = constructLineKey(newPointCloserToStart, triangleTop);
            String newEdgeKey2 = constructLineKey(newPointCloserToEnd, triangleTop);
            ArrayList<Integer>sharedBorderDistrictIdxs = new ArrayList<>();
            sharedBorderDistrictIdxs.add(didx);
            sharedBorderDistrictIdxs.add(adjacentDistrictIdx);
            districtsByBorder.put(newEdgeKey1, sharedBorderDistrictIdxs);
            districtsByBorder.put(newEdgeKey2, sharedBorderDistrictIdxs);

            ArrayList<Point2D>adjacentNewPoints = insertInto(adjacentDistrict.getPoints(), adjacentInsertPoints,
                    adjacentSpliceIdx);
            Polygon2D adjacentNewPolygon = new election.sim.Polygon2D();
            for (Point2D p : adjacentNewPoints) {
                adjacentNewPolygon.append(p);
            }
            initialDistricts.set(adjacentDistrictIdx, adjacentNewPolygon);

            System.out.println("Exchanging Territory between district " + didx + ", and " + adjacentDistrictIdx);
        }
    }
}
