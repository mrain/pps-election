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
                //temp.addAll(gerrySplit(d, d.voters.size()/3, repPerDistrict));
                temp.addAll(splitTriangleDistrict(d, d.voters.size()/3));
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

    public List<District> gerrySplit(District startingDistrict, int repPerDistrict, int repNum){
        if (startingDistrict.polygon.size() != 3){
            throw new IllegalArgumentException("Not a triangle, can't split a " + startingDistrict.polygon.size() + " sided polygon. ");
        } else {
            //System.out.println("Getting here");
            //pick a point on a line between the vertices of a triangle, then try to split into 2 districts. 
            Polygon2D polygon = startingDistrict.polygon;
            Point2D point1 = polygon.getPoints().get(0);
            Point2D point2 = polygon.getPoints().get(1);

            List<District>[] districts = new ArrayList[3];
            for(int i = 0; i < 3; i++) districts[i] = new ArrayList<District>();
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

            districts[0].add(district1);
            districts[0].add(district2);
            districts[0].add(district3);

            //System.out.println("Getting here1");

            point1 = polygon.getPoints().get(1);
            point2 = polygon.getPoints().get(2);

            slope = (point1.getY() - point2.getY()) / (point1.getX() - point2.getX());
            b = point2.getY() - (slope * point2.getX());
            valid = false;
            district1 = new District();
            district2 = new District();
            district3 = new District();
            mid1 = new Point2D.Double(0,0);
            
            diff = 1.;
            i = 1.;
            dec = 1.;
            check = false;
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
                polygon1.append(polygon.getPoints().get(0));
                district1 = new District(polygon1, startingDistrict.voters, false);

                polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(0));
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
                polygon2.append(polygon.getPoints().get(0));
                district2 = new District(polygon2, startingDistrict.voters, false);

                Polygon2D polygon3 = new Polygon2D();
                polygon3.append(trialPoint);
                polygon3.append(point2);
                polygon3.append(polygon.getPoints().get(0));
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

            districts[1].add(district1);
            districts[1].add(district2);
            districts[1].add(district3);

            //System.out.println("Getting here2");

            point1 = polygon.getPoints().get(0);
            point2 = polygon.getPoints().get(2);

            slope = (point1.getY() - point2.getY()) / (point1.getX() - point2.getX());
            b = point2.getY() - (slope * point2.getX());
            valid = false;
            district1 = new District();
            district2 = new District();
            district3 = new District();
            mid1 = new Point2D.Double(0,0);
            
            diff = 1.;
            i = 1.;
            dec = 1.;
            check = false;
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
                polygon1.append(polygon.getPoints().get(1));
                district1 = new District(polygon1, startingDistrict.voters, false);

                polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(1));
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
                polygon2.append(polygon.getPoints().get(1));
                district2 = new District(polygon2, startingDistrict.voters, false);

                Polygon2D polygon3 = new Polygon2D();
                polygon3.append(trialPoint);
                polygon3.append(point2);
                polygon3.append(polygon.getPoints().get(1));
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

            districts[2].add(district1);
            districts[2].add(district2);
            districts[2].add(district3);

            double[][] per = new double[3][3];

            per[0] = getPercentages(districts[0]);
            per[1] = getPercentages(districts[1]);
            per[2] = getPercentages(districts[2]);

            int[] rep = new int[3];
            double[] waste = new double[3];
            getWaste(rep, waste, per, repNum);

            //System.out.println(per[0][0] + ", " + per[0][1] + ", " + per[0][2]);
            //System.out.println(per[1][0] + ", " + per[1][1] + ", " + per[1][2]);
            //System.out.println(per[2][0] + ", " + per[2][1] + ", " + per[2][2]);

            if(rep[0] == rep[1] && rep[0] == rep[2]) {
                return districts[getMinIndex(waste)];
            }

            else if(rep[0] == rep[1] && rep[0] > rep[2]) {
                if(waste[0] > waste[1]) return districts[1];
                else return districts[0];
            }

            else if(rep[1] == rep[2] && rep[1] > rep[0]) {
                if(waste[1] > waste[2]) return districts[2];
                else return districts[1];
            }

            else if(rep[0] == rep[2] && rep[2] > rep[1]) {
                if(waste[0] > waste[2]) return districts[2];
                else return districts[0];
            }

            else {
                return districts[getMaxIndex(rep)];
            }
        }
    }

    public int getMinIndex(double[] waste) {
        double min = 100000000.;
        int index = 1;

        for(int i = 0; i < 3; i++) {
            if(waste[i] < min) {
                min = waste[i];
                index = i;
            }
        }
        return index;
    }

    public int getMaxIndex(int[] rep) {
        double max = 0;
        int index = 1;

        for(int i = 0; i < 3; i++) {
            if(rep[i] > max) {
                max = rep[i];
                index = i;
            }
        }
        return index;
    }

    public void getWaste(int[] rep, double[] waste, double[][] per, int repPerDistrict) {
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                if(repPerDistrict == 3) {
                    if(per[i][j] > .75) {
                        rep[i] += 3;
                        waste[i] += per[i][j] - .75;
                    }
                    else if(per[i][j] > .5) {
                        rep[i] += 2;
                        waste[i] += per[i][j] - .5;
                    }
                    else if(per[i][j] > .25) {
                        rep[i] += 1;
                        waste[i] += per[i][j] - .25;
                    }
                    else {
                        waste[i] += per[i][j];
                    }
                }
                else {
                    if(per[i][j] > .5) {
                        rep[i] += 1;
                        waste[i] += per[i][j] - .5;
                    }
                    else {
                        waste[i] += per[i][j];
                    }
                }
            }
        }
    }

    //Gets info for party1
    public double[] getPercentages(List<District> districts) {
        int[][] count = new int[3][3];
        for(int i = 0 ; i < 3; i++) {
            List<Voter> voters = districts.get(i).voters;
            for(Voter v: voters) {
                double max = 0;
                int index = 0;
                for(int j = 0; j < 3; j++) {
                    if(v.getPreference().get(j) > max) {
                        max = v.getPreference().get(j);
                        index = j;
                    }
                }
                count[i][index]++;
            }
        }

        double[] total = new double[3];
        for(int i = 0; i < 3; i++) {
            total[i] = Double.valueOf(count[i][0]) / Double.valueOf(districts.get(i).voters.size());
        }

        return total;
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
