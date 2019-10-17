package election.g3;

import election.sim.Polygon2D;
import election.sim.Voter;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 *
 * @author Group 3
 */
public class DistrictGenerator implements election.sim.DistrictGenerator {
    
    private final static double NUM_VERT_TRIANGLES = 9.0;
    private final static int NUM_CLUSTERS = 81;
    private final double scale = 1000.0;
    private Random random;
    private List<Voter> voters;
    
    public DistrictGenerator() {
        random = new Random();
    }
    
    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
    	this.voters = voters;
        int numVoters = voters.size();
        int numDistricts = 243 / repPerDistrict;
        
        // Execute K-Means
        KMeans kmeans = new KMeans(voters, NUM_CLUSTERS);
        kmeans.execute();
        List<Cluster> clusters = kmeans.getClusters();
        
        // Execute Voronoi
        List<NewPoint> centroids = new ArrayList<>();
        for(Cluster cluster : clusters) {
        	centroids.add(cluster.getCentroid());
        }
        Voronoi voronoi = new Voronoi(voters, centroids);
        voronoi.execute();
        voronoi.print();
        
        Polygon2D threeLand = new Polygon2D();
        threeLand.append(0, 0);
        threeLand.append(1000, 0);
        threeLand.append(500, 500 * Math.sqrt(3));
        try {
            List<Polygon2D> result = fixPolygon(
                    threeLand,
                    voters,
                    getMinPopulation(numVoters, numDistricts),
                    getMaxPopulation(numVoters, numDistricts),
                    1.0 / 3
            );
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    protected List<Polygon2D> fixPolygon(Polygon2D polygon, List<Voter> voters, int minPop, int maxPop, double splitProportion) throws Exception {
        List<Voter> vInD = getVotersInPolygon(voters, polygon);
        if (vInD.size() < minPop) {
            throw new Exception("Not enough people in polygon. Only " + vInD.size() + ", minimum of " + minPop + " needed");
        }
        if (vInD.size() > maxPop) {
            Point2D splitVertex = pickVertex(polygon);
            Point2D oppositePoint = searchPoint(polygon, splitVertex, vInD, (int) Math.round(vInD.size() * splitProportion), minPop * 0.05 / vInD.size());
            if (oppositePoint != null) {
                List<Point2D> otherPoints = polygon.getPoints().stream().filter((p) -> !p.equals(splitVertex)).collect(Collectors.toList());
                Polygon2D poly1 = createPolygonFromPoints(Arrays.asList(splitVertex, otherPoints.get(0), oppositePoint));
                List<Polygon2D> result = fixPolygon(
                        poly1,
                        getVotersInPolygon(voters, poly1),
                        minPop,
                        maxPop,
                        1.0 / 3
                );
                Polygon2D poly2 = createPolygonFromPoints(Arrays.asList(splitVertex, oppositePoint, otherPoints.get(1)));
                result.addAll(fixPolygon(
                        poly2,
                        getVotersInPolygon(voters, poly2),
                        minPop,
                        maxPop,
                        splitProportion == 0.5 ? 1.0 / 3 : 0.5
                ));
                return result;
            } else {
                throw new Exception("Could not split polygon properly");
            }
        } else {
            return new ArrayList(Arrays.asList(polygon));
        }
    }
    
    protected List<Voter> getVotersInPolygon(List<Voter> voters, Polygon2D polygon) {
        return voters.stream().filter((v) -> polygon.contains(v.getLocation())).collect(Collectors.toList());
    }
    
    protected Polygon2D createPolygonFromPoints(List<Point2D> points) {
        Polygon2D polygon = new Polygon2D();
        for (Point2D p : points) {
            polygon.append(p);
        }
        return polygon;
    }
    
    protected Point2D pickVertex(Polygon2D polygon) {
        List<Point2D> points = polygon.getPoints();
        Point2D selected = null;
        double len = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            final double oppSide = polygon.ptDist(points.get((i + 1) % 3), points.get((i + 2) % 3));
            if (oppSide > len) {
                len = oppSide;
                selected = points.get(i);
            }
        }
        return selected;
        //return polygon.getPoints().get(random.nextInt(3));
    }
    
    protected Point2D searchPoint(Polygon2D polygon, Point2D point, List<Voter> voters, int targetSplit, double tolerance) {
        List<Point2D> otherPoints = polygon.getPoints().stream().filter((p) -> !p.equals(point)).collect(Collectors.toList());
        final int numVoterTolerance = (int) Math.floor(voters.size() * tolerance);
        Point2D p0 = otherPoints.get(0);
        Point2D p1 = otherPoints.get(1);
        final Point2D pA = p0;
        
        boolean found = false;
        do {
            Point2D pM = getMidPoint(p0, p1);
            Polygon2D nPoly = createPolygonFromPoints(Arrays.asList(point, pA, pM));
            List<Voter> nPolyVoters = getVotersInPolygon(voters, nPoly);
            if (Math.abs(nPolyVoters.size() - targetSplit) < numVoterTolerance) {
                return pM;
            } else if (nPolyVoters.size() < targetSplit) {
                p0 = pM;
            } else {
                p1 = pM;
            }
        } while (!found);
        System.out.println("Could not find split point");
        return null;
    }
    
    protected Point2D getMidPoint(Point2D p1, Point2D p2) {
        return new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
    }
    
    protected List<Polygon2D> generateInitialPolygons() {
        List<Polygon2D> result = new ArrayList<>();
        
        double height = scale / 2.0 * Math.sqrt(3);
        double hstep = scale / NUM_VERT_TRIANGLES;
        
        for (int i = 0; i < NUM_VERT_TRIANGLES; ++i) {
            double top = height * (NUM_VERT_TRIANGLES - i) / NUM_VERT_TRIANGLES;
            double btm = top - height / NUM_VERT_TRIANGLES;
            double left = scale / 2 - hstep / 2 * (i + 1);
            for (int j = 0; j <= i; ++j) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(left + hstep * j, btm);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep / 2, top);
                result.add(polygon);
            }
            for (int j = 0; j < i; ++j) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(left + hstep * j + hstep / 2, top);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep * 3 / 2, top);
                result.add(polygon);
            }
        }
        return result;
    }
    
    protected int getMinPopulation(int numVoters, int numDistricts) {
        return (int) Math.ceil(0.9 * numVoters / numDistricts);
    }
    
    protected int getMaxPopulation(int numVoters, int numDistricts) {
        return (int) Math.floor(1.1 * numVoters / numDistricts);
    }
    
    // For testing purposes, to be used with the main method
    private static List<Voter> obtainVoters(String mapPath) throws IllegalArgumentException, FileNotFoundException {
        List<Voter> voters = new ArrayList<Voter>();
        File file = new File(mapPath);
        Scanner scanner = new Scanner(file);
        int numVoters = scanner.nextInt();
        int numParties = scanner.nextInt();
        for (int i = 0; i < numVoters; ++i) {
            double x, y;
            List<java.lang.Double> pref = new ArrayList<java.lang.Double>();
            x = scanner.nextDouble();
            y = scanner.nextDouble();
            Point2D location = new Point2D.Double(x, y);
            for (int j = 0; j < numParties; ++j)
                pref.add(scanner.nextDouble());
            voters.add(new Voter(location, pref));
        }
        return voters;
    }
    
    // For testing purposes, since the generator times out when applying KMeans
    public static void main(String[] args) {
    	String mapPath = "maps/g3/g1TwoPartyTurkey.map";
    	List<Voter> voters = null;
		try {
			voters = obtainVoters(mapPath);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
        // Execute K-Means
        KMeans kmeans = new KMeans(voters, NUM_CLUSTERS);
        
        kmeans.execute();
        List<Cluster> clusters = kmeans.getClusters();
        
        // Execute Voronoi
        List<NewPoint> centroids = new ArrayList<>();
        for(Cluster cluster : clusters) {
        	centroids.add(cluster.getCentroid());
        }
        int numCentroid = 1;
        for(NewPoint centroid : centroids) {
        	System.out.println("Centroid for cluster " + numCentroid + ": (" + centroid.getX() + ", " + centroid.getY() + ")");
        	numCentroid++;
        }
        Voronoi voronoi = new Voronoi(voters, centroids);
        voronoi.execute();
        voronoi.print();
    }
} 