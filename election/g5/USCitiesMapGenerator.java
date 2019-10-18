package election.g5;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;


public class USCitiesMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;

    private class City {
        // Position of the city
        private Point2D.Double pos;
        // Preference bias for party 1 and 2
        private double p1Bias, p2Bias;

        public City(Point2D.Double pos) {
            this.pos = pos;

            Random random = new Random();
            this.p1Bias = random.nextDouble();
            this.p2Bias = random.nextDouble();
        }

        public Point2D.Double getPos() { return this.pos; }
        public double getP1Bias() { return this.p1Bias; }
        public double getP2Bias() { return this.p2Bias; }

        // Returns distance of pos to city center.
        public double getDist(Point2D.Double pos) {
            double xDist = pos.getX() - this.pos.getX();
            double yDist = pos.getY() - this.pos.getY();

            return Math.sqrt(xDist*xDist + yDist*yDist);
        }

        public String toString() {
            return "city @ (" + String.valueOf(pos.getX()) + ", " + String.valueOf(pos.getY()) + ")";
        }
    }

    // Returns a value between 0 and 1 that represents how desirable a position is to build a city.
    public double getDesirability(Point2D.Double pos) {
        // Just gonna use scaled distance to nearest coast for now.
        return 1 - getNearestCoastDist(pos)/1000.0;
    }

    public double getDesirability(City city) { return getDesirability(city.getPos()); }

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

    // populate real cities and their voting preference
    private List<City> scale(List<City> realCities) {
        List<City> cities = new ArrayList<City>();
        City upperLeft = new City(new Point2D.Double(1000,0));
        City upperRight = new City(new Point2D.Double(0,0));
        City bottom = new City(new Point2D.Double(0, 1000));
        for (City city : realCities) {
            if (city.pos.y < bottom.pos.y) {bottom = city;}
            if (city.pos.x < upperLeft.pos.x) {upperLeft = city;}
            if (city.pos.x > upperRight.pos.x) {upperRight = city;}
        }
        double topx = (upperRight.pos.y+2*upperRight.pos.x-(upperLeft.pos.y-2*upperLeft.pos.x))/4;
        double leftCorner = (bottom.pos.y-(upperLeft.pos.y-2*upperLeft.pos.x))/2;
        double rightCorner = (upperRight.pos.y+2*upperRight.pos.x-bottom.pos.y)/2;

        Polygon2D triangle = new Polygon2D();
        triangle.append((bottom.pos.y-(upperLeft.pos.y-2*upperLeft.pos.x))/2, bottom.pos.y);
        triangle.append(topx, (leftCorner+rightCorner)/2*Math.sqrt(3)+bottom.pos.y);
        triangle.append((upperRight.pos.y+2*upperRight.pos.x-bottom.pos.y)/2, bottom.pos.y);

        System.out.println(triangle.toString());
        List<City> res = new ArrayList<City>();
        Point2D center = new Point2D.Double((leftCorner+rightCorner)/2, ((leftCorner+rightCorner)/2*Math.sqrt(3))/3);
        List<Point2D> tri_points = triangle.getPoints();
        double D = tri_points.get(0).distance(tri_points.get(1));
        for (City city : realCities) {
        		double rate = 1000.0/D;
            // expand(center, city, triangle);
            res.add(expand(center, city, rate));
        }
        return res;
    }
    
    private City expand(Point2D center, City city, double r) {
    		City c = new City(new Point2D.Double(city.pos.x, city.pos.y));
    		c.pos.x -= center.getX();
    		c.pos.y -= center.getY();
    		c.pos.x *= r;
    		c.pos.y *= r;
    		c.pos.x += center.getX();
    		c.pos.y += center.getY();
    		return c;
    }

    private City shift(Point2D center, City city, Polygon2D triangle) {
        List<Point2D> points = triangle.getPoints();
        double xDiff = center.getX()-500; double yDiff = center.getY()-500*Math.sqrt(3)/3;
        return new City(new Point2D.Double(city.pos.x-xDiff, city.pos.y-yDiff));
    }

    private City expand(Point2D center, City city, Polygon2D triangle) {
        List<Point2D> points = triangle.getPoints();
        double dist = Integer.MAX_VALUE;
        Line2D line = new Line2D.Double();
        for (Point2D point1 : points) {
            for (Point2D point2: points) {
                if (point1 != point2) {
                    Line2D cand = new Line2D.Double(point1, point2);
                    double curr = cand.ptLineDist(new Point2D.Double(city.pos.x, city.pos.y));
                    if (curr < dist) {
                        line = cand;
                        dist = curr;
                    }
                }
            }
        }
        double center_to_city = center.distance(city.pos);
        double center_to_line = line.ptLineDist(center);
        double center_to_realline = 500*Math.sqrt(3)/3;

        double A = center.getX()-city.pos.x; double B = center.getY()-city.pos.y;
        double X = center_to_realline/center_to_line*Math.sqrt(Math.pow(A, 2)+Math.pow(B, 2));
        System.out.println(X);
        System.out.println(center.getX()-Math.sqrt(Math.pow(X, 2)/((1+Math.pow(B, 2)/Math.pow(A, 2)))));
        System.out.println(center.getY()-Math.sqrt(Math.pow(X, 2)/((1+Math.pow(A, 2)/Math.pow(B, 2)))));
        return city;
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

        List<City> realCities = new ArrayList<City>();
        realCities.add(new City(new Point2D.Double(40.6635, 73.9387)));
        realCities.add(new City(new Point2D.Double(34.0194, 118.4108)));
        realCities.add(new City(new Point2D.Double(41.8376, 87.6818)));
        realCities.add(new City(new Point2D.Double(29.7866, 95.3909)));
        realCities.add(new City(new Point2D.Double(33.5722, 112.0901)));
        realCities.add(new City(new Point2D.Double(40.0094, 75.1333)));
        realCities.add(new City(new Point2D.Double(29.4724, 98.5251)));
        realCities.add(new City(new Point2D.Double(32.8153, 117.1350)));
        realCities.add(new City(new Point2D.Double(32.7933, 96.7665)));
        realCities.add(new City(new Point2D.Double(37.2967, 121.8189)));
        List<City> cities = scale(realCities);
        System.out.println(cities);


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
            City closestCity = null;
            for (City city : cities) {
                double distToCity = city.getDist(pos);
                if (distToCity < distToClosestCity) {
            
                    distToClosestCity = distToCity;
                    closestCity = city;
                }
                
            }
            System.out.println(closestCity);

            // The closer to the closest city, the higher the chance a voter is generated there
            if (random.nextDouble() > distToClosestCity/(random.nextGaussian()*50+100)) {
                List<Double> pref = new ArrayList<Double>();
                pref.add(random.nextDouble() * closestCity.getP1Bias());
                pref.add(random.nextDouble() * closestCity.getP2Bias());
                for (int j = 0; j < numParties - 2; ++j)
                    pref.add(random.nextDouble() * 0.2);

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
