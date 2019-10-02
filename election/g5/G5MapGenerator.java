package election.g5;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class G5MapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;
    private static int numCentersOfInfluence = 27;
    private static int constant = 9;

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> voters = new ArrayList<>();
        Random random = new Random();

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        for (int i = 0; i < numVoters; ++ i) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            List<Double> pref = new ArrayList<>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            voters.add(new Voter(new Point2D.Double(x, y), pref));
        }

        // randomly create centers of influence to update voter preferences
        List<Voter> centersOfInfluence = new ArrayList<>();
        List<Voter> votersInfluenced = new ArrayList<>();

        for (int i = 0; i < this.numCentersOfInfluence; i++) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            List<Double> pref = new ArrayList<>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            centersOfInfluence.add(new Voter(new Point2D.Double(x, y), pref));
        }

        // update each voter preference based on influences - yes, the order of influences can matter in the result!
        for (int i = 0; i < this.numCentersOfInfluence; i++) {
            votersInfluenced = new ArrayList<>();
            System.out.println("i: " + i);
            Voter centerOfInfluence = centersOfInfluence.get(i);
            List<Double> centerOfInfluencePrefs = centerOfInfluence.getPreference();
            for (int j = 0; j < numVoters; j++) {
                Voter voter = voters.get(j);
                List<Double> voterPrefs = voter.getPreference();
                double distance = centerOfInfluence.getLocation().distance(voter.getLocation());
                double influence = constant/(constant + distance);
                List<Double> prefs = new ArrayList<>();
                for (int k = 0; k < numParties; k++) {
                    double pref = (1-influence)*voterPrefs.get(k) + influence*centerOfInfluencePrefs.get(k);
                    prefs.add(pref);
                }
                Voter updatedVoter = new Voter(new Point2D.Double(voter.getLocation().getX(), voter.getLocation().getY()), prefs);
                votersInfluenced.add(updatedVoter);
            }
            voters = votersInfluenced;
        }
        return voters;
    }
}
