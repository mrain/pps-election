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

    public HashMap<String, Integer>calculatePointCounts(List<Polygon2D> districts) {
        HashMap<String, Integer>pointCounts = new HashMap<String, Integer>();
        for (Polygon2D district : districts) {
            for(Point2D p : district.getPoints()) {
                String k = p.getX() + "_" + p.getY();
                if (pointCounts.containsKey(k)) {
                    pointCounts.put(k, pointCounts.get(k) + 1);
                } else {
                    pointCounts.put(k, 1);
                }
            }
        }

        return pointCounts;
    }

    public void sampleDistricts(List<Polygon2D> initialDistricts) {
        double probSwitch = 0.1;
        double maxChunkArea = 100;
        double targetSides = Math.sqrt(maxChunkArea * 4.0 / Math.sqrt(3.0));
        double targetTriangleHeight = targetSides * Math.sqrt(3.0) / 2.0;

        HashMap<String, Integer>pointCounts = calculatePointCounts(initialDistricts);

        for(int didx=0; didx < initialDistricts.size(); didx++) {
            Polygon2D district = initialDistricts.get(didx);
            if(didx > 0) { // Math.random() > probSwitch || district.getPoints().size() > 6) {
                continue;
            }

            ArrayList<Integer>possiblePoints = new ArrayList<>();

            for(int i = 0; i < district.getPoints().size(); i++) {
                Point2D startPoint = district.getPoints().get(i);
                String startKey = startPoint.getX() + "_" + startPoint.getY();
                int nextIdx = i + 1;
                if(i + 1 >= district.getPoints().size())  {
                    nextIdx = 0;
                }
                Point2D endPoint = district.getPoints().get(nextIdx);
                String endKey = endPoint.getX() + "_" + endPoint.getY();

//                if(!pointCounts.containsKey(startKey) || !pointCounts.containsKey(endKey)) {
//                    System.out.println("HERE");
//                }

                if (pointCounts.get(startKey) > 1 && pointCounts.get(endKey) > 1) {
                    possiblePoints.add(i);
                }
            }

            if(possiblePoints.size() == 0) {
                continue;
            }

            int startIdx = possiblePoints.get(new Random().nextInt(possiblePoints.size()));

            int endIdx = (startIdx + 1 >= district.getPoints().size()) ? 0: startIdx + 1;

            Point2D startPoint = district.getPoints().get(startIdx);
            Point2D endPoint = district.getPoints().get(endIdx);
            Point2D midPoint = new Point2D.Double((startPoint.getX() + endPoint.getX()) / 2.0,
                    (startPoint.getY() + endPoint.getY()) / 2.0);
            Line2D edge = new Line2D.Double(startPoint, endPoint);
            double edgeLength = Math.sqrt(Math.pow(edge.getX2() - edge.getX1(), 2) + Math.pow(edge.getY2() - edge.getY1(), 2));
            double percentageDisruptLine = targetSides / edgeLength;
            double percentageDown = 0.5 - percentageDisruptLine / 2.0;
            double percentageUp = 0.5 + percentageDisruptLine / 2.0;

//            if(Math.random() <= 0.5) {
//                targetAngle += Math.PI;
//                if (targetAngle > 2 * Math.PI) {
//                    targetAngle -= 2 * Math.PI;
//                }
//            }

            Polygon2D adjacentDistrict = null;
            int adjacentDistrictIdx = -1;
            for(int j = 0; j < initialDistricts.size(); j++) {
                if(didx != j && initialDistricts.get(j).intersectsLine(edge)) {
                    adjacentDistrict = initialDistricts.get(j);
                    adjacentDistrictIdx = j;
                    break;
                }
            }

            if(adjacentDistrict.getPoints().size() > 6) {
                continue;
            }

            Point2D newPointCloserToStart = new Point2D.Double(percentageUp * startPoint.getX() + (1.0 - percentageUp) * endPoint.getX(),
                    percentageUp * startPoint.getY() + (1.0 - percentageUp) * endPoint.getY());
            Point2D newPointCloserToEnd = new Point2D.Double(percentageDown * startPoint.getX() + (1.0 - percentageDown) * endPoint.getX(),
                    percentageDown * startPoint.getY() + (1.0 - percentageDown) * endPoint.getY());

            double angle = Math.atan((newPointCloserToEnd.getY() - newPointCloserToStart.getY()) /
                    (newPointCloserToEnd.getX() - newPointCloserToStart.getX()));
            double targetAngle = angle + Math.PI / 2.0;

            double deltaX = Math.cos(targetAngle) * targetTriangleHeight;
            double deltaY = Math.sin(targetAngle) * targetTriangleHeight;

            Point2D triangleTop = new Point2D.Double(midPoint.getX() + deltaX, midPoint.getY() + deltaY);
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

            int otherEndIdx = 9999;
            int otherStartIdx = -1;
            for(int z = 0; z < adjacentDistrict.getPoints().size(); z++) {
                if(adjacentDistrict.getPoints().get(z) == endPoint) {
                    otherEndIdx = z;
                } else {
                    otherStartIdx = z;
                }
            }

            int otherSpliceIdx = -1;

            if(otherEndIdx == 0 && otherStartIdx == adjacentDistrict.getPoints().size() - 1) {
                otherSpliceIdx = adjacentDistrict.getPoints().size() - 1;
            } else if(otherStartIdx == 0 && otherEndIdx == adjacentDistrict.getPoints().size() - 1) {
                otherSpliceIdx = adjacentDistrict.getPoints().size() - 1;
            } else {
                otherSpliceIdx = Math.min(otherStartIdx, otherEndIdx);
            }

            ArrayList<Point2D> otherInsertPoints = new ArrayList<>();

            if(otherSpliceIdx == otherEndIdx) {
                otherInsertPoints.add(newPointCloserToEnd);
                otherInsertPoints.add(triangleTop);
                otherInsertPoints.add(newPointCloserToStart);
            } else {
                otherInsertPoints.add(newPointCloserToStart);
                otherInsertPoints.add(triangleTop);
                otherInsertPoints.add(newPointCloserToEnd);
            }

            String k = newPointCloserToStart.getX() + "_" + newPointCloserToStart.getY();
            pointCounts.put(k, 2);
            k = newPointCloserToEnd.getX() + "_" + newPointCloserToEnd.getY();
            pointCounts.put(k, 2);
            k = triangleTop.getX() + "_" + triangleTop.getY();
            pointCounts.put(k, 2);

            ArrayList<Point2D>otherNewPoints = insertInto(adjacentDistrict.getPoints(), otherInsertPoints,
                    otherSpliceIdx);

            Polygon2D adjacentNewPolygon = new election.sim.Polygon2D();
            for (Point2D p : otherNewPoints) {
                adjacentNewPolygon.append(p);
            }
            initialDistricts.set(adjacentDistrictIdx, adjacentNewPolygon);

            System.out.println("Exchanging Territory between district " + didx + ", and " + adjacentDistrictIdx);
        }
    }
}
