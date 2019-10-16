package election.g0;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class TournamentMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;

    private static List<Polygon2D> districts;

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> ret = new ArrayList<Voter>();
        districts = new ArrayList<Polygon2D>();
        Random random = new Random();

        double height = scale / 2.0 * Math.sqrt(3);
        double hstep = scale / 9.0;
        for (int i = 0; i < 9; ++ i) {
            double top = height * (9 - i) / 9.0;
            double btm = top - height / 9.0;
            double left = scale / 2 - hstep / 2 * (i + 1);
            for (int j = 0; j <= i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(left + hstep * j, btm);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep / 2, top);
                districts.add(polygon);
            }
            for (int j = 0; j < i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(left + hstep * j + hstep / 2, top);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep * 3 / 2, top);
                districts.add(polygon);
            }
        }

        int[] favoredParty = new int[81];
        Arrays.fill(favoredParty, 0);
        for (int j = 1; j < numParties; ++ j)
            for (int i = 0; i < 81 / numParties; ++ i) {
                int k = Math.abs(random.nextInt() % 81);
                while (favoredParty[k] != 0)
                    k = Math.abs(random.nextInt() % 81);
                favoredParty[k] = j;
            }

        for (int i = 0; i < numVoters; ++ i) {
            double x, y;
            int k = -1;
            while (true) {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
                for (int j = 0; j < districts.size(); ++ j)
                    if (districts.get(j).strictlyContains(new Point2D.Double(x, y))) {
                        k = j;
                        break;
                    }
                if (k != -1) break;
            }
            List<Double> pref = new ArrayList<Double>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            pref.set(favoredParty[k], 1.0);
            ret.add(new Voter(new Point2D.Double(x, y), pref));
        }
        return ret;
    }
}
