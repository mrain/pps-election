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

        int r;
        if(numDistricts == 81) r = 4;
        else r = 5;

        Polygon2D init = new Polygon2D();
        init.append(0.,0.);
        init.append(1000., 0.);
        init.append(500., 500. * Math.sqrt(3));
        District initDistrict = new District(init, voters, true);

        List<District> disList = new ArrayList<District>();
        disList.add(initDistrict);

        for(int i = 0; i < r; i++) {
            List<District> temp = new ArrayList<District>();
            for(District d : disList) {
                temp.addAll(splitTriangleDistrict(d, d.voters.size()/3));
                //
            }
            System.out.println(temp.size());
            disList = temp;
        }

        for(District d : disList) {
            //System.out.println(d.polygon);
            result.add(d.polygon);
        }

        return result;
    }

    public List<District> initSplit(List<Voter> voters) {
        List<District> output = new LinkedList<District>();
        numVoters = voters.size();
        int triSize = numVoters / 9;
        int trapSize = numVoters * 4 / 9;
        District triangle = getTriangle(voters, triSize);
        output.add(triangle);

        Point2D point1 = triangle.polygon.getPoints().get(1);
        Point2D point2 = triangle.polygon.getPoints().get(2);
        District trap1 = getTrap(voters, trapSize, point1, point2);
        output.add(trap1);

        point1 = trap1.polygon.getPoints().get(3);
        point2 = trap1.polygon.getPoints().get(2);
        Polygon2D temp = new Polygon2D();
        temp.append(point1);
        temp.append(point2);
        temp.append(1000., 0);
        temp.append(0, 0); 

        List<Voter> initVoters = trap1.populateDistrict(temp, voters);

        System.out.println("LAST!: " + temp);
        System.out.println(initVoters.size());
        District trap2 = new District(temp, initVoters, true);
        output.add(trap2);

        return output;
    }

    public District getTriangle(List<Voter> voters, int targetNum) {
        District output = new District();
        double x = 500.;
        double y = 500. * Math.sqrt(3);
        boolean valid = false;
        double a = 60. * Math.PI/180.;
        int i = 0;
        while(!valid) {
            Polygon2D tri = new Polygon2D();
            double leftX = x - i * Math.cos(a);
            double leftY = y - i * Math.sin(a);
            double rightX = x + (x - leftX);
            double rightY = leftY;

            tri.append(x, y);
            tri.append(leftX, leftY);
            tri.append(rightX, rightY);
            //System.out.println(tri);
            List<Voter> temp = output.populateDistrict(tri, voters);
            //System.out.println(i + ": " + temp.size());
            if(temp.size() > targetNum) {
                System.out.println(i + ": " + temp.size());
                valid = true;
                output.polygon = tri;
                output.voters = temp;
            }
            i++;
        }
        return output;
    }

    public District getTrap(List<Voter> voters, int targetNum, Point2D left, Point2D right) {
        District output = new District();
        double lx = left.getX();
        double ly = left.getY();
        double rx = right.getX();
        double ry = right.getY();
        boolean valid = false;
        double a = 60. * Math.PI/180.;
        int i = 0;
        while(!valid) {
            Polygon2D trap = new Polygon2D();
            double leftX = lx - i * Math.cos(a);
            double leftY = ly - i * Math.sin(a);
            double rightX = 500. + (500. - leftX);
            double rightY = leftY;

            trap.append(left);
            trap.append(right);
            trap.append(rightX, rightY);
            trap.append(leftX, leftY);
            System.out.println(trap);
            List<Voter> temp = output.populateDistrict(trap, voters);
            System.out.println(i + ": " + temp.size());
            if(temp.size() > targetNum) {
                System.out.println(i + ": " + temp.size());
                valid = true;
                output.polygon = trap;
                output.voters = temp;
            }
            i++;
        }
        return output;
    }

    public List<District> splitTriangleDistrict(District startingDistrict, int repPerDistrict){
        if (startingDistrict.polygon.size() != 3){
            throw new IllegalArgumentException("Not a triangle, can't split a " + startingDistrict.polygon.size() + " sided polygon. ");
        } else {
            List<Integer> ran = new ArrayList<>();
            ran.add(0);
            ran.add(1);
            ran.add(2);
            Collections.shuffle(ran);
            //pick a point on a line between the vertices of a triangle, then try to split into 2 districts. 
            Polygon2D polygon = startingDistrict.polygon;
            Point2D point1 = polygon.getPoints().get(ran.get(0));
            Point2D point2 = polygon.getPoints().get(ran.get(1));

            List<District> districts = new ArrayList<District>();
            double slope = (point1.getY() - point2.getY()) / (point1.getX() - point2.getX());
            double b = point2.getY() - (slope * point2.getX());
            boolean valid = false;
            double totalDistrictSize = startingDistrict.voters.size();
            double topThreshold = totalDistrictSize/3 + (totalDistrictSize/3 * .01);
            double bottomThreshold = totalDistrictSize/3 - (totalDistrictSize/3 * .01);
            District district1 = new District();
            District district2 = new District();
            District district3 = new District();
            Point2D mid1 = new Point2D.Double(0,0);
            
            double diff = 1.;
            double i = 1.;
            double dec = 1.;
            boolean check = false;
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
                polygon1.append(polygon.getPoints().get(ran.get(2)));
                district1 = new District(polygon1, startingDistrict.voters, false);

                polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(ran.get(2)));
                district2 = new District(polygon2, startingDistrict.voters, false);
                if (district1.voters.size() >= bottomThreshold && district1.voters.size() < topThreshold){
                    valid = true;
                    mid1 = trialPoint;
                } else if (district1.voters.size() > topThreshold){
                    //System.out.println("Something");
                    check = true;
                    diff-=dec;
                    //throw new IllegalArgumentException("District contains too many voters, can't split");
                } else {
                    if(check) {
                        check = false;
                        diff += dec;
                        dec /= 2.;
                    }
                    else diff+=3;
                }
            }
            valid = false;
            diff = 1.;
            dec = 1.;
            check = false;
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
                polygon2.append(polygon.getPoints().get(ran.get(2)));
                district2 = new District(polygon2, startingDistrict.voters, false);

                Polygon2D polygon3 = new Polygon2D();
                polygon3.append(trialPoint);
                polygon3.append(point2);
                polygon3.append(polygon.getPoints().get(ran.get(2)));
                district3 = new District(polygon3, startingDistrict.voters, false);
                if (district2.voters.size() >= bottomThreshold && district2.voters.size() < topThreshold){
                    valid = true;
                } else if (district2.voters.size() > topThreshold){
                    //System.out.println("Something2");
                    check = true;
                    diff-= dec;
                    //throw new IllegalArgumentException("District contains too many voters, can't split");
                } else {
                    if(check) {
                        check = false;
                        diff += dec;
                        dec /= 2.;
                    }
                    else diff+=3;
                }
            }

            districts.add(district1);
            districts.add(district2);
            districts.add(district3);
            return districts;
        }
    }
}
