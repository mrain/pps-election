package election.g1;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;
import javafx.util.Pair;

public class Municipal {
	
	private static final double EPSILON = 1e-8;
	private final int P = 0;

	private Map<Pair<Integer, Integer>, Polygon2D> map = new HashMap<>();
	private Polygon2D polygon = new Polygon2D();
	private Set<Point2D> seenPoints = new HashSet<>();
	private List<Voter> voters = new ArrayList<>();
	private int numTriangles = 1;
	private int numVoters = 0;
	private List<Double> voterPref = new ArrayList<>();; 
	private List<Double> voterStrength = new ArrayList<>();;
	private List<Integer> voterCount = new ArrayList<>();;
	private int upperPop = 4526;
	private int lowerPop = 3703;
	private double target = 0.0;


	public Municipal(Pair<Integer, Integer> coordinates, Polygon2D newTriangle, List<Voter> voters) {
		this.map.put(coordinates, newTriangle);
		this.polygon = newTriangle;
		for (Point2D p : newTriangle.getPoints()) {
			this.seenPoints.add(p);
		}
		this.voters = voters;
		updateVoterValues();
	}

	private void updateVoterValues(){
		numVoters = voters.size();
		int oneCount = 0;
		int twoCount = 0;
		double onePrefDiff = 0.0;
		double twoPrefDiff = 0.0;
		for(Voter v : voters){
			List<Double> pref = v.getPreference();
			if(pref.get(0) > pref.get(1)){
				oneCount++;
				onePrefDiff += pref.get(0) - pref.get(1);
			}
			else{
				twoCount++;
				twoPrefDiff += pref.get(1) - pref.get(0);
			}
		}
		
		voterCount.add(oneCount);
		voterCount.add(twoCount);

		voterPref.add(oneCount / (double) numVoters);
		voterPref.add(twoCount/ (double) numVoters);

		voterStrength.add(onePrefDiff / oneCount);
		voterStrength.add(twoPrefDiff / twoCount);

		//calculate target
	}

	// public List<Integer> calculateCount(List<Integer> voters){}
	// public List<Double> calculatePref(List<Integer> voters){}
	// public List<Double> calculateStrength(List<Integer> voters){}

	public boolean canAdd(Pair<Integer, Integer> coordinates, Municipal newMunicipal, int upperPop) {
		Polygon2D newTriangle = newMunicipal.getPolygon();
		int newVoters = newMunicipal.getNumVoters();
		if (numVoters + newVoters > upperPop) return false;
		if (newTriangle.getPoints().size() != 3) return false;
		if (map.keySet().contains(coordinates)) return false;
		if (isInvalidShapeClosing(coordinates, newTriangle)) return false;
		// if (isTooManySides(newTriangle)) return false;
		return true;
	}

	public boolean shouldAdd(List<Voter> newVoters){
		// calculate new values
		// compare values to target
		// decide
		return false;
	}

	// assume that the new polygon is a triangle
	public boolean add(Pair<Integer, Integer> coordinates, Polygon2D newTriangle, List<Voter> voters) {
		if (newTriangle.getPoints().size() != 3) return false;
		if (map.keySet().contains(coordinates)) return false;
		if (isInvalidShapeClosing(coordinates, newTriangle)) return false;
		// if (isTooManySides(newTriangle)) return false;
		this.map.put(coordinates, newTriangle);
		this.voters.addAll(voters);
		this.updateVoterValues();
		this.updatePolygon(newTriangle);
		this.numTriangles++;
		return true;
	}

	public void updatePolygon(Polygon2D newPolygon) {
		// assume it is a neighboring triangle, 3 sides
		// System.out.println("Before: " + this.polygon.toString());
		// System.out.println("Appending: " + newPolygon.toString());
		Polygon2D updatedPolygon = new Polygon2D();
		List<Point2D> points = newPolygon.getPoints();
		List<Point2D> oldPoints = polygon.getPoints();
		List<Point2D> newPoints = new ArrayList<>();

		int pointsIndex = 0;
		int oldPointsIndex = 0;

		Point2D p0 = points.get(0);
		Point2D p1 = points.get(1);
		Point2D p2 = points.get(2);

		while(oldPointsIndex < oldPoints.size()) {
			Point2D oldPrevious = oldPoints.get((oldPointsIndex-1+oldPoints.size()) % oldPoints.size());
			Point2D oldCurrent = oldPoints.get(oldPointsIndex);
			Point2D oldNext = oldPoints.get((oldPointsIndex+1) % oldPoints.size());
			Point2D oldNextNext = oldPoints.get((oldPointsIndex+2) % oldPoints.size());
			// System.out.println("appending current: " + oldCurrent);
			// boolean success = updatedPolygon.append(oldCurrent);
			// newPoints.add(oldCurrent);
			// System.out.println("success?: " + success);
			if ((approxEquals(oldCurrent, p0) && approxEquals(oldNext, p1) || (approxEquals(oldCurrent, p1) && approxEquals(oldNext, p0)))) {
				// System.out.println("p0,p1 equal");
				if (approxEquals(oldPrevious, p2)) {
					// ; intentionally do not add, this is a middle point that will be processed later.
				} else {
					newPoints.add(oldCurrent);
					if (!approxEquals(oldNextNext, p2)) {
						newPoints.add(p2);
					}
				}	
			} else if ((approxEquals(oldCurrent, p1) && approxEquals(oldNext, p2) || (approxEquals(oldCurrent, p2) && approxEquals(oldNext, p1)))) {
				// System.out.println("p1,p2 equal");
				if (approxEquals(oldPrevious, p0)) {
					// ; intentionally do not add, this is a middle point that will be processed later.
				} else {
					newPoints.add(oldCurrent);
					if (!approxEquals(oldNextNext, p0)) {
						newPoints.add(p0);
					}
				}
			} else if ((approxEquals(oldCurrent, p2) && approxEquals(oldNext, p0) || (approxEquals(oldCurrent, p0) && approxEquals(oldNext, p2)))) {
				// System.out.println("p2,p0 equal");
				if (approxEquals(oldPrevious, p1)) {
					// ; intentionally do not add, this is a middle point that will be processed later.
				} else {
					newPoints.add(oldCurrent);
					if (!approxEquals(oldNextNext, p1)) {
						newPoints.add(p1);
					}
				}		
			} else {
				newPoints.add(oldCurrent);
			}
			oldPointsIndex++;
		}

		for (Point2D newPoint : newPoints) {
			boolean success = updatedPolygon.append(newPoint);
			if (!success) {
				throw new RuntimeException("Could not append a point to the updated polygon?");
			}
		}

		validate(this.polygon, updatedPolygon, newPolygon);

		this.polygon = updatedPolygon;
		for (Point2D p : newPolygon.getPoints()) {
			this.seenPoints.add(p);
		}
		// System.out.println("After: " + this.polygon.toString());
	}

	// maybe don't use this because it's very restrictive... instead simplify the resulting polygon?
	private boolean isTooManySides(Polygon2D newTriangle) {
		Polygon2D testPolygon = new Polygon2D();
		for (Point2D p : this.polygon.getPoints()) {
			testPolygon.append(p);
		}
		Municipal testMunicipal = new Municipal(new Pair<>(-1,-1), testPolygon, new ArrayList<>());
		testMunicipal.updatePolygon(newTriangle);
		List<Point2D> testMunicipalPoints = testMunicipal.getPolygon().getPoints();
		int count = 1;
		Point2D current = testMunicipalPoints.get(0);
		Point2D next = testMunicipalPoints.get(1);
		double slope = (next.getY() - current.getY())/(next.getX() - current.getX());
		for (int i = 2; i < testMunicipalPoints.size(); i++) {
			current = next;
			next = testMunicipalPoints.get(i);
			double newSlope = (next.getY() - current.getY())/(next.getX() - current.getX());
			if (Math.abs(newSlope - slope) > EPSILON) count++;
		}
		return count > 9;
	}

	private boolean isInvalidShapeClosing(Pair<Integer, Integer> coordinate, Polygon2D newTriangle) {
		// a closing of a shape is invalid if 3 of the triangle's points have been seen, but only 1 of its neighbors!
		Set<Pair<Integer, Integer>> neighboringCoordinates = new HashSet<>(this.getNeighboringCoordinates(coordinate));
		neighboringCoordinates.retainAll(this.map.keySet());
		Set<Point2D> overlappingPoints = new HashSet<>();
		for(Point2D p1 : newTriangle.getPoints()) {
			for(Point2D p2 : this.seenPoints) {
				if (approxEquals(p1, p2)) {
					overlappingPoints.add(p1);
					continue;
				}
			}
		}
		return neighboringCoordinates.size() == 1 && overlappingPoints.size() == 3;
	}

	private void validate(Polygon2D old, Polygon2D update, Polygon2D delta) {
		validateArea(old, update, delta);
		validatePoints(old, update, delta);
	}

	private void validatePoints(Polygon2D old, Polygon2D update, Polygon2D delta) {
		Set<Point2D> seenPoints = new HashSet<>();
		for(Point2D point : update.getPoints()) {
			if (seenPoints.contains(point)) {
				System.out.println("Current:");
				this.printListPoint2D(old.getPoints());
				System.out.println("Adding:");
				this.printListPoint2D(delta.getPoints());
				System.out.println("Updated:");
				this.printListPoint2D(update.getPoints());
				throw new RuntimeException("invalid update, polygon contains same point twice");
			}
			seenPoints.add(point);
		}
	}

	private void validateArea(Polygon2D old, Polygon2D update, Polygon2D delta) {
		double expectedArea = old.area() + delta.area();
		double actualArea = update.area();
		if (Math.abs(old.area() + delta.area() - update.area()) > 1e-7) {
			System.out.println("Current:");
			this.printListPoint2D(old.getPoints());
			System.out.println("Adding:");
			this.printListPoint2D(delta.getPoints());
			System.out.println("Updated:");
			this.printListPoint2D(update.getPoints());
			throw new RuntimeException("invalid update, polygon area did not increase as expected");
		}
	}

	private boolean approxEquals(Point2D p1, Point2D p2) {
		return (Math.abs(p1.getX()-p2.getX()) < EPSILON && Math.abs(p1.getY() - p2.getY()) < EPSILON);
	}

	private void printListPoint2D(List<Point2D> list) {
		for (Point2D point : list) {
			System.out.println(point);
		}
	}

	public Polygon2D getPolygon() {
		return polygon;
	}

	public boolean contains(Pair<Integer, Integer> coordinate) {
		return map.keySet().contains(coordinate);
	}

	public boolean containsAllVertices(Polygon2D newPolygon) {
		for(Point2D point: newPolygon.getPoints()) {
			if(!this.seenPoints.contains(point)) return false;
		}
		return true;
	}

	public List<Pair<Integer, Integer>> getNeighboringCoordinates() {
	List<Pair<Integer, Integer>> neighbors = new ArrayList<>();
		for (Pair<Integer, Integer> coordinate : this.map.keySet()) {
			List<Pair<Integer, Integer>> coordinateNeighbors = getNeighboringCoordinates(coordinate);
			neighbors.addAll(coordinateNeighbors);
		}
		neighbors.removeAll(this.map.keySet());
		return neighbors;
	}

	public static List<Pair<Integer, Integer>> getNeighboringCoordinates(Pair<Integer, Integer> coordinate) {
        List<Pair<Integer, Integer>> neighbors = new ArrayList<>();
        int x = coordinate.getKey();
        int y = coordinate.getValue();
        if (x % 2 == 0) {
            neighbors.add(new Pair<Integer, Integer>(x-1, y-1));
            neighbors.add(new Pair<Integer, Integer>(x-1, y));  
            neighbors.add(new Pair<Integer, Integer>(x+1, y));
        } else {
            neighbors.add(new Pair<Integer, Integer>(x+1, y+1));  
            neighbors.add(new Pair<Integer, Integer>(x+1, y));
            neighbors.add(new Pair<Integer, Integer>(x-1, y));
        }
        return neighbors;
	}
	
	public boolean isValid(){
		return numVoters < upperPop && numVoters > lowerPop;
	}

    public int getNumTriangles() {
    	return this.numTriangles;
	}
	
	public int getNumVoters() {
    	return this.numVoters;
    }

    public List<Voter> getVoters() {
    	return this.voters;
	}

	public int getPopulation(){
		return this.voters.size();
	}

	public List<Integer> getVoterCount(){
		return this.voterCount;
	}

	public List<Double> getVoterPref(){
		return this.voterPref;
	}

	public List<Double> getVoterStrength(){
		return this.voterStrength;
	}

    public void print() {
    	System.out.println("Municipal:");
    	// for (Pair<Integer, Integer> coordinate : map.keySet()) {
    	// 	System.out.print(" x : " + coordinate.getKey() + " y: " + coordinate.getValue() + " ");
    	// 	System.out.println(map.get(coordinate));
    	// }
    	this.printListPoint2D(this.polygon.getPoints());
    	// System.out.println();
    }

}