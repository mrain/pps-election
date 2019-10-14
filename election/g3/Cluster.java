package election.g3;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
	
	private List<NewPoint> points;
	private NewPoint centroid;
	private int id;
	
	//Creates a new Cluster
	public Cluster(int id) {
		this.id = id;
		this.points = new ArrayList<>();
		this.centroid = null;
	}

	public List<NewPoint> getPoints() {
		return points;
	}
	
	public void addPoint(NewPoint point) {
		points.add(point);
	}

	public void setPoints(List<NewPoint> points) {
		this.points = points;
	}

	public NewPoint getCentroid() {
		return centroid;
	}

	public void setCentroid(NewPoint centroid) {
		this.centroid = centroid;
	}

	public int getID() {
		return id;
	}
	
	public void clear() {
		points.clear();
	}
	
	public void plotCluster() {
		System.out.println("[Cluster: " + id + "]");
		System.out.println("[Centroid: " + centroid + "]");
		System.out.println("[Points: \n");
		for(NewPoint p : points) {
			System.out.println(p);
		}
		System.out.println("]");
	}
}
