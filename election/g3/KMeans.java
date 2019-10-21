package election.g3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import election.sim.Voter;
  
public class KMeans {
	
	private Random random = new Random();
 
	// Number of clusters
    private int numClusters;
    
    // Calculate minimum and maximum population sizes for clusters
    private int minimumPopulation;
    private int maximumPopulation;
    
    // Voters
    private List<Voter> voters;
    
    // List of clusters
    private List<Cluster> clusters;
    
    public KMeans(List<Voter> voters, int numClusters) {
    	this.voters = voters;
    	this.numClusters = numClusters;
    	this.minimumPopulation = (int) (Math.ceil((double) voters.size() * 0.9 / (double) numClusters));
    	this.maximumPopulation = (int) (Math.floor((double) voters.size() * 1.1 / (double) numClusters));
    	this.clusters = new ArrayList<>();
    }
    
    public void execute() {
    	init();
    	calculate();
    }
    
    // Initialize the process
    private void init() {
    	// Create clusters, setting random centroids
    	for (int i = 0; i < numClusters; i++) {
    		Cluster cluster = new Cluster(i);
    		NewPoint centroid = NewPoint.createRandomPoint();
    		cluster.setCentroid(centroid);
    		clusters.add(cluster);
    	}
    	
    	// Print initial state
//    	plotClusters();
    }
 
	private void plotClusters() {
    	for (int i = 0; i < numClusters; i++) {
    		Cluster cluster = clusters.get(i);
    		cluster.plotCluster();
    	}
    }
    
	// Calculate K-Means
    private void calculate() {
        boolean finish = false;
        int iteration = 0;
        
        // Add in new data, one at a time, recalculating centroids with each new one
        while(!finish) {
        	// Clear cluster state
        	clearClusters();
        	
        	List<NewPoint> lastCentroids = getCentroids();
        	
        	// Assign points to the closer cluster
        	assignCluster();
            
            // Calculate new centroids
        	CentroidCalculation centroidCalculation = new CentroidCalculation(clusters);
        	centroidCalculation.calculateCentroids();
        	
        	iteration++;
        	
        	List<NewPoint> currentCentroids = getCentroids();
        	
        	// Calculate total distance between new and old centroids
        	double distance = 0;
        	for(int i = 0; i < lastCentroids.size(); i++)
        		distance += NewPoint.distance(lastCentroids.get(i), currentCentroids.get(i));
//        	System.out.println("#################");
//        	System.out.println("Iteration: " + iteration);
        	System.out.println("Centroid distances: " + distance);
//        	plotClusters();
        	        	
        	if(distance == 0)
        		finish = true;
        }
    }
    
    private void clearClusters() {
    	for(Cluster cluster : clusters)
    		cluster.clear();
    }
    
    private List<NewPoint> getCentroids() {
    	List<NewPoint> centroids = new ArrayList<>();
    	for(Cluster cluster : clusters) {
    		NewPoint aux = cluster.getCentroid();
    		NewPoint point = new NewPoint(aux.getX(),aux.getY());
    		centroids.add(point);
    	}
    	return centroids;
    }
    
    private void assignCluster() {
    	double max = Double.MAX_VALUE;
    	double min = max; 
    	int cluster = -1;                 
    	double distance = 0.0; 

    	for(Voter voter : voters) {
    		NewPoint point = getVoterLocation(voter);
    		min = max;
    		for(int i = 0; i < numClusters; i++) {
    			Cluster c = clusters.get(i);
    			distance = NewPoint.distance(point, c.getCentroid());
    			if(distance < min) {
    				min = distance;
    				cluster = i;
    			}
    		}
			clusters.get(cluster).addVoter(voter);
    	}
    }
    
    private NewPoint getVoterLocation(Voter voter) {
   		return new NewPoint(voter.getLocation().getX(), voter.getLocation().getY());
    }
    
    public List<Cluster> getClusters() {
    	return clusters;
    }
}