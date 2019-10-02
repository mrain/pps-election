package election.g5;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;


public class CoastalCitiesMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;

    private class City {
        // Position of the city
        private Point2D.Double pos;

        // Radius of the city; unused for now
        // double radius;

        // Number of voters in a city; unused for now
        // int population = 0;

        public City(Point2D.Double pos) { this.pos = pos; }
        public Point2D.Double getPos() { return this.pos; }

        // Returns distance of pos to city center.
        public double getDist(Point2D.Double pos) {
            double xDist = pos.getX() - this.pos.getX();
            double yDist = pos.getY() - this.pos.getY();

            return Math.sqrt(xDist*xDist + yDist*yDist);
        }

        public String toString() {
            return "city @ (" + String.valueOf(pos.getX()) + ", " + String.valueOf(pos.getY()) + ")";
        }

        // public void addPop(int n) { this.population += n; }
    }

    // Returns a value between 0 and 1 that represents how desirable a position is to build a city.
    public double getDesirability(Point2D.Double pos) {
        // Just gonna use scaled distance to nearest coast for now.
        return 1 - getNearestCoastDist(pos)/1000.0;

    /*  This is the desirability index idea Jonas + Jaewan discussed on 10/01.
        Either the code or the math is wrong though, needs fixing.

        double x = pos.getX();
        double y = pos.getY();
        double theta;

        // Distance to bottom edge
        double botDist = y;

        theta = Math.atan(y/x);
        // Distance to left edge
        double leftDist = Math.sqrt(x*x + y*y) * Math.sin(Math.PI/3 - theta);

        theta = Math.atan(y/1000-x);
        // Distance to right edge
        double rightDist = Math.sqrt((1000-x)*(1000-x) + y*y) * Math.sin(Math.PI/3 - theta);

        // I thought this would work but it doesn't seem to work
        double desirability = ((botDist*botDist + leftDist*leftDist + rightDist*rightDist) - 750*750)
                              / (500*Math.sqrt(3) * 500*Math.sqrt(3));

        System.out.println(botDist);
        System.out.println(leftDist);
        System.out.println(rightDist);

        // This is negative a lot of the time
        System.out.println(desirability);

        return desirability;
        */
    }

    public double getNearestCoastDist(Point2D.Double pos) {
        double x = pos.getX();
        double y = pos.getY();
        double theta;

        // Distance to bottom edge
        double botDist = y;

        theta = Math.atan(y/x);
        // Distance to left edge
        double leftDist = Math.sqrt(x*x + y*y) * Math.sin(Math.PI/3 - theta);

        theta = Math.atan(y/1000-x);
        // Distance to right edge
        double rightDist = Math.sqrt((1000-x)*(1000-x) + y*y) * Math.sin(Math.PI/3 - theta);

        return Math.min(Math.min(botDist, leftDist), rightDist);
    }


    public List<City> getCities(long numCities, long seed) {
        List<City> cities = new ArrayList<City>();
        Random random = new Random(seed);

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        int i = 0;
        while (i < numCities) {
            double x, y;

            // Randomly sample a valid point
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 867.0; // ~= 500 * sqrt(3)
            } while (!triangle.contains(x, y));

            Point2D.Double pos = new Point2D.Double(x, y);
            double desirability = getDesirability(pos);

            // The higher the desirability, the higher the chance we build a city here
            if (random.nextDouble() < Math.pow(desirability, 10)) {
                cities.add(new City(pos));
                i++;
            }
        }

        System.out.println("City locations:");
        for (City city : cities)
            System.out.println(city);

        return cities;
    }

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> voters = new ArrayList<Voter>();
        Random random = new Random(seed);

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        // Generate cities
        // Sample numCities from dist N(10, 2)
        long numCities = Math.round(random.nextGaussian() * 2 + 10);
        System.out.println(numCities);
        List<City> cities = getCities(numCities, seed);

        int i = 0;
        double totalDist = 0;
        while (i < numVoters) {
            double x, y;

            // Randomly sample a valid point
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 867.0; // ~= 500 * sqrt(3)
            } while (!triangle.contains(x, y));

            Point2D.Double pos = new Point2D.Double(x, y);

            // Compute distance to closest city
            double distToClosestCity = 1000.0;
            for (City city : cities) {
                double distToCity = city.getDist(pos);
                if (distToCity < distToClosestCity)
                    distToClosestCity = distToCity;
            }

            // The closer to the closest city, the higher the chance a voter is generated there
            if (random.nextDouble() > distToClosestCity/(random.nextGaussian()*50+100)) {
                List<Double> pref = new ArrayList<Double>();
                for (int j = 0; j < numParties; ++j)
                    pref.add(random.nextDouble());

                voters.add(new Voter(pos, pref));
                totalDist += distToClosestCity;
                i++;
            }
        }

        System.out.println("Average voter distance to nearest city: " +
                           String.valueOf(totalDist/numVoters));

        return voters;
    }
}
