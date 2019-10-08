package election.g1;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;
import javafx.util.Pair;

public class Municipal {
	
	private Map<Pair<Integer, Integer>, Polygon2D> map = new HashMap<>();
	private Polygon2D polygon = new Polygon2D();

	public Municipal(Pair<Pair<Integer, Integer>, Polygon2D> newTriangle) {
		// if (newTriangle.getValue() == null) System.out.println("constructing municipal with null triangle!");
		this.map.put(newTriangle.getKey(), newTriangle.getValue());
		polygon = newTriangle.getValue();
	}

	// assume that the new polygon is a triangle
	public boolean add(Pair<Pair<Integer, Integer>, Polygon2D> newTriangle) {
		if (map.keySet().contains(newTriangle.getKey())) return false;
		this.map.put(newTriangle.getKey(), newTriangle.getValue());
		this.updatePolygon(newTriangle.getValue());
		return true;
	}

	private void updatePolygon(Polygon2D newPolygon) {
		// assume it is a neighboring triangle, 3 sides
		System.out.println("Before: " + this.polygon.toString());
		System.out.println("Appending: " + newPolygon.toString());
		Polygon2D updatedPolygon = new Polygon2D();
		List<Point2D> points = newPolygon.getPoints();
		List<Point2D> oldPoints = polygon.getPoints();

		int pointsIndex = 0;
		int oldPointsIndex = 0;

		Point2D p0 = points.get(0);
		Point2D p1 = points.get(1);
		Point2D p2 = points.get(2);

		while(oldPointsIndex < oldPoints.size()) {
			Point2D oldCurrent = oldPoints.get(oldPointsIndex);
			Point2D oldNext = oldPoints.get((oldPointsIndex+1) % oldPoints.size());
			Point2D oldNextNext = oldPoints.get((oldPointsIndex+2) % oldPoints.size());
			System.out.println("appending current: " + oldCurrent);
			System.out.println("success?: " + updatedPolygon.append(oldCurrent));
			if ((oldCurrent.equals(p0) && oldNext.equals(p1)) || (oldCurrent.equals(p1) && oldNext.equals(p0))) {
				if (oldNextNext.equals(p2)) {
					oldPointsIndex++;
				} else {
					System.out.println("appending new: " + p2);
					System.out.println("success1?: " + updatedPolygon.append(p2)); 
				}
			} else if ((oldCurrent.equals(p1) && oldNext.equals(p2)) || (oldCurrent.equals(p2) && oldNext.equals(p1))) {
				if (oldNextNext.equals(p0)) {
					oldPointsIndex++;
				} else {
					System.out.println("appending new: " + p0);
					System.out.println("success2?: " + updatedPolygon.append(p0)); 
				}
			} else if ((oldCurrent.equals(p2) && oldNext.equals(p0)) || (oldCurrent.equals(p0) && oldNext.equals(p2))) {
				if (oldNextNext.equals(p1)) {
					oldPointsIndex++;
				} else {
					System.out.println("appending new: " + p1);
					System.out.println("success3?: " + updatedPolygon.append(p1)); 
				}			
			}
			oldPointsIndex++;
		}
		this.polygon = updatedPolygon;
		System.out.println("After: " + this.polygon.toString());
	}

	public Polygon2D getPolygon() {
		return polygon;
	}

	public boolean contains(Pair<Integer, Integer> coordinate) {
		return map.keySet().contains(coordinate);
	}

	public List<Pair<Integer, Integer>> getNeighboringCoordinates() {
	List<Pair<Integer, Integer>> neighbors = new ArrayList<>();
		for (Pair<Integer, Integer> coordinate : this.map.keySet()) {
			List<Pair<Integer, Integer>> coordinateNeighbors = getNeighboringCoordinates(coordinate);
			neighbors.addAll(coordinateNeighbors);
		}
		neighbors.remove(this.map.keySet());
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

    public void print() {
    	System.out.println("Municipal:");
    	for (Pair<Integer, Integer> coordinate : map.keySet()) {
    		System.out.print(" x : " + coordinate.getKey() + " y: " + coordinate.getValue());
    	}
    	System.out.println();
    }

}