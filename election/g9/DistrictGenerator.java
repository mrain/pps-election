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

        boolean last = false;
        for(int i = 0; i < r; i++) {
            if(i == r - 1) last = true;
            List<District> temp = new ArrayList<District>();
            for(District d : disList) {
                temp.addAll(gerrySplit(d, d.voters.size()/3, repPerDistrict, last, numParties));
                //temp.addAll(splitTriangleDistrict(d, d.voters.size()/3, i));
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

    public List<District> gerrySplit(District startingDistrict, int repPerDistrict, int repNum, boolean last, int num){
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
            
            double diff = 300. / ((double)(repNum + 1.) * 6);
            double i = 1.;
            double dec = diff/2.;
            boolean check = false;
            int d = 0;
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

                /*polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(2));
                district2 = new District(polygon2, startingDistrict.voters, false);*/
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
                    else diff+=dec;
                }
                d++;
                if(d > 1000) System.out.println("stuck at 1");
            }
            valid = false;
            diff = 300. / ((double)(repNum + 1.) * 6);
            dec = diff/2.;
            check = false;
            d = 0;
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
                    else diff+=dec;
                }
                d++;
                if(d > 1000) System.out.println("stuck at 1");
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
            
            diff = 300. / ((double)(repNum + 1.) * 6);
            i = 1.;
            dec = diff/2.;
            check = false;
            d = 0;
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

                /*polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(0));
                district2 = new District(polygon2, startingDistrict.voters, false);*/
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
                    else diff+=dec;
                }
                d++;
                if(d > 1000) {
                    System.out.println("stuck at 2");
                    return districts[0];
                }
            }
            valid = false;
            diff = 300. / ((double)(repNum + 1.) * 6);
            dec = diff/2.;
            check = false;
            d = 0;
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
                    else diff+=dec;
                }
                d++;
                if(d>1000) {
                    System.out.println("stuck at 2");
                    return districts[0];
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
            
            diff = 300. / ((double)(repNum + 1.) * 6);
            i = 1.;
            dec = diff/2.;
            check = false;
            d = 0;
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

                /*polygon2.append(point2);
                polygon2.append(trialPoint);
                polygon2.append(polygon.getPoints().get(1));
                district2 = new District(polygon2, startingDistrict.voters, false);*/
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
                    else diff+=dec;
                }
                d++;
                if(d > 1000) {
                    System.out.println("stuck at 3");
                    return districts[0];
                }
            }
            valid = false;
            diff = 300. / ((double)(repNum + 1.) * 6);
            dec = diff/2;
            check = false;
            d = 0;
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
                    else diff+=dec;
                }
                d++;
                if(d > 1000) {
                    System.out.println("stuck at 3");
                    return districts[0];
                }
            }

            districts[2].add(district1);
            districts[2].add(district2);
            districts[2].add(district3);

            if(numParties == 3) {

                double[][] per = new double[3][3];

                per[0] = getPercentages(districts[0], 0);
                per[1] = getPercentages(districts[1], 0);
                per[2] = getPercentages(districts[2], 0);

                double[][] per1 = new double[3][3];

                per1[0] = getPercentages(districts[0], 1);
                per1[1] = getPercentages(districts[1], 1);
                per1[2] = getPercentages(districts[2], 1);

                double[][] per2 = new double[3][3];

                per2[0] = getPercentages(districts[0], 2);
                per2[1] = getPercentages(districts[1], 2);
                per2[2] = getPercentages(districts[2], 2);

                int[] rep = new int[3];
                double[] waste = new double[3];
                getWaste(rep, waste, per, repNum);

                int[] rep1 = new int[3];
                double[] waste1 = new double[3];
                getWaste(rep1, waste1, per1, repNum);

                int[] rep2 = new int[3];
                double[] waste2 = new double[3];
                getWaste(rep2, waste2, per2, repNum);

                return districts[getMaxRep(rep1, waste1)];
            }
            else {

                double[][] per = new double[2][2];

                per[0] = getPercentages(districts[0], 0);
                per[1] = getPercentages(districts[1], 0);

                double[][] per1 = new double[2][2];

                per1[0] = getPercentages(districts[0], 1);
                per1[1] = getPercentages(districts[1], 1);

                int[] rep = new int[2];
                double[] waste = new double[2];
                getWaste(rep, waste, per, repNum);

                int[] rep1 = new int[2];
                double[] waste1 = new double[2];
                getWaste(rep1, waste1, per1, repNum);

                return districts[getMaxRep1(rep1, waste1)];
            }
            //if(!last) return districts[getMaxPer(per, per2)];
            //else return districts[getMaxRep(rep1, waste1)];
            //return districts[getMaxEfficiency(waste1, waste2, waste)];
            //return districts[getMinWaste(waste1)];
            //return districts[getMaxWaste(waste, waste2)];
            //if(!last) return districts[getGreedy(rep2, rep, rep1, waste2, per1, per)];
            //else return districts[getMaxRep(rep2, waste2)];
        }
    }

    public int getGreedy(int[] rep1, int[] rep2, int[] rep, double[] waste1, double[][] per, double[][] per2) {
        for(int i = 0; i < numParties; i++) {
            int temp = rep1[i];
            if(temp > rep2[i] && temp > rep[i]) return getMaxRep(rep1, waste1);
        }
        //System.out.println("Not the biggest");
        return getMaxPer(per, per2);
    }

    public int getMaxRep(int[] rep, double[] waste) {
        if(rep[0] == rep[1] && rep[0] == rep[2]) {
            return getMinIndex(waste);
        }

        else if(rep[0] == rep[1] && rep[0] > rep[2]) {
            if(waste[0] > waste[1]) return 1;
            else return 0;
        }

        else if(rep[1] == rep[2] && rep[1] > rep[0]) {
            if(waste[1] > waste[2]) return 2;
            else return 1;
        }

        else if(rep[0] == rep[2] && rep[2] > rep[1]) {
            if(waste[0] > waste[2]) return 2;
            else return 0;
        }

        else {
            return getMaxIndex(rep);
        }
    }

    public int getMaxRep1(int[] rep, double[] waste) {
        if(rep[0] == rep[1]) {
            return getMinIndex(waste);
        }

        else if(rep[0] > rep[1]) {
            return 0;
        }

        else return 1;
    }

    public int getMinWaste(double[] waste) {
        return getMinIndex(waste);
    }

    public int getMaxWaste(double[] waste, double[] waste1) {
        double[] totalWaste = new double[numParties];
        for(int i = 0; i < numParties; i++) {
            totalWaste[i] = waste[i] + waste1[i];
        }

        return getMaxIndex(totalWaste);
    }

    public int getMaxEfficiency(double[] our, double[] waste1, double[] waste2) {
        double gap[] = new double[numParties];
        for(int i = 0; i < 3; i++) {
            gap[i] = our[i] - waste1[i] - waste2[i];
        }
        return getMaxIndex(gap);
    }

    public int getMaxPer(double[][] per, double[][] per2) {
        double[] percent = new double[numParties];
        for(int i = 0; i < numParties; i++) {
            percent[i] = per[0][i] + per2[0][i] + per[1][i] + per2[1][i] + per[2][i] + per2[2][i]; 
        }

        double max = 0;
        int index = 0;

        return getMaxIndex(percent);
    }

    public int getMinIndex(double[] waste) {
        double min = 100000000.;
        int index = 1;

        for(int i = 0; i < numParties; i++) {
            if(waste[i] < min) {
                min = waste[i];
                index = i;
            }
        }
        return index;
    }

    public int getMaxIndex(double[] waste) {
        double max = 0;
        int index = 1;

        for(int i = 0; i < numParties; i++) {
            if(waste[i] > max) {
                max = waste[i];
                index = i;
            }
        }
        return index;
    }

    public int getMaxIndex(int[] rep) {
        double max = 0;
        int index = 1;

        for(int i = 0; i < numParties; i++) {
            if(rep[i] > max) {
                max = rep[i];
                index = i;
            }
        }
        return index;
    }

    public void getWaste(int[] rep, double[] waste, double[][] per, int repPerDistrict) {
        for(int i = 0; i < numParties; i++) {
            for(int j = 0; j < numParties; j++) {
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

    public double[] getPercentages(List<District> districts, int party) {
        int[][] count = new int[numParties][numParties];
        for(int i = 0 ; i < numParties; i++) {
            List<Voter> voters = districts.get(i).voters;
            for(Voter v: voters) {
                double max = 0;
                int index = 0;
                for(int j = 0; j < numParties; j++) {
                    if(v.getPreference().get(j) > max) {
                        max = v.getPreference().get(j);
                        index = j;
                    }
                }
                count[i][index]++;
            }
        }

        double[] total = new double[numParties];
        for(int i = 0; i < numParties; i++) {
            total[i] = Double.valueOf(count[i][party]) / Double.valueOf(districts.get(i).voters.size());
        }

        return total;
    }

    public List<District> splitTriangleDistrict(District startingDistrict, int repPerDistrict, int edge){
        if (startingDistrict.polygon.size() != 3){
            throw new IllegalArgumentException("Not a triangle, can't split a " + startingDistrict.polygon.size() + " sided polygon. ");
        } else {
            //List<Integer> ran = new ArrayList<>();
            //ran.add(0);
            //ran.add(1);
            //ran.add(2);
            //Collections.shuffle(ran);
            //pick a point on a line between the vertices of a triangle, then try to split into 2 districts. 
            Polygon2D polygon = startingDistrict.polygon;
            Point2D[] points = new Point2D[3];
            for(int i = 0; i < 3; i++) {
            	points[i] = polygon.getPoints().get(i);
            }

            Point2D point1 = polygon.getPoints().get(edge % 3);
            Point2D point2 = polygon.getPoints().get((edge+1) % 3);
            Point2D point3 = polygon.getPoints().get((edge+2) % 3);

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
                if(edge % 4 == 0) {
                    polygon1.append(point1);
                    polygon1.append(trialPoint);
                    polygon1.append(point3);
                }
                else if(edge % 4 == 1) {
                    polygon1.append(point3);
                    polygon1.append(point1);
                    polygon1.append(trialPoint);
                }
                else if(edge % 4 == 2) {
                    polygon1.append(trialPoint);
                    polygon1.append(point3);
                    polygon1.append(point1);
                }
                else {
                    polygon1.append(point3);
                    polygon1.append(point1);
                    polygon1.append(trialPoint);
                }
                district1 = new District(polygon1, startingDistrict.voters, false);

                /*polygon2.append(trialPoint);
                polygon2.append(point2);
                polygon2.append(polygon.getPoints().get((edge+2) % 3));
                district2 = new District(polygon2, startingDistrict.voters, false);*/
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
                /*polygon2.append(mid1);
                polygon2.append(trialPoint);
                polygon2.append(point3);*/
                if(edge % 3 == 0) {
                    polygon2.append(mid1);
                    polygon2.append(trialPoint);
                    polygon2.append(point3);
                }
                else if(edge % 3 == 1) {
                    polygon2.append(point3);
                    polygon2.append(mid1);
                    polygon2.append(trialPoint);
                }
                else {
                    polygon2.append(trialPoint);
                    polygon2.append(point3);
                    polygon2.append(mid1);
                }

                district2 = new District(polygon2, startingDistrict.voters, false);

                Polygon2D polygon3 = new Polygon2D();
                /*polygon3.append(trialPoint);
                polygon3.append(point2);
                polygon3.append(point3);*/

                if(edge % 3 == 0) {
                    polygon3.append(trialPoint);
                    polygon3.append(point2);
                    polygon3.append(point3);
                }
                else if(edge % 3 == 1) {
                    polygon3.append(point3);
                    polygon3.append(trialPoint);
                    polygon3.append(point2);
                }
                else {
                    polygon3.append(point2);
                    polygon3.append(point3);
                    polygon3.append(trialPoint);
                }
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
