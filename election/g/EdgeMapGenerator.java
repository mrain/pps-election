package election.g;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class EdgeMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;
    private static double THRESHOLDPERCENT = .5;
    private static double THRESHOLDBORDER = 50.;

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
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();
        
        Path2D threshold1 = new Path2D.Double();
        threshold1.moveTo(THRESHOLDBORDER * Math.sqrt(3), THRESHOLDBORDER);
        threshold1.lineTo(1000. - (THRESHOLDBORDER*Math.sqrt(3)), THRESHOLDBORDER);
        threshold1.lineTo(500., (500-THRESHOLDBORDER) * Math.sqrt(3));
        
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
            ret.add(new Voter(new Point2D.Double(x, y), pref));
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
            ret.add(new Voter(new Point2D.Double(x, y), pref));
        }
        return ret;
    }
}
