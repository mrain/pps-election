package election.g3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import election.sim.Voter;

public class Voronoi {

    private Map<NewPoint, List<Voter>> voronoiMap = new HashMap<>();
    private List<Voter> voters;
	private List<NewPoint> centroids;
	
    // Calculate minimum and maximum population sizes for clusters
    private int minimumPopulation;
    private int maximumPopulation;
    
	public Voronoi(List<Voter> voters, List<NewPoint> centroids) {
		this.voters = voters;
		this.centroids = centroids;
    	this.minimumPopulation = (int) (Math.ceil((double) voters.size() * 0.9 / (double) centroids.size()));
    	this.maximumPopulation = (int) (Math.floor((double) voters.size() * 1.1 / (double) centroids.size()));
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
    			if(voronoiMap.get(centroid).size() >= minimumPopulation)
    				continue;
	    		if(NewPoint.distance(centroid, voterLocation) < minDistance) {
    				minDistance = NewPoint.distance(centroid, voterLocation);
    				chosenCentroid = centroid;
    			}
    		}
    		if(chosenCentroid != null)
    			voronoiMap.get(chosenCentroid).add(voter);
    		else {
        		for(NewPoint centroid : centroids) {
        			if(voronoiMap.get(centroid).size() >= maximumPopulation)
        				continue;
    	    		if(NewPoint.distance(centroid, voterLocation) < minDistance) {
        				minDistance = NewPoint.distance(centroid, voterLocation);
        				chosenCentroid = centroid;
        			}
        		}    			
        		if(chosenCentroid != null)
        			voronoiMap.get(chosenCentroid).add(voter);
        		else
        			System.out.println("Voter cannot be added to any district based on Voronoi. Something is fishy!");
    		}
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