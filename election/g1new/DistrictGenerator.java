package election.g1new;

import java.util.*;
import java.util.stream.*;
import election.sim.*;
import java.awt.geom.*;
import javafx.util.Pair;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private static final double KMEANS_EPSILON = 1;
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private List<Voter> votersSortedByX;
    private double voterDensity;
    private double average1District;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        this.numVoters = voters.size();
        this.numParties = voters.get(0).getPreference().size();
        this.numDistricts = 243 / repPerDistrict;
        this.voterDensity = voters.size()*1.0/(Math.sqrt(3)/4*scale*scale);
        this.average1District = 1.0*voters.size()/numDistricts;
        int numClusters = 200;
        int numTopClusters = 10;
        sortVotersByX(voters);
        this.votersSortedByX = new ArrayList<>(voters);

        List<Polygon2D> districts = new ArrayList<>();
        Map<Point2D, List<Voter>> clusters = getClusters(voters, numClusters);
        List<Polygon2D> squareDistricts = new ArrayList<>();

        for (Map.Entry<Point2D, List<Voter>> cluster : clusters.entrySet()) {
            Polygon2D squareDistrict = generateSquareDistrict(cluster, squareDistricts);
            squareDistricts.add(squareDistrict);
        }

        List<Pair<Polygon2D, Double>> squareDistrictsWithPref = new ArrayList<>();

        Set<Polygon2D> squareDistrictsSet = new HashSet<>(squareDistricts);
        for (Polygon2D squareDistrict : squareDistricts) {
            // We know it is a square. Otherwise we're in trouble.
            squareDistrictsSet.remove(squareDistrict);
            Polygon2D resizedSquareDistrict = resizeSquareDistrict(squareDistrict, squareDistrictsSet);
            squareDistrictsSet.add(resizedSquareDistrict);
            List<Voter> squareDistrictVoters = getVotersBetweenBrute(resizedSquareDistrict);
            double rank = calculateVoterPref(squareDistrictVoters);
            squareDistrictsWithPref.add(new Pair(squareDistrict, rank));
            // rank the square based on voter disparity, the percent of voters who vote in our party's favor
            // note that if there are 243 districts this is 52/53%, but if there are 81 districts it's more complicated, could be 77/53/27% or so
        }

        sortDistrictsByPref(squareDistrictsWithPref);
        List<Pair<Polygon2D, Double>> topSquareDistrictPrefs = squareDistrictsWithPref.subList(0, numTopClusters);
        for (Pair<Polygon2D, Double> pair : topSquareDistrictPrefs) {
            System.out.println("Pref: " + pair.getValue());
        }
        List<Polygon2D> topSquareDistricts = topSquareDistrictPrefs.stream().map(d -> d.getKey()).collect(Collectors.toList());

        List<Polygon2D> topSquareDistrictsInner = new ArrayList<>();
        for (Polygon2D squareDistrict : topSquareDistricts) {
            List<Voter> squareDistrictVoters = getVotersBetweenBrute(squareDistrict);
            topSquareDistrictsInner.addAll(generateInnerDistricts(squareDistrict, squareDistrictVoters, 0));
        }
        // top clusters are the smallest areas, most likely indicative of cities

        List<Polygon2D> outerDistricts = generateOuterDistricts();
        return topSquareDistrictsInner;
    }

    private boolean isSplittableDistrict(int numVoters) {
        System.out.println("checking if district is splittable with numVoters: " + numVoters);
        int numTotalVoters = votersSortedByX.size();
        int min1District = (int) Math.ceil(average1District*0.9) + 1;
        int max1District = (int) Math.floor(average1District*1.1);
        double average2District = 2*average1District;
        int min2District = (int) Math.ceil(average2District*0.9) + 1;
        int max2District = (int) Math.floor(average2District*1.1);
        double average3District = 3*average1District;
        int min3District = (int) Math.ceil(average3District*0.9) + 1;
        int max3District = (int) Math.floor(average3District*1.1);
        double average4District = 4*average1District;
        int min4District = (int) Math.ceil(average4District*0.9) + 1;
        int max4District = (int) Math.floor(average4District*1.1);
        double average5District = 5*average1District;
        int min5District = (int) Math.ceil(average5District*0.9) + 1;
        int max5District = (int) Math.floor(average5District*1.1);
        if (numVoters > min1District && numVoters < max1District) return true;
        if (numVoters > min2District && numVoters < max2District) return true;
        if (numVoters > min3District && numVoters < max3District) return true;
        if (numVoters > min4District && numVoters < max4District) return true;
        if (numVoters > min5District) return true;
        return false;
    }

    // party should be 0 or 1 to indicate if you prefer the party in index 0 or index 1
    private List<Polygon2D> generateInnerDistricts(Polygon2D squareDistrict, List<Voter> voters, int party) {
        // If there is no hope to split it, do not bother
        List<Polygon2D> innerDistricts = new ArrayList<>();
        // if (!isSplittableDistrict(voters.size())) {
        //     innerDistricts.add(squareDistrict);
        //     return innerDistricts;
        // }
        double voterPref = calculateVoterPref(voters);
        int numPreferredDistricts = 0;
        if (voterPref > 0.5) {
            numPreferredDistricts = (int) (1.0*voters.size()/(0.9*average1District));
        } else {
            numPreferredDistricts = (int) (1.0*voters.size()/(1.1*average1District)) + 1;
        }
        System.out.println("With voters = " + voters.size() + " prefer " + numPreferredDistricts + " districts.");
        // Just for testing
        // DELETE THIS LATER
        if (numPreferredDistricts < 2) numPreferredDistricts = 5;
        for (int i = 0; i < numPreferredDistricts; i++) {
            double theta0 = 2*Math.PI*i/numPreferredDistricts;
            double theta1 = 2*Math.PI*(i+1)/numPreferredDistricts;
            innerDistricts.add(getInnerDistrictPolygon(squareDistrict, theta0, theta1));
        }
        return innerDistricts;
    }

    // SEARCH FOR VALID/BEST ANGLE
    // private Polygon2D generateInnerDistrict(Polygon squareDistrict, List<Voter> voters, int party, double theta0) {
    //     double minTheta = getMinimumTheta(theta, voters);
    // }

    // private double getMinimumTheta(double theta0, List<Voter> voters, int numPreferredDistricts) {
    //     double stepSize = Math.PI/(5*numPreferredDistricts);
    //     double theta = Math.PI/numPreferredDistricts;

    // }

    // private double getMaximumTheta(double theta0, double theta1) {

    // }

    private Polygon2D getInnerDistrictPolygon(Polygon2D squareDistrict, double theta0, double theta1) {
        Point2D corner0 = squareDistrict.getPoints().get(0);
        Point2D corner2 = squareDistrict.getPoints().get(2);
        List<Point2D> corners = new ArrayList<>();
        double minX = Math.min(corner0.getX(), corner2.getX());
        double maxX = Math.max(corner0.getX(), corner2.getX());
        double minY = Math.min(corner0.getY(), corner2.getY());
        double maxY = Math.max(corner0.getY(), corner2.getY());
        Point2D center = getCenter(squareDistrict);

        double m0 = Math.tan(theta0);
        double m1 = Math.tan(theta1);
        double b0 = center.getY() - m0*center.getX();
        double b1 = center.getY() - m1*center.getX();
        // compute the intersection point for theta
        Polygon2D innerDistrictPolygon = new Polygon2D();
        innerDistrictPolygon.append(new Point2D.Double(center.getX(), center.getY()));
        innerDistrictPolygon.append(getIntersectionPoint(center, minX, maxX, minY, maxY, theta0));
        int theta0Quadrant = getQuadrant(theta0);
        int theta1Quadrant = getQuadrant(theta1);
        while (theta0Quadrant != theta1Quadrant) {
            innerDistrictPolygon.append(squareDistrict.getPoints().get(theta0Quadrant));
            theta0Quadrant = (theta0Quadrant + 1) % 4;
        }
        innerDistrictPolygon.append(getIntersectionPoint(center, minX, maxX, minY, maxY, theta1));
        return innerDistrictPolygon;
    }

    private Point2D getIntersectionPoint(Point2D center, double minX, double maxX, double minY, double maxY, double theta) {
        double m = Math.tan(theta);
        double b = center.getY() - m*center.getX();
        double x, y;
        int quadrant = getQuadrant(theta);
        switch (quadrant) {
            case 0: x = maxX; y = m*x + b; break;
            case 1: y = maxY; x = (y-b)/m; break;
            case 2: x = minX; y = m*x + b; break;
            case 3: y = minY; x = (y-b)/m; break;
            default: throw new RuntimeException("should never default option");
        }
        return new Point2D.Double(x, y);
    }

    private int getQuadrant(double theta) {
        if (-Math.PI/4 <= theta && theta < Math.PI/4) {
            return 0;
        } else if (Math.PI/4 <= theta && theta < 3*Math.PI/4) {
            return 1;
        } else if (3*Math.PI/4 <= theta && theta < 5*Math.PI/4) {
            return 2;
        } else if (5*Math.PI/4 <= theta && theta < 7*Math.PI/4) {
            return 3;
        } else if (7*Math.PI/4 <= theta && theta < 9*Math.PI/4) {
            return 0;
        } else {
            System.out.println("Theta: " + theta);
            throw new RuntimeException("quadrant is not any of the first four, 0/1/2/3 !?!");
        }
    }

    private List<Polygon2D> generateOuterDistricts() {
        return new ArrayList<>();
    }

    private Polygon2D resizeSquareDistrict(Polygon2D squareDistrict, Set<Polygon2D> squareDistricts) {
        // increase or decrease the size of the square, subject to the constraints of other square districts, or a reasonable maximum.
        Point2D center = getCenter(squareDistrict);
        List<Voter> squareDistrictVoters = getVotersBetweenBrute(squareDistrict);

        double minimumDistance = getMinimumDistance(center, squareDistricts);

        return squareDistrict;
    }

    private Polygon2D generateSquareDistrict(Map.Entry<Point2D, List<Voter>> cluster, List<Polygon2D> squareDistricts) {
        // after creating these square districts, we will expand the ones where we have majority.
        Point2D center = cluster.getKey();
        List<Voter> voters = cluster.getValue();
        double minX = center.getX();
        double maxX = center.getX();
        double minY = center.getY();
        double maxY = center.getY();
        for (Voter voter : voters) {
            Point2D location = voter.getLocation();
            if (location.getX() < minX) minX = location.getX();
            if (location.getX() > maxX) maxX = location.getX();
            if (location.getY() < minY) minY = location.getY();
            if (location.getY() > maxY) maxY = location.getY();
        }
        List<Point2D> corners = new ArrayList<Point2D>();
        corners.add(new Point2D.Double(minX, minY)); // bottom left
        corners.add(new Point2D.Double(minX, maxY)); // bottom right
        corners.add(new Point2D.Double(maxX, minY)); // top left
        corners.add(new Point2D.Double(maxX, maxY)); // top right
        double smallestDiagonal = scale;
        // choose the closest corner
        for (Point2D corner : corners) {
            double distance = getDistance(center, corner);
            if (distance < smallestDiagonal) smallestDiagonal = distance;
        }
        // make sure it doesn't go outside of the triangle, in case it wasn't originally a square and you made it bigger
        // left edge: -sqrt(3)x + y = 0
        // right edge: sqrt(3)x + y - 1000sqrt(3) = 0
        // |ax0 + by0 + c|/sqrt(x^2 + y^2)
        double distanceToLeftEdge = Math.abs(-Math.sqrt(3)*center.getX() + 1*center.getY())/2.0;
        double distanceToRightEdge = Math.abs(Math.sqrt(3)*center.getX() + 1*center.getY() - 1000*Math.sqrt(3))/2.0;
        if (distanceToLeftEdge < smallestDiagonal) smallestDiagonal = distanceToLeftEdge;
        if (distanceToRightEdge < smallestDiagonal) smallestDiagonal = distanceToRightEdge;
        if (center.getY()*Math.sqrt(2) < smallestDiagonal) smallestDiagonal = center.getY()*Math.sqrt(2);
        double minimumDistance = getMinimumDistance(center, new HashSet<>(squareDistricts));
        if (minimumDistance < smallestDiagonal) smallestDiagonal = minimumDistance;

        // for (Polygon2D squareDistrict : squareDistricts) {
        //     for (Point2D point : squareDistrict.getPoints()) {
        //         double distance = getDistance(center, point);
        //         if (distance < smallestDiagonal) smallestDiagonal = distance;
        //     }
        // }

        double delta = smallestDiagonal/Math.sqrt(2);
        Polygon2D result = new Polygon2D();
        result.append(new Point2D.Double(center.getX()+delta, center.getY()+delta));
        result.append(new Point2D.Double(center.getX()-delta, center.getY()+delta));
        result.append(new Point2D.Double(center.getX()-delta, center.getY()-delta));
        result.append(new Point2D.Double(center.getX()+delta, center.getY()-delta));
        return result;
    }

    private double getMinimumDistance(Point2D center, Set<Polygon2D> squareDistricts) {
        double distance = scale;
        for (Polygon2D squareDistrict : squareDistricts) {
            for (Point2D point : squareDistrict.getPoints()) {
                double d = getDistance(center, point);
                if (d < distance) distance = d;
            }
        }
        return distance;
    }

    private static double getDistance(Point2D p1, Point2D p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    private Point2D getCenter(Polygon2D squareDistrict) {
        Point2D corner1 = squareDistrict.getPoints().get(0);
        Point2D corner2 = squareDistrict.getPoints().get(2);
        return new Point2D.Double((corner1.getX() + corner2.getX())/2, (corner1.getY() + corner2.getY())/2);
    } 

    private Map<Point2D, List<Voter>> getClusters(List<Voter> voters, int k){
        List<Point2D> centroids = randomCentroids(k);
        sortPointsByX(centroids); // convenience for printing
        System.out.println(centroids.size());
        Map<Point2D, List<Voter>> clusters = new HashMap<>();
        Map<Point2D, List<Voter>> lastState = new HashMap<>();
        int maxIterations = 10000; //arbitrary number of times
        for(int i = 0; i < maxIterations; i++){
            System.out.println("iteration: " + i);
            for(Voter voter : voters){
                Point2D centroid = nearestCentroid(voter, centroids); 
                assignToCluster(clusters, voter, centroid);
            }
            if(i == maxIterations - 1 || keySetsEqual(clusters.keySet(), lastState.keySet())){
                lastState = clusters;
                break;
            } 
            lastState = new HashMap<>(clusters);
            // System.out.println("Before relocate: " + centroids);
            centroids = relocateCentroids(clusters);
            sortPointsByX(centroids); // convenience for printing
            // System.out.println("After relocate: " + centroids);
            clusters = new HashMap<>(); //start over and loop again 
        }
        return lastState;
    }

    private boolean keySetsEqual(Set<Point2D> s1, Set<Point2D> s2) {
        // It is enough to compare the keysets to check if they are equal, because voter assignment will be the same!
        // Go through every point in s1, and make sure there's a pair in s2.
        if (s1.size() != s2.size()) return false;
        for (Point2D p1 : s1) {
            boolean paired = false;
            for (Point2D p2 : s2) {
                if (approxEquals(p1, p2)) {
                    paired = true;
                    break;
                }
            }
            if (!paired) return false;
        }
        return true;
    }

    private boolean approxEquals(Point2D p1, Point2D p2) {
        return (Math.abs(p1.getX() - p2.getX()) < KMEANS_EPSILON && Math.abs(p1.getY() - p2.getY()) < KMEANS_EPSILON);
    }

    private static List<Point2D> randomCentroids(int k){
        List<Point2D> randomKPoints = new ArrayList<>();
        int i = 0;
        while(i < k){ 
            //find k random points within the triangle
            //get random point in the box of the triangle:
            double x = Math.random() * 1001;
            double y;
            if(x > 500.0){
                y = Math.tan(Math.PI / 3)* (1000.0 - x);
            }
            else{
                y = Math.tan(Math.PI / 3) * x;
            }
            //check if point is within traingle
            if(isInside(0.0, 0.0, 1000.0, 0.0, 500.0, (500.0 * Math.sqrt(3)), x, y)){
                i++;
                randomKPoints.add(new Point2D.Double(x, y));
            }
        }
        return randomKPoints;
    }

    private static Point2D nearestCentroid(Voter voter, List<Point2D> centroids){  //WHAT IS DISTANCE
        double minimumDistance = Double.MAX_VALUE;
        Point2D nearestCentroid = null;
     
        for (Point2D centroid : centroids) {
            //double currentDistance = distance.calculate(voter.getFeatures(), centroid.getCoordinates());
            double x1 = voter.getLocation().getX();
            double y1 = voter.getLocation().getY();
            double x2 = centroid.getX();
            double y2 = centroid.getY();
            double currentDistance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

            if (currentDistance < minimumDistance) {
                minimumDistance = currentDistance;
                nearestCentroid = centroid;
            }
        }
     
        return nearestCentroid;
    }

    private static void assignToCluster(Map<Point2D, List<Voter>> clusters, Voter voter, Point2D centroid){
        clusters.compute(centroid, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
     
            list.add(voter);
            return list;
        });
    }

    private static Point2D average(List<Voter> voters) {
        int numVoters = voters.size();
        double averageX = 0;
        double averageY = 0;
        for (Voter voter : voters) {
            averageX += voter.getLocation().getX();
            averageY += voter.getLocation().getY();
        }
        return new Point2D.Double(averageX/numVoters, averageY/numVoters);
    }

    private double calculateVoterPref(List<Voter> voters){
        double first = 0.0;
        double second = 0.0;
        for(Voter v : voters){
            if(v.getPreference().get(0) > v.getPreference().get(1)) first++;
            else second++;
        }
        return first / voters.size();
    }

    private List<Voter> getVotersBetweenBrute(Polygon2D squareDistrict) {
        Point2D corner0 = squareDistrict.getPoints().get(0);
        Point2D corner2 = squareDistrict.getPoints().get(2);
        return getVotersBetweenBrute(corner0.getX(), corner2.getX(), corner0.getY(), corner2.getY());
    }

    private List<Voter> getVotersBetweenBrute(double x1, double x2, double y1, double y2){
        if (x1 > x2) { double x = x1; x1 = x2; x2 = x;}
        if (y1 > y2) { double y = y1; y1 = y2; y2 = y;}

        List<Voter> result = new ArrayList<Voter>();

        for(Voter v : votersSortedByX){
            double vx = v.getLocation().getX();
            double vy = v.getLocation().getY();
            if(vx < x2 && vx > x1 && vy < y2 && vy > y1) {
                result.add(v);
            }
        }

        return result;
    }

    private List<Voter> getVotersBetween(double x1, double x2, double y1, double y2) {
        if (x1 > x2) { double x = x1; x1 = x2; x2 = x;}
        if (y1 > y2) { double y = y1; y1 = y2; y2 = y;}
        // guarantee that x1 < x2, y1 < y2
        // efficiently get the voters based on x value. Then iterate checking y1 < v.getLocation().getY() < y2
        // alternatively just brute force check if every voter belongs.
        // Will call many times after every resizing of the box to check majority percentage.
        // TODO: IMPLEMENT
        int start = binaryVoterSearch(votersSortedByX, x1);
        int end = binaryVoterSearch(votersSortedByX, x2);
        List<Voter> yList = votersSortedByX.subList(start, end + 1);
        sortVotersByY(yList);
        start = binaryVoterSearch(yList, y1);
        end = binaryVoterSearch(yList, y2);
        return yList.subList(start, end + 1);
    }

    private int binaryVoterSearch(List<Voter> voters, double target){
        int l = 0, r = voters.size() - 1; 
        int m = l + (r - l) / 2; 
        while (l <= r) { 
            m = l + (r - l) / 2; 
            if (voters.get(m).getLocation().getX() == target) break; 
  
            if (voters.get(m).getLocation().getX() < target) l = m + 1; 
            else r = m - 1; 
        } 
        return m; 
    }

    private void sortPointsByX(List<Point2D> points) {
        Collections.sort(points, new Comparator<Point2D>() {
            @Override
            public int compare(Point2D p1, Point2D p2) {
                return p1.getX() > p2.getX() ? 1 : -1;
            }
        });
    }

    private void sortVotersByX(List<Voter> voters) {
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return v1.getLocation().getX() > v2.getLocation().getX() ? 1 : -1;
            }
        });
    }

    private void sortPointsByY(List<Point2D> points) {
        Collections.sort(points, new Comparator<Point2D>() {
            @Override
            public int compare(Point2D p1, Point2D p2) {
                return p1.getY() > p2.getY() ? 1 : -1;
            }
        });
    }
    private void sortVotersByY(List<Voter> voters) {
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return v1.getLocation().getY() > v2.getLocation().getY() ? 1 : -1;
            }
        });
    }

    private void sortDistrictsByPref(List<Pair<Polygon2D, Double>> voters) {
        Collections.sort(voters, new Comparator<Pair<Polygon2D, Double>>() {
            @Override
            public int compare(Pair<Polygon2D, Double> v1, Pair<Polygon2D, Double> v2) {
                double v1Pref = v1.getValue();
                double v2Pref = v2.getValue();

                double d1to100 = Math.abs(1.0 - v1Pref);
                double d1to75 = Math.abs(0.75 - v1Pref);
                // double d1to50 = Math.abs(50 - v1Pref);
                double d1to25 = Math.abs(0.25 - v1Pref);
                double d1to0 = Math.abs(0.0 - v1Pref);

                double d2to100 = Math.abs(1.0 - v2Pref);
                double d2to75 = Math.abs(0.75 - v2Pref);
                // double d2to50 = Math.abs(50 - v2Pref);
                double d2to25 = Math.abs(0.25 - v2Pref);
                double d2to0 = Math.abs(0.0 - v2Pref);

                double d1min = Math.min(d1to100, Math.min(d1to75, Math.min(d1to25, d1to0))); //Math.min(d1to50, 
                double d2min = Math.min(d2to100, Math.min(d2to75, Math.min(d2to25, d2to0))); //Math.min(d2to50

                if(d1min < d2min) return -1;
                else if(d1min > d2min) return 1;
                return 0;
            }
        });
    }

    private static List<Point2D> relocateCentroids(Map<Point2D, List<Voter>> clusters) {
        //go through every cluster (every Point2D given)
        List<Point2D> relocatedCentroids = new ArrayList<>();
        //for(int i = 0; i < clusters.size(); i++){
        for(Point2D point : clusters.keySet()){
            //for each one, replace that point with the new point with average() function
            relocatedCentroids.add(average(clusters.get(point)));
        }
        return relocatedCentroids;
    }
    
    //-------------------------End of K-means clustering solution-----------------------------//

    //---------------------------- Beginning of checking if point is inside triangle --------------------------------//
    //---- Taken from: https://www.geeksforgeeks.org/check-whether-a-given-point-lies-inside-a-triangle-or-not/ -----//

    private static boolean isInside(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y) 
    {    
        /* Calculate area of triangle ABC */
        double A = area (x1, y1, x2, y2, x3, y3); 

        /* Calculate area of triangle PBC */  
        double A1 = area (x, y, x2, y2, x3, y3); 

        /* Calculate area of triangle PAC */  
        double A2 = area (x1, y1, x, y, x3, y3); 

        /* Calculate area of triangle PAB */   
        double A3 = area (x1, y1, x2, y2, x, y); 

        /* Check if sum of A1, A2 and A3 is same as A */
        return (A == A1 + A2 + A3); 
    } 

    private static double area(double x1, double y1, double x2, double y2, double x3, double y3) 
    { 
        return Math.abs((x1*(y2-y3) + x2*(y3-y1)+ x3*(y1-y2))/2.0); 
    } 

    //---------------------------End of checking if point is inside triangle -------------------------------//
}
