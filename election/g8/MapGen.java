package election.g8;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;
import java.awt.Point;

public class MapGen implements election.sim.MapGenerator {
    private static double scale = 1000.0;
    private Path2D triangle;
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {

        List<Voter> ret = new ArrayList<Voter>();
        Random random = new Random(seed);

        triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();
        if(numParties == 3) {
            //Begin Nick Code
            Point2D cities[] = new Point2D[6];
            cities[0] = new Point2D.Double(150., 100.);
            cities[1] = new Point2D.Double(900, 75);
            cities[2] = new Point2D.Double(600, 500.*Math.sqrt(3) - 100.);
            cities[3] = new Point2D.Double(375, 300);
            cities[4] = new Point2D.Double(700, 350);
            cities[5] = new Point2D.Double(300, 200);
            double x;
            double y;
            for (int i = 0; i < numVoters; ++i) {
                double rand = random.nextInt(5);
                if(rand == 0) {
                    rand = random.nextInt(2);
                    Point2D voterSpot;
                    if (rand == 1) {
                        voterSpot = getPointNearCity(cities[0], random);
                    } else {
                        voterSpot = getPointNearCity(cities[2], random);
                    }
                    ret.add(new Voter(voterSpot, seededPreferences(0., .2, .1, random)));
                } else if (rand == 1) {
                    //big city 2
                    rand = random.nextInt(2);
                    Point2D voterSpot;
                    if (rand == 1) {
                        voterSpot = getPointNearCity(cities[1], random);
                    } else {
                        voterSpot = getPointNearCity(cities[3], random);
                    }
                    ret.add(new Voter(voterSpot, seededPreferences(0., .1, .2, random)));
                } else if(rand == 2) {
                    rand = random.nextInt(2);
                    Point2D voterSpot;
                    if (rand == 1) {
                        voterSpot = getPointNearCity(cities[4], random);
                    } else {
                        voterSpot = getPointNearCity(cities[5], random);
                    }
                    ret.add(new Voter(voterSpot, seededPreferences(0., .1, .1, random)));
                }
                else {
                    //random voter
                    do {
                        x = random.nextDouble() * 1000.0;
                        y = random.nextDouble() * 900.0;
                    } while (!triangle.contains(x, y));
                    List<Double> pref = seededPreferences(.2, 0, 0, random);
                    ret.add(new Voter(new Point2D.Double(x, y), pref));
                }
            }
        }
        else if(numParties == 2){
            // Begin Jiaqi Code
            Point2D cities[] = new Point2D[6];
            cities[0] = new Point2D.Double(150., 100.);
            cities[1] = new Point2D.Double(900, 75);
            cities[2] = new Point2D.Double(600, 500.*Math.sqrt(3) - 100.);
            cities[3] = new Point2D.Double(375, 300);
            double x;
            double y;
            for (int i = 0; i < numVoters; ++i) {
                double rand = random.nextInt(4);
                if(rand == 0) {
                    rand = random.nextInt(2);
                    Point2D voterSpot;
                    if (rand == 1) {
                        voterSpot = getPointNearCity(cities[0], random);
                    } else {
                        voterSpot = getPointNearCity(cities[2], random);
                    }
                    ret.add(new Voter(voterSpot, seededPreferences_2(0, .2, random)));
                } else if (rand == 1) {
                    //big city 2
                    rand = random.nextInt(2);
                    Point2D voterSpot;
                    if (rand == 1) {
                        voterSpot = getPointNearCity(cities[1], random);
                    } else {
                        voterSpot = getPointNearCity(cities[3], random);
                    }
                    ret.add(new Voter(voterSpot, seededPreferences_2(0, .2, random)));
                }
                else {
                    //random voter
                    do {
                        x = random.nextDouble() * 1000.0;
                        y = random.nextDouble() * 900.0;
                    } while (!triangle.contains(x, y));
                    List<Double> pref = seededPreferences_2(.2, 0, random);
                    ret.add(new Voter(new Point2D.Double(x, y), pref));
                }
            }

        }

        return ret;
    }

    private List<Double> seededPreferences(double seed1, double seed2, double seed3, Random r) {
        double max = Math.max(Math.max(seed1, seed2), seed3);
        List<Double> ret = new ArrayList<Double>();
        ret.add(seed1 + r.nextDouble() * (1-max));
        ret.add(seed2 + r.nextDouble() * (1-max));
        ret.add(seed3 + r.nextDouble() * (1-max));
        return ret;
    }

    private List<Double> seededPreferences_2(double seed1, double seed2, Random r) {
        double max = Math.max(seed1, seed2);
        List<Double> ret = new ArrayList<Double>();
        ret.add(seed1 + r.nextDouble() * (1-max));
        ret.add(seed2 + r.nextDouble() * (1-max));
        return ret;
    }


    private Point2D getPointNearCity(Point2D city, Random r) {
        // Get point a distance from the city determined by normal distribution
        double x;
        double y;
        Point2D p;
        do {
            double dist = r.nextGaussian() * 200;
            double theta = r.nextDouble() * Math.PI * 2;
            x = city.getX() + dist * Math.cos(theta);
            y = city.getY() + dist * Math.sin(theta);
            p = new Point2D.Double(x, y);
        } while(!triangle.contains(x, y));
        return p;

    }
}