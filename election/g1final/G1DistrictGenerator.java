package election;

import java.util.*;

import com.seisw.util.geom.Poly;
import election.Polygon2D;
import election.Subtraction;
import election.Voter;
import javafx.util.Pair;
import java.awt.geom.*;

public class G1DistrictGenerator implements election.DistrictGenerator {
    private static final double EPSILON = 1e-7;
    private static final double KMEANS_EPSILON = 1;
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private List<Voter> votersSortedByX;
    private double voterDensity;
    private double average1District;
    private double max1District;
    private double min1District;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        this.numVoters = voters.size();
        this.numParties = voters.get(0).getPreference().size();
        this.numDistricts = 243 / repPerDistrict;
        this.voterDensity = voters.size()*1.0/(Math.sqrt(3)/4*scale*scale);
        this.average1District = 1.0*voters.size()/numDistricts;
//        this.average1District = 500;
        this.max1District = 1.1*this.average1District;
        this.min1District = 0.9*this.average1District;
        int numClusters = 81;
        int numTopClusters = 10;
        sortVotersByX(voters);
        this.votersSortedByX = new ArrayList<>(voters);

        List<Polygon2D> districts = new ArrayList<>();
//        Map<Point2D, List<Voter>> clusters = getClusters(voters, numClusters);
//        List<Polygon2D> squareDistricts = new ArrayList<>();
//
//        for (Map.Entry<Point2D, List<Voter>> cluster : clusters.entrySet()) {
//            Polygon2D squareDistrict = generateSquareDistrict(cluster, squareDistricts);
//            squareDistricts.add(squareDistrict);
//        }
//
//        List<Pair<Polygon2D, Double>> squareDistrictsWithPref = new ArrayList<>();
//
//        Set<Polygon2D> squareDistrictsSet = new HashSet<>(squareDistricts);
//        for (Polygon2D squareDistrict : squareDistricts) {
//            // We know it is a square. Otherwise we're in trouble.
//            List<Voter> squareDistrictVoters = getVotersInSquare(squareDistrict);
//            double rank = calculateVoterPref(squareDistrictVoters);
//            System.out.println("RANK: " + rank);
//            squareDistrictsWithPref.add(new Pair(squareDistrict, rank));
//            // rank the square based on voter disparity, the percent of voters who vote in our party's favor
//            // note that if there are 243 districts this is 52/53%, but if there are 81 districts it's more complicated, could be 77/53/27% or so
//        }

//        sortDistrictsByPref(squareDistrictsWithPref);
//        List<Pair<Polygon2D, Double>> topSquareDistrictPrefs = squareDistrictsWithPref.subList(0, numTopClusters);
//        for (Pair<Polygon2D, Double> pair : topSquareDistrictPrefs) {
//            System.out.println("Pref: " + pair.getValue());
//        }

//        Nazli's code generates these square districts based on density.
//        List<Polygon2D> topSquareDistricts = topSquareDistrictPrefs.stream().map(d -> d.getKey()).collect(Collectors.toList());
//        for(int i = 0; i < topSquareDistricts.size(); i++){
//            Polygon2D topSquareDistrict = topSquareDistricts.get(i);
//            topSquareDistricts.set(i, resizeSquareDistrict(topSquareDistrict, topSquareDistricts));
//        }
        List<Polygon2D> topSquareDistricts = getNazlisSquareDistricts();

//        List<Polygon2D> topSquareDistrictsInner = new ArrayList<>();
//        for (Polygon2D squareDistrict : topSquareDistricts) {
//            List<Voter> squareDistrictVoters = getVotersInSquare(squareDistrict);
//            topSquareDistrictsInner.addAll(generateInnerDistricts(squareDistrict, squareDistrictVoters, 0));
//        }

        // top clusters are the smallest areas, most likely indicative of cities

//        int numRemainingDistricts = this.numDistricts - topSquareDistrictsInner.size();
        int numRemainingDistricts = 81-39; // we know this from generating inner districts
        List<Polygon2D> outerDistricts = generateOuterDistricts(numRemainingDistricts, topSquareDistricts, voters);
        return outerDistricts;
    }

    private boolean isSplittableDistrict(int numVoters, int numDistricts) {
        int min1District = (int) Math.ceil(average1District*0.9);
        int max1District = (int) Math.floor(average1District*1.1);
        int votersPerDistrict = (int) ((double) numVoters)/numDistricts;
        if (min1District < votersPerDistrict && votersPerDistrict < max1District) return true;
        return false;
    }

    private boolean isSplittableDistrict(int numVoters) {
        System.out.println("checking if district is splittable with numVoters: " + numVoters);
        int numTotalVoters = votersSortedByX.size();
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
        if (!isSplittableDistrict(voters.size())) {
            throw new RuntimeException("Was expecting splittable population");
            // innerDistricts.add(squareDistrict);
            // return innerDistricts;
        }
        double allVoterPref = calculateVoterPref(voters);
        int numPreferredDistricts = 0;
        if (allVoterPref > 0.5) {
            numPreferredDistricts = (int) (1.0*voters.size()/(0.9*average1District));
        } else {
            numPreferredDistricts = (int) (1.0*voters.size()/(1.1*average1District)) + 1;
        }
        System.out.println("With voters = " + voters.size() + " prefer " + numPreferredDistricts + " districts.");

        if (numPreferredDistricts == 1) {
            innerDistricts.add(squareDistrict);
            return innerDistricts;
        }
        // average preferred
        int numPreferredVotersPerDistrict = (int) ((double) voters.size())/numPreferredDistricts;
        Set<Voter> votersSet = new HashSet<>(voters);
        int numRemainingVoters = votersSet.size();

        double originalTheta = 0.0;
        double previousTheta = originalTheta;
        int stepSize = 1000;

        for (int i = 0; i < numPreferredDistricts-1; i++) {
            int minVotersForRemaining = numRemainingVoters - (int) max1District*(numPreferredDistricts-i-1);
            minVotersForRemaining = minVotersForRemaining < min1District ? (int) min1District + 1 : minVotersForRemaining;
            minVotersForRemaining = minVotersForRemaining > max1District ? (int) max1District : minVotersForRemaining;
            int maxVotersForRemaining = numRemainingVoters - (int) min1District*(numPreferredDistricts-i-1);
            maxVotersForRemaining = maxVotersForRemaining < min1District ? (int) min1District + 1 : maxVotersForRemaining;
            maxVotersForRemaining = maxVotersForRemaining > max1District ? (int) max1District : maxVotersForRemaining;

            Polygon2D polygon;
            if (maxVotersForRemaining - minVotersForRemaining < 25) {
                double phi = binarySearchTheta(squareDistrict, previousTheta, voters, (minVotersForRemaining + maxVotersForRemaining)/2);
                polygon = getInnerDistrictPolygon(squareDistrict, previousTheta, phi);
                innerDistricts.add(polygon);
                Set<Voter> votersInPolygon = getVotersInPolygon(polygon, votersSet);
                numRemainingVoters -= votersInPolygon.size();
                previousTheta = phi;
            } else {
                double thetaMin = binarySearchTheta(squareDistrict, previousTheta, voters, minVotersForRemaining);
                double thetaMax = binarySearchTheta(squareDistrict, previousTheta, voters, maxVotersForRemaining);
                System.out.println("ThetaMin: " + thetaMin + " ThetaMax: " + thetaMax);
                double theta = thetaMin;
                double thetaDiff = (thetaMax-thetaMin)/stepSize;
                List<Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>>> rankedThetas = new ArrayList<>();
                while (theta <= thetaMax) {
                    polygon = getInnerDistrictPolygon(squareDistrict, previousTheta, theta);
                    Set<Voter> votersInPolygon = getVotersInPolygon(polygon, votersSet);
                    double voterPref = calculateVoterPref(votersInPolygon);
                    rankedThetas.add(new Pair<>(new Pair<>(polygon, theta), new Pair<>(votersInPolygon.size(), voterPref)));
                    theta += thetaDiff;
                }
                sortRankedThetas(rankedThetas, allVoterPref, minVotersForRemaining, maxVotersForRemaining);
                Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>> bestScore = rankedThetas.get(0);
                System.out.println("Best Result! Voter Size: " + bestScore.getValue().getKey() + " Theta: " + bestScore.getKey().getValue() + " Voter Pref: " + bestScore.getValue().getValue());
                innerDistricts.add(bestScore.getKey().getKey());
                numRemainingVoters -= bestScore.getValue().getKey();
                previousTheta = bestScore.getKey().getValue();
            }
        }
        Polygon2D lastPolygon = getInnerDistrictPolygon(squareDistrict, previousTheta, originalTheta);
        innerDistricts.add(lastPolygon);
        return innerDistricts;
    }

    private void sortRankedThetas(List<Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>>> rankedThetas, double allVoterPref, int minVotersForRemaining, int maxVotersForRemaining) {
        Collections.sort(rankedThetas, new Comparator<Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>>>() {
            @Override
            public int compare(Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>> p1, Pair<Pair<Polygon2D, Double>, Pair<Integer, Double>> p2) {
                int p1NumVoters = p1.getValue().getKey();
                double p1VoterPref = p1.getValue().getValue();
                int p2NumVoters = p2.getValue().getKey();
                double p2VoterPref = p2.getValue().getValue();

                double minOverMax = ((double) minVotersForRemaining)/((double) maxVotersForRemaining);
                double p1NumVotersFrac = ((double) p1NumVoters)/((double) maxVotersForRemaining);
                double p2NumVotersFrac = ((double) p2NumVoters)/((double) maxVotersForRemaining);

                double p1NumVoterScore = Math.pow(Math.max(1-p1NumVotersFrac, p1NumVotersFrac - minOverMax), 2);
                double p2NumVoterScore = Math.pow(Math.max(1-p2NumVotersFrac, p2NumVotersFrac - minOverMax), 2);

                double p1VoterPrefScore = Math.pow(allVoterPref - p1VoterPref, 2);
                double p2VoterPrefScore = Math.pow(allVoterPref - p2VoterPref, 2);

                double p1Score = p1VoterPrefScore * p1NumVoterScore;
                double p2Score = p2VoterPrefScore * p2NumVoterScore;

                // LOWER IS BETTER
                if (p1Score < p2Score) return -1;
                else if (p1Score > p2Score) return 1;
                else return 0;
            }
        });
    }

    private Set<Voter> getVotersInPolygon(Polygon2D polygon, Set<Voter> voters) {
        Set<Voter> votersInPolygon = new HashSet<>();
        for (Voter voter : voters) {
            if (polygon.contains(voter.getLocation())) votersInPolygon.add(voter);
        }
        return votersInPolygon;
    }

    // binary search over theta for the target population size.
    private double binarySearchTheta(Polygon2D squareDistrict, double theta0, List<Voter> voters, int targetPopulationSize) {
        double thetaMin = theta0;
        double thetaMax = theta0 + 2*Math.PI;
        double thetaAvg = (thetaMin + thetaMax)/2;
        double thetaDelta = thetaMax - thetaMin;
        Polygon2D nextPolygon;
        while(thetaDelta > EPSILON) {
            nextPolygon = getInnerDistrictPolygon(squareDistrict, theta0, thetaAvg);
            int populationSize = 0;
            for (Voter voter : voters) {
                if (nextPolygon.contains(voter.getLocation())) {
                    populationSize++;
                }
            }
            if (populationSize < targetPopulationSize) {
                thetaMin = thetaAvg;
            } else if (populationSize > targetPopulationSize) {
                thetaMax = thetaAvg;
            } else {
                thetaMax = thetaAvg;
                thetaMin = thetaAvg;
            }
            thetaDelta = thetaMax - thetaMin;
            thetaAvg = (thetaMin + thetaMax)/2;
        }
        return thetaMin;
    }

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
        } else if (9*Math.PI/4 <= theta && theta < 11*Math.PI/4) {
            return 1;
        } else if (11*Math.PI/4 <= theta && theta < 13*Math.PI/4) {
            return 2;
        } else if (13*Math.PI/4 <= theta && theta < 15*Math.PI/4) {
            return 3;
        } else if (15*Math.PI/4 <= theta && theta < 17*Math.PI/4) {
            return 0;
        } else {
            System.out.println("Theta: " + theta);
            throw new RuntimeException("quadrant is not any of the first four, 0/1/2/3 !?!");
        }
    }

    private List<Polygon2D> generateOuterDistricts(int numRemainingDistricts, List<Polygon2D> squareDistricts, List<Voter> voters) {
        List<Polygon2D> outerRegions = generateOuterRegions(squareDistricts);
        List<Polygon2D> outerRegionsValidSides = generateOuterRegionsValidSize(outerRegions);
//        List<Polygon2D> outerRegionsValidSidesAndNumDistricts = generateOuterRegionsValidSidesAndNumDistricts(numRemainingDistricts, outerRegionsValidSides, voters);
//        return outerRegionsValidSidesAndNumDistricts;
        return outerRegionsValidSides;
    }

    private List<Polygon2D> generateOuterRegions(List<Polygon2D> squareDistricts) {
        List<Polygon2D> outerRegions = new ArrayList<>();
        sortSquareDistricts(squareDistricts);

        for (Polygon2D squareDistrict : squareDistricts) {
            Polygon2D topRightCorner = new Polygon2D();
            double rightSide = getRightSide(squareDistrict);
            double topSide = getTopSide(squareDistrict);
            double leftSide = topSide/Math.sqrt(3);
            topRightCorner.append(rightSide, topSide);
            topRightCorner.append(leftSide, topSide);
            topRightCorner.append(0.0, 0.0);
            topRightCorner.append(rightSide, 0.0);
            outerRegions.add(topRightCorner);
        }
        Polygon2D wholeTriangle = getWholeTriangle();
        outerRegions.add(wholeTriangle);

        ArrayList<Polygon2D> newOuterRegions = new ArrayList<>();
        for (int i = 0; i < outerRegions.size(); i++) {
            Polygon2D outerRegion = outerRegions.get(i);
            for (int j = 0; j < i; j++) {
                outerRegion = Subtraction.subtract(outerRegion, outerRegions.get(j));
            }
            // remove the square district itself from the largest square as well, does not apply to the whole triangle.
            if (i < squareDistricts.size()) {
                outerRegion = Subtraction.subtract(outerRegion, squareDistricts.get(i));
            }
            newOuterRegions.add(outerRegion);
        }
        return newOuterRegions;
    }

    private List<Polygon2D> generateOuterRegionsValidSize(List<Polygon2D> outerRegions) {
        for (int i = 0; i < outerRegions.size(); i++) {
            // if it has more than 9 sides, split it at a corner on 1 side.
        }
        return outerRegions;
    }

    private List<Polygon2D> generateOuterRegionsValidSidesAndNumDistricts(int numRemainingDistricts, List<Polygon2D> outerRegions, List<Voter> voters) {
        Map<Polygon2D, List<Voter>> outerDistrictsToVoters = new HashMap<>();
        for (int i = 0; i < outerRegions.size(); i++) {
            Polygon2D outerDistrict = outerRegions.get(i);
            outerDistrictsToVoters.put(outerDistrict, new ArrayList<>());
            for (Voter voter : voters) {
                if (outerRegions.get(i).contains(voter.getLocation())) {
                    outerDistrictsToVoters.get(outerDistrict).add(voter);
                }
            }
        }

        Map<Polygon2D, Integer> outerDistrictsToNumDistricts = new HashMap<>();
        int numOuterDistricts = 0;
        for (Map.Entry<Polygon2D, List<Voter>> entry : outerDistrictsToVoters.entrySet()) {
            int numDistricts = (int) Math.round(entry.getValue().size()/average1District);
            numOuterDistricts += numDistricts;
            outerDistrictsToNumDistricts.put(entry.getKey(), numDistricts);
        }

        while (numOuterDistricts != numRemainingDistricts) {
            if (numOuterDistricts < numRemainingDistricts) {
                for (Map.Entry<Polygon2D, List<Voter>> entry : outerDistrictsToVoters.entrySet()) {
                    int currentNumDistricts = outerDistrictsToNumDistricts.get(entry.getKey());
                    if (isSplittableDistrict(entry.getValue().size(), currentNumDistricts + 1)) {
                        outerDistrictsToNumDistricts.put(entry.getKey(), currentNumDistricts + 1);
                        numOuterDistricts += 1;
                    }
                }
            } else if (numOuterDistricts > numRemainingDistricts) {
                for (Map.Entry<Polygon2D, List<Voter>> entry : outerDistrictsToVoters.entrySet()) {
                    int currentNumDistricts = outerDistrictsToNumDistricts.get(entry.getKey());
                    if (isSplittableDistrict(entry.getValue().size(), currentNumDistricts - 1)) {
                        outerDistrictsToNumDistricts.put(entry.getKey(), currentNumDistricts - 1);
                        numOuterDistricts -= 1;
                    }
                }
            }
        }

        List<Polygon2D> result = new ArrayList<>();
        for (Map.Entry<Polygon2D, Integer> region : outerDistrictsToNumDistricts.entrySet()) {
            result.addAll(splitVertically(region.getKey(), outerDistrictsToVoters.get(region.getKey()), region.getValue()));
        }
        return result;
    }

    private List<Polygon2D> splitVertically(Polygon2D polygon, List<Voter> voters, int numDistricts) {
        double maxX = 0;
        double minX = 1000;
        for (Point2D point : polygon.getPoints()) {
            if (point.getX() > maxX) maxX = point.getX();
            if (point.getX() < minX) minX = point.getX();
        }
        List<Polygon2D> result = new ArrayList<>();
        int averagePopulation = (int) ((double) voters.size())/numDistricts;
        sortVotersByX(voters);
        List<Pair<Double, Double>> xRanges = new ArrayList<>();
        double start = minX;
        double end;
        for (int i = 0; i < numDistricts-1; i++) {
            end = voters.get((i+1)*averagePopulation).getLocation().getX() + 1e-4;
            xRanges.add(new Pair<>(start, end));
            start = end;
        }
        xRanges.add(new Pair<>(start, maxX));

        for (Pair<Double, Double> xRange : xRanges) {
            double xMin = xRange.getKey();
            double xMax = xRange.getValue();
            Polygon2D leftPolygon = new Polygon2D();
            leftPolygon.append(xMin, 1000.0);
            leftPolygon.append(0.0, 1000.0);
            leftPolygon.append(0.0, 0.0);
            leftPolygon.append(xMin, 0.0);
            Polygon2D rightPolygon = new Polygon2D();
            rightPolygon.append(1000.0, 1000.0);
            rightPolygon.append(xMax, 1000.0);
            rightPolygon.append(xMax, 0.0);
            rightPolygon.append(1000.0, 0);
            Polygon2D slice = Subtraction.subtract(polygon, leftPolygon);
            slice = Subtraction.subtract(slice, rightPolygon);
            result.add(slice);
        }
        return result;
    }

    private Polygon2D getWholeTriangle() {
        Polygon2D wholeTriangle = new Polygon2D();
        wholeTriangle.append(500.0, 500*Math.sqrt(3));
        wholeTriangle.append(0.0, 0.0);
        wholeTriangle.append(1000.0, 0.0);
        return wholeTriangle;
    }

    private List<Pair<Point2D, Point2D>> createEdgeList(List<Point2D> points) {
        List<Pair<Point2D, Point2D>> edges = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get((i+1)%points.size());
            edges.add(new Pair<Point2D, Point2D>(new Point2D.Double(p1.getX(), p1.getY()), new Point2D.Double(p2.getX(), p2.getY())));
        }
        return edges;
    }

    private double getDistrictTargetPercentage(Polygon2D squareDistrict){
        List<Voter> voters = getVotersInSquare(squareDistrict);
        double pref = calculateVoterPref(voters);
        if(pref > .71) return 0.75;
        if(pref > .47) return 0.50;
        if(pref > 0.10) return 0.25;
        return 0;
    }

    private Polygon2D resizeSquareDistrict(Polygon2D squareDistrict, List<Polygon2D> squareDistricts) {
        return resizeSquareDistrict(squareDistrict, new HashSet<>(squareDistricts));
    }

    private Polygon2D resizeSquareDistrict(Polygon2D squareDistrict, Set<Polygon2D> squareDistricts) {
        // increase or decrease the size of the square, subject to the constraints of other square districts, or a reasonable maximum.
        Point2D center = getCenter(squareDistrict);

        Polygon2D newSquareDistrict = new Polygon2D();

        double x1 = squareDistrict.getPoints().get(0).getX();
        double x2 = squareDistrict.getPoints().get(2).getX();
        double y1 = squareDistrict.getPoints().get(0).getY();
        double y2 = squareDistrict.getPoints().get(2).getY();

        if (x1 > x2) { double x = x1; x1 = x2; x2 = x;} //make sure x1 < x2
        if (y1 > y2) { double y = y1; y1 = y2; y2 = y;} //make sure y1 < y2

        newSquareDistrict.append(new Point2D.Double(x1, y1));
        newSquareDistrict.append(new Point2D.Double(x1, y2));
        newSquareDistrict.append(new Point2D.Double(x2, y2));
        newSquareDistrict.append(new Point2D.Double(x2, y1));

        double delta = 0.25;
        double base = Math.abs(x1 - x2);
        double optimalBase = base;
        double optimalRate = Double.MAX_VALUE;

        double target = getDistrictTargetPercentage(squareDistrict);

        List<Voter> squareDistrictVoters = getVotersInSquare(newSquareDistrict);

        //decrease
        while(isSplittableDistrict(squareDistrictVoters.size())){
            x1 -= delta;
            x2 -= delta;
            y1 -= delta;
            x2 -= delta;

            Polygon2D tmpSquareDistrict = new Polygon2D();

            tmpSquareDistrict.append(new Point2D.Double(x1, y1));
            tmpSquareDistrict.append(new Point2D.Double(x1, y2));
            tmpSquareDistrict.append(new Point2D.Double(x2, y2));
            tmpSquareDistrict.append(new Point2D.Double(x2, y1));

            newSquareDistrict = tmpSquareDistrict;
            squareDistrictVoters = getVotersInSquare(newSquareDistrict);
            double curRate = calculateVoterPref(squareDistrictVoters);
            if(curRate < optimalRate && curRate > target){
                optimalRate = curRate;
                optimalBase = Math.abs(x1 - x2);
            }
            else if(curRate > optimalRate && curRate < optimalRate){
                optimalRate = curRate;
                optimalBase = Math.abs(x1 - x2);
            }
        }

        //increase
        x1 = squareDistrict.getPoints().get(0).getX();
        x2 = squareDistrict.getPoints().get(2).getX();
        y1 = squareDistrict.getPoints().get(0).getY();
        y2 = squareDistrict.getPoints().get(2).getY();

        if (x1 > x2) { double x = x1; x1 = x2; x2 = x;} //make sure x1 < x2
        if (y1 > y2) { double y = y1; y1 = y2; y2 = y;} //make sure y1 < y2

        newSquareDistrict.append(new Point2D.Double(x1, y1));
        newSquareDistrict.append(new Point2D.Double(x1, y2));
        newSquareDistrict.append(new Point2D.Double(x2, y2));
        newSquareDistrict.append(new Point2D.Double(x2, y1));

        double minimumDistance = getMinimumDistance(center, squareDistricts);
        System.out.println("MinimumDistance: " + minimumDistance);
        double diagonal = Math.sqrt((Math.pow(Math.abs(center.getX() - x1), 2) + Math.pow(Math.abs(center.getY() - y1), 2)));
        while(diagonal < minimumDistance){
            x1 += delta;
            x2 += delta;
            y1 += delta;
            x2 += delta;

            Polygon2D tmpSquareDistrict = new Polygon2D();

            tmpSquareDistrict.append(new Point2D.Double(x1, y1));
            tmpSquareDistrict.append(new Point2D.Double(x1, y2));
            tmpSquareDistrict.append(new Point2D.Double(x2, y2));
            tmpSquareDistrict.append(new Point2D.Double(x2, y1));

            newSquareDistrict = tmpSquareDistrict;
            diagonal = Math.sqrt((Math.pow(Math.abs(center.getX() - x1), 2) + Math.pow(Math.abs(center.getY() - y1), 2)));
            squareDistrictVoters = getVotersInSquare(newSquareDistrict);
            double curRate = calculateVoterPref(squareDistrictVoters);
            if(curRate < optimalRate && curRate > target){
                optimalRate = curRate;
                optimalBase = Math.abs(x1 - x2);
            }
            else if(curRate > optimalRate && curRate < optimalRate){
                optimalRate = curRate;
                optimalBase = Math.abs(x1 - x2);
            }
        }

        Polygon2D resultSquareDistrict = new Polygon2D();
        resultSquareDistrict.append(center.getX() - base / 2, center.getY() - base / 2);
        resultSquareDistrict.append(center.getX() - base / 2, center.getY() + base / 2);
        resultSquareDistrict.append(center.getX() + base / 2, center.getY() + base / 2);
        resultSquareDistrict.append(center.getX() + base / 2, center.getY() - base / 2);
        return resultSquareDistrict;
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

    private boolean approxEquals(double d1, double d2) {
        return Math.abs(d1-d2) < EPSILON;
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

    private double calculateVoterPref(Set<Voter> voters) {
        return calculateVoterPref(new ArrayList<>(voters));
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

    private List<Voter> getVotersInSquare(Polygon2D squareDistrict) {
        List<Voter> result = new ArrayList<Voter>();
        for(Voter v : votersSortedByX){
            if(squareDistrict.contains(v.getLocation())){
                result.add(v);
            }
        }
        return result;
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

    private double getLeftSide(Polygon2D squareDistrict) {
        Point2D c1 = squareDistrict.getPoints().get(0);
        Point2D c2 = squareDistrict.getPoints().get(2);
        return Math.min(c1.getX(), c2.getX());
    }

    private double getRightSide(Polygon2D squareDistrict) {
        Point2D c1 = squareDistrict.getPoints().get(0);
        Point2D c2 = squareDistrict.getPoints().get(2);
        return Math.max(c1.getX(), c2.getX());
    }

    private double getBottomSide(Polygon2D squareDistrict) {
        Point2D c1 = squareDistrict.getPoints().get(0);
        Point2D c2 = squareDistrict.getPoints().get(2);
        return Math.min(c1.getY(), c2.getY());
    }

    private double getTopSide(Polygon2D squareDistrict) {
        Point2D c1 = squareDistrict.getPoints().get(0);
        Point2D c2 = squareDistrict.getPoints().get(2);
        return Math.max(c1.getY(), c2.getY());
    }

    private boolean isWeaklyLeft(Point2D p1c1, Point2D p1c2, Point2D p2c1, Point2D p2c2) {
        double maxX1 = Math.max(p1c1.getX(), p1c2.getX());
        double maxX2 = Math.max(p2c1.getX(), p2c2.getX());
        return maxX1 < maxX2;
    }

    private boolean isStrictlyLeft(Point2D p1c1, Point2D p1c2, Point2D p2c1, Point2D p2c2) {
        double maxX1 = Math.max(p1c1.getX(), p1c2.getX());
        double minX2 = Math.min(p2c1.getX(), p2c2.getX());
        return maxX1 < minX2;
    }

    private boolean isWeaklyBelow(Point2D p1c1, Point2D p1c2, Point2D p2c1, Point2D p2c2) {
        double maxY1 = Math.max(p1c1.getY(), p1c2.getY());
        double maxY2 = Math.max(p2c1.getY(), p2c2.getY());
        return maxY1 <= maxY2;
    }

    private boolean isStrictlyBelow(Point2D p1c1, Point2D p1c2, Point2D p2c1, Point2D p2c2) {
        double maxY1 = Math.max(p1c1.getY(), p1c2.getY());
        double minY2 = Math.min(p2c1.getY(), p2c2.getY());
        return maxY1 <= minY2;
    }

    private void sortSquareDistricts(List<Polygon2D> squareDistricts) {
        Collections.sort(squareDistricts, new Comparator<Polygon2D> (){
            @Override
            public int compare(Polygon2D p1, Polygon2D p2) {
                Point2D p1c1 = p1.getPoints().get(0);
                Point2D p1c2 = p1.getPoints().get(2);
                Point2D p2c1 = p2.getPoints().get(0);
                Point2D p2c2 = p2.getPoints().get(2);

                if (isStrictlyLeft(p1c1, p1c2, p2c1, p2c2)) return -1;
                else if (isStrictlyLeft(p2c1, p2c2, p1c1, p1c2)) return 1;
                else {
                    if (isStrictlyBelow(p1c1, p1c2, p2c1, p2c2)) return -1;
                    else if (isStrictlyBelow(p2c1, p2c2, p1c1, p1c2)) return 1;
                    else throw new RuntimeException("Square neither left or below another square?");
                }
            }
        });
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

    private List<Polygon2D> getNazlisSquareDistricts() {
//        MUST CONSTRUCT SQUARES IN CORRECT ORDER, FROM TOP RIGHT, TOP LEFT, BOTTOM LEFT, BOTTOM RIGHT
//        NAZLI IS DOING BOTTOM LEFT, BOTTOM RIGHT, TOP RIGHT, TOP LEFT
        List<Polygon2D> polygons = new ArrayList<>();
//        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{425.0,95.0 ,335.0,95.0 ,335.0,5.0  ,425.0,5.0  }))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{847.5, 257.5, 612.5, 257.5, 612.5, 22.5 , 847.5, 22.5 }))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{347.5, 452.5, 262.5, 452.5, 262.5, 367.5, 347.5, 367.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{482.5, 632.5, 367.5, 632.5, 367.5, 517.5, 482.5, 517.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{432.5, 107.5, 327.5, 107.5, 327.5, 2.5  , 432.5, 2.5  }))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{272.5, 262.5, 197.5, 262.5, 197.5, 187.5, 272.5, 187.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{402.5, 342.5, 327.5, 342.5, 327.5, 267.5, 402.5, 267.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{677.5, 522.5, 602.5, 522.5, 602.5, 447.5, 677.5, 447.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{637.5, 597.5, 562.5, 597.5, 562.5, 522.5, 637.5, 522.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{607.5, 137.5, 512.5, 137.5, 512.5, 42.5 , 607.5, 42.5 }))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{497.5, 477.5, 432.5, 477.5, 432.5, 412.5, 497.5, 412.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{497.5, 742.5, 432.5, 742.5, 432.5, 677.5, 497.5, 677.5}))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{272.5, 157.5, 187.5, 157.5, 187.5, 72.5 , 272.5, 72.5 }))));
        polygons.add(createPolygon(new ArrayList<>(Arrays.asList(new Double[]{352.5, 187.5, 277.5, 187.5, 277.5, 112.5, 352.5, 112.5}))));

        return polygons;
    }

    private Polygon2D createPolygon(List<Double> doubles) {
        Polygon2D polygon = new Polygon2D();
        for (int i = 0; i < doubles.size(); i=i+2) {
            polygon.append(new Point2D.Double(doubles.get(i), doubles.get(i+1)));
        }
        return polygon;
    }
}
