package election.maps;

import java.awt.geom.*;
import java.io.*;
import java.util.*;

public class RandomMapGenerator {
    private static int numVoters = 333333;
    private static int numParties = 3;
    private static double scale = 1000.0;
    private static Random random;

    public static void main(String[] args) {
        random = new Random();

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        System.out.println(numVoters + " " + numParties);
        for (int i = 0; i < numVoters; ++ i) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            System.out.print(x + " " + y);
            for (int j = 0; j < numParties; ++ j)
                System.out.print(" " + random.nextDouble());
            System.out.println();
        }
        return;
    }
}