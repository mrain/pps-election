package election.g;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class EdgeMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;
    private static double THRESHOLDPERCENT = 45.0/81.0;
    private static double THRESHOLDBORDER = 50.;
    
    private static double corner1X = 0.0;
    private static double corner1Y = 0.0;
    private static double corner2X = 1000.0;
    private static double corner2Y = 0.0;
    private static double corner3X = 500.0;
    private static double corner3Y = 500. * Math.sqrt(3);
    
    /* make 2 methods, change what we do depending on how many 
    * parties there are
    *
    * Keep threshold to nearness to edge
    * create a smaller triangle and make sure it's within one triangle but not the other
    * then extend the traingle once a threshold is reached.
    */

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> ret = new ArrayList<Voter>();
        Random random = new Random();

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(corner1X, corner1Y);
        triangle.lineTo(corner2X, corner2Y);
        triangle.lineTo(corner3X, corner3Y);
        triangle.closePath();
        
        Path2D threshold1 = new Path2D.Double();
        threshold1.moveTo(THRESHOLDBORDER * Math.sqrt(3), THRESHOLDBORDER);
        threshold1.lineTo(1000. - (THRESHOLDBORDER*Math.sqrt(3)), THRESHOLDBORDER);
        threshold1.lineTo(500., corner3Y - 100);
        
        //how many voters we want within the initial threshold convert to int
        int threshold1number = (int) (THRESHOLDPERCENT * numVoters);
        System.out.println(threshold1number);
        for (int i = 0; i < threshold1number; ++ i) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y) || threshold1.contains(x, y));
            List<Double> pref = new ArrayList<Double>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            ret.add(new Voter(new Point2D.Double(x, y), getPrefBasedOnVoterRegion(x, y, numParties)));
        }
        for (int i = threshold1number; i < numVoters; ++i){
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!threshold1.contains(x, y));
            List<Double> pref = new ArrayList<Double>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            ret.add(new Voter(new Point2D.Double(x, y), getPrefBasedOnVoterRegion(x, y, numParties)));
        }
        return ret;
    }
    
    /**
     * Get party preference based on region of the voter
     * (Works for three party)
     * @param voterX
     * @param voterY
     * @param numParties
     * @return
     */
    public List<Double> getPrefBasedOnVoterRegion(double voterX, double voterY, int numParties) {
        List<Double> pref = new ArrayList<Double>();
        
        double distancePoint1 = calculateDistanceBetweenPoints(voterX, voterY, corner1X, corner1Y);
        double distancePoint2 = calculateDistanceBetweenPoints(voterX, voterY, corner2X, corner2Y);
        double distancePoint3 = calculateDistanceBetweenPoints(voterX, voterY, corner3X, corner3Y);
        
        double minDistance = 0.0;
        int minDistancePoint = 0;
        if(distancePoint1 < distancePoint2){
            minDistance = distancePoint1;
            minDistancePoint = 1;
        }else{
            minDistance = distancePoint2;
            minDistancePoint = 2;
        }
        
        if(distancePoint3 < minDistance){
            minDistance = distancePoint3;
            minDistancePoint = 3;
        }
        
        
        for (int j = 0; j < numParties; ++ j){
            if(minDistancePoint == j + 1){
                pref.add(1.0);
            }else{
                pref.add(0.0);
            }
        }
        return pref;
    }
    
    /**
     * Calculate euclidean distance between two points
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public double calculateDistanceBetweenPoints(double x1, double y1, double x2, double y2) {       
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }
}