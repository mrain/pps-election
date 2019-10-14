package election.g3;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import election.sim.Voter;

public class Voronoi {

    private Map<NewPoint, List<Voter>> voronoiMap = new HashMap<>();
    private List<Voter> voters;
	private List<NewPoint> centroids;
    
	public Voronoi(List<Voter> voters, List<NewPoint> centroids) {
		this.voters = voters;
		this.centroids = centroids;
	}
	
    public void execute() {
    	for(NewPoint centroid : centroids) {
    		voronoiMap.put(centroid, new ArrayList<>());
    	} 
    	for(Voter voter : voters) {
    		NewPoint voterLocation = new NewPoint(voter.getLocation().getX(), voter.getLocation().getY());
    		NewPoint chosenCentroid = null;
    		double minDistance = Double.MAX_VALUE;
    		for(NewPoint centroid : centroids) {
	    		if(NewPoint.distance(centroid, voterLocation) < minDistance) {
    				minDistance = NewPoint.distance(centroid, voterLocation);
    				chosenCentroid = centroid;
    			}
    		}
    		voronoiMap.get(chosenCentroid).add(voter);
		}
    }
    
    public void print() {
    	int i = 1;
    	int totalVoters = 0;
    	int validDistricts = 0;
    	for (NewPoint p : voronoiMap.keySet()) {
    		System.out.println("District " + i + ": " + voronoiMap.get(p).size() + " voters");
    		totalVoters += voronoiMap.get(p).size();
    		i++;
    		if (voronoiMap.get(p).size() >= 3703 && voronoiMap.get(p).size() <= 4526)
    			validDistricts++;
    	}
    	System.out.println("Total voters: " + totalVoters);
    	System.out.println("Number of valid districts: " + validDistricts);
    }
    
    public Map<NewPoint, List<Voter>> getVoronoiMap() {
    	return voronoiMap;
    }
    
    public List<Voter> getVotersInDistrict(NewPoint centroid) {
    	return voronoiMap.get(centroid);
    }
}