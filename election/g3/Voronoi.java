package election.g3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import election.sim.Voter;

public class Voronoi {

    private Map<Cluster, List<Voter>> voronoiMap = new HashMap<>();
    private List<Voter> voters;
	private List<Cluster> clusters;
	
    // Calculate minimum and maximum population sizes for clusters
    private int minimumPopulation;
    private int maximumPopulation;
    
	public Voronoi(List<Voter> voters, List<Cluster> clusters) {
		this.voters = voters;
		this.clusters = clusters;
    	this.minimumPopulation = (int) (Math.ceil((double) voters.size() * 0.9 / (double) clusters.size()));
    	this.maximumPopulation = (int) (Math.floor((double) voters.size() * 1.1 / (double) clusters.size()));
	}
	
    public void execute() {
    	for(Cluster cluster : clusters) {
    		cluster.clear();
    		voronoiMap.put(cluster, new ArrayList<>());
    	} 
    	for(Voter voter : voters) {
    		NewPoint voterLocation = new NewPoint(voter.getLocation().getX(), voter.getLocation().getY());
    		Cluster chosenCluster = null;
    		double minDistance = Double.MAX_VALUE;
    		for(Cluster cluster : clusters) {
    			if(voronoiMap.get(cluster).size() >= minimumPopulation)
    				continue;
    			NewPoint centroid = cluster.getCentroid();
	    		if(NewPoint.distance(centroid, voterLocation) < minDistance) {
    				minDistance = NewPoint.distance(centroid, voterLocation);
    				chosenCluster = cluster;
    			}
    		}
    		if(chosenCluster != null) {
    			chosenCluster.addVoter(voter);
    			voronoiMap.get(chosenCluster).add(voter);
    		}
    		else {
        		for(Cluster cluster : clusters) {
        			if(voronoiMap.get(cluster).size() >= maximumPopulation)
        				continue;
        			NewPoint centroid = cluster.getCentroid();
    	    		if(NewPoint.distance(centroid, voterLocation) < minDistance) {
        				minDistance = NewPoint.distance(centroid, voterLocation);
        				chosenCluster = cluster;
        			}
        		}
        		if(chosenCluster != null) {
        			chosenCluster.addVoter(voter);
        			voronoiMap.get(chosenCluster).add(voter);
        		}
        		else
        			System.out.println("Voter cannot be added to any district based on Voronoi. Something is fishy!");
    		}
		}
    	CentroidCalculation centroidCalculation = new CentroidCalculation(clusters);
    	centroidCalculation.calculateCentroids();
    }
        
    public void print() {
    	int i = 1;
    	int totalVoters = 0;
    	int validDistricts = 0;
    	for (Cluster c : voronoiMap.keySet()) {
    		System.out.println("District " + i + ": " + voronoiMap.get(c).size() + " voters");
    		totalVoters += voronoiMap.get(c).size();
    		i++;
    		if (voronoiMap.get(c).size() >= minimumPopulation && voronoiMap.get(c).size() <= maximumPopulation)
    			validDistricts++;
    	}
    	System.out.println("Total voters: " + totalVoters);
    	System.out.println("Number of valid districts: " + validDistricts);
    }
    
    public Map<Cluster, List<Voter>> getVoronoiMap() {
    	return voronoiMap;
    }
    
    public List<Voter> getVotersInDistrict(Cluster cluster) {
    	return voronoiMap.get(cluster);
    }
}