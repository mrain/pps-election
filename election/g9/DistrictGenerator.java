package election.g9;

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
                    District testDistrict = new District(polygon, voters, false);
                    List<District> testDistricts = splitTriangleDistrict(testDistrict, repPerDistrict);
                    result.add(testDistricts.get(0).polygon);
                    result.add(testDistricts.get(1).polygon);
                    result.add(testDistricts.get(2).polygon);
                }
                for (int j = 0; j < i; ++ j) {
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(left + hstep * j + hstep / 2, top);
                    polygon.append(left + hstep * j + hstep, btm);
                    polygon.append(left + hstep * j + hstep * 3 / 2, top);
                    District testDistrict = new District(polygon, voters, false);
                    List<District> testDistricts = splitTriangleDistrict(testDistrict, repPerDistrict);
                    result.add(testDistricts.get(0).polygon);
                    result.add(testDistricts.get(1).polygon);
                    result.add(testDistricts.get(2).polygon);
                    //result.add(polygon);
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
        return result;
    }

    public List<District> splitTriangleDistrict(District startingDistrict, int repPerDistrict){
        if (startingDistrict.polygon.size() != 3){
            throw new IllegalArgumentException("Not a triangle, can't split a " + startingDistrict.polygon.size() + " sided polygon. ");
        } else {
            //pick a point on a line between the vertices of a triangle, then try to split into 2 districts. 
            Polygon2D polygon = startingDistrict.polygon;
            Point2D point1 = polygon.getPoints().get(0);
            Point2D point2 = polygon.getPoints().get(1);

            List<District> districts = new ArrayList<District>();
            double slope = (point1.getY() - point2.getY()) / (point1.getX() - point2.getX());
            double b = point2.getY() - (slope * point2.getX());
            boolean valid = false;
            double totalDistrictSize = startingDistrict.voters.size();
            double topThreshold = totalDistrictSize/3 + (totalDistrictSize/3 * .1);
            double bottomThreshold = totalDistrictSize/3 - (totalDistrictSize/3 * .1);
            District district1 = new District();
            District district2 = new District();
            District district3 = new District();
            Point2D mid1 = new Point2D.Double(0,0);
            
            int diff = 1;
            int i = 1;
            while (!valid){
                if (point1.getX() > point2.getX()){
                    i = diff * -1;
                } else {
                    i = diff;
                }
                Point2D trialPoint = new Point2D.Double(point1.getX() + i, (slope * (point1.getX() + i)) + b);
                Polygon2D polygon1 = new Polygon2D();
                Polygon2D polygon2 = new Polygon2D();
                polygon1.append(point1);
                polygon1.append(trialPoint);
                polygon1.append(polygon.getPoints().get(2));
                district1 = new District(polygon1, startingDistrict.voters, false);

                polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(2));
                district2 = new District(polygon2, startingDistrict.voters, false);
                if (district1.voters.size() >= bottomThreshold && district1.voters.size() < topThreshold){
                    valid = true;
                    mid1 = trialPoint;
                } else if (district1.voters.size() > topThreshold){
                    throw new IllegalArgumentException("District contains too many voters, can't split");
                } else {
                    diff+=3;
                }
            }
            valid = false;
            diff = 1;
            while (!valid){
                if (mid1.getX() > point2.getX()){
                    i = diff * -1;
                } else {
                    i = diff;
                }
                Point2D trialPoint = new Point2D.Double(mid1.getX() + i, (slope * (mid1.getX() + i)) + b);

                Polygon2D polygon2 = new Polygon2D();
                polygon2.append(mid1);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(2));
                district2 = new District(polygon2, startingDistrict.voters, false);

                Polygon2D polygon3 = new Polygon2D();
                polygon3.append(trialPoint);
                polygon3.append(point2);
                polygon3.append(polygon.getPoints().get(2));
                district3 = new District(polygon3, startingDistrict.voters, false);
                if (district2.voters.size() >= bottomThreshold && district2.voters.size() < topThreshold){
                    valid = true;
                } else if (district2.voters.size() > topThreshold){
                    throw new IllegalArgumentException("District contains too many voters, can't split");
                } else {
                    diff+=3;
                }
            }

            districts.add(district1);
            districts.add(district2);
            districts.add(district3);
            return districts;
        }
    }
}
