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
        	calculateCentroids();
        	
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
    
    private void calculateCentroids() {
        for(Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            List<Voter> clusterVoters = cluster.getVoters();
            int numPoints = clusterVoters.size();
            
            for(Voter voter : clusterVoters) {
            	sumX += voter.getLocation().getX();
                sumY += voter.getLocation().getY();
            }
            
            NewPoint centroid = cluster.getCentroid();
            if(numPoints > 0) {
            	double newX = sumX / numPoints;
            	double newY = sumY / numPoints;
                centroid.setX(newX);
                centroid.setY(newY);
            }
        }
    }
    
    private void evenlyDistributeClustersV3() {
    	Collections.sort(clusters);

    	int fromClusterToAnalyze = 0;
    	
    	while(fromClusterToAnalyze < numClusters) {
    		Cluster fromCluster = clusters.get(fromClusterToAnalyze);
    		List<Voter> fromClusterVoters = fromCluster.getVoters();
    		while(fromClusterVoters.size() > maximumPopulation) {
    			int chosenToClusterIndex = -1;
    			double smallestDistance = Double.MAX_VALUE;
    			for(int toClusterToAnalyze = fromClusterToAnalyze + 1; toClusterToAnalyze < numClusters; toClusterToAnalyze++) {
    				Cluster toCluster = clusters.get(toClusterToAnalyze);
    				List<Voter> toClusterVoters = toCluster.getVoters();
    				if(toClusterVoters.size() > minimumPopulation)	// Need minimum population, since we do not want to add new voters if cluster already has more than minimum
    					continue;
    				double distance = NewPoint.distance(fromCluster.getCentroid(), toCluster.getCentroid());
    				if(distance < smallestDistance) {
    					smallestDistance = distance;
    					chosenToClusterIndex = toClusterToAnalyze;
    				}
    			}
    			
    			if(chosenToClusterIndex == -1)
    				continue;
    			
    			Cluster chosenToCluster = clusters.get(chosenToClusterIndex);
    			    			
				while(chosenToCluster.getVoters().size() < minimumPopulation && fromClusterVoters.size() > minimumPopulation) {
					Voter voterToMove = fromCluster.getVoters().get(random.nextInt(fromCluster.getVoters().size()));
    				clusters.get(fromClusterToAnalyze).removeVoter(voterToMove);
    				clusters.get(chosenToClusterIndex).addVoter(voterToMove);
				}
    		}
    		fromClusterToAnalyze++;
    	}
    	calculateCentroids();
    }
    
    private void evenlyDistributeClustersV2() {
    	Collections.sort(clusters);

    	int fromClusterToAnalyze = 0;
    	
    	while(fromClusterToAnalyze < numClusters) {
    		Cluster fromCluster = clusters.get(fromClusterToAnalyze);
    		List<Voter> fromClusterVoters = fromCluster.getVoters();
    		while(fromClusterVoters.size() > maximumPopulation) {
    			int chosenToClusterIndex = -1;
    			double smallestDistance = Double.MAX_VALUE;
    			for(int toClusterToAnalyze = fromClusterToAnalyze + 1; toClusterToAnalyze < numClusters; toClusterToAnalyze++) {
    				Cluster toCluster = clusters.get(toClusterToAnalyze);
    				List<Voter> toClusterVoters = toCluster.getVoters();
    				if(toClusterVoters.size() > minimumPopulation)	// Need minimum population, since we do not want to add new voters if cluster already has more than minimum
    					continue;
    				double distance = NewPoint.distance(fromCluster.getCentroid(), toCluster.getCentroid());
    				if(distance < smallestDistance) {
    					smallestDistance = distance;
    					chosenToClusterIndex = toClusterToAnalyze;
    				}
    			}
    			
    			if(chosenToClusterIndex == -1)
    				continue;
    			
    			Cluster chosenToCluster = clusters.get(chosenToClusterIndex);
    			
    			Map<Voter, Double> fromClusterVotersToCentroidMap = new HashMap<>();
    			for(Voter fromClusterVoter : fromClusterVoters)
    				fromClusterVotersToCentroidMap.put(fromClusterVoter, NewPoint.distance(chosenToCluster.getCentroid(), getVoterLocation(fromClusterVoter)));
    			
    			Object[] sortedFromClusterVotersArray = fromClusterVotersToCentroidMap.entrySet().toArray();
				Arrays.sort(sortedFromClusterVotersArray, new Comparator<Object>() {
				    public int compare(Object o1, Object o2) {
				        return ((Double) (((Map.Entry<Voter, Double>) o1).getValue())).compareTo((Double) (((Map.Entry<Voter, Double>) o2).getValue()));
				    }
				});
				
				List<Voter> sortedVoters = new ArrayList<>();
				for (Object sortedFromClusterVotersArrayElement : sortedFromClusterVotersArray)
					sortedVoters.add(((Map.Entry<Voter, Double>) sortedFromClusterVotersArrayElement).getKey());
    			
				while(chosenToCluster.getVoters().size() < minimumPopulation && fromClusterVoters.size() > minimumPopulation) {
					Voter voterToMove = sortedVoters.get(0);
    				clusters.get(fromClusterToAnalyze).removeVoter(voterToMove);
    				clusters.get(chosenToClusterIndex).addVoter(voterToMove);
					sortedVoters.remove(voterToMove);
				}
    		}
    		fromClusterToAnalyze++;
    	}
    	calculateCentroids();
    }
    
    private void evenlyDistributeClustersV1() {
    	Collections.sort(clusters);

    	int chosenFromClusterIndex = -1;
    	int fromClusterToAnalyze = 0;
    	
    	while(fromClusterToAnalyze < numClusters) {
    		Cluster fromCluster = clusters.get(fromClusterToAnalyze);
    		List<Voter> fromClusterVoters = fromCluster.getVoters();
    		while(fromClusterVoters.size() > maximumPopulation) {
    			Voter voterToMove = null;
    			int chosenToClusterIndex = -1;
    			double smallestDistance = Double.MAX_VALUE;
    			for(int toClusterToAnalyze = fromClusterToAnalyze + 1; toClusterToAnalyze < numClusters; toClusterToAnalyze++) {
    				Cluster toCluster = clusters.get(toClusterToAnalyze);
    				List<Voter> toClusterVoters = toCluster.getVoters();
    				if(toClusterVoters.size() < minimumPopulation) {
    					for(Voter fromClusterVoter : fromClusterVoters) {
				        	NewPoint fromClusterVoterLocation = getVoterLocation(fromClusterVoter);
			                double distance = NewPoint.distance(fromClusterVoterLocation, toCluster.getCentroid());
			                if(distance < smallestDistance) {
			                    smallestDistance = distance;
			                    chosenFromClusterIndex = fromClusterToAnalyze;
			                    voterToMove = fromClusterVoter;
			                    chosenToClusterIndex = toClusterToAnalyze;
			                }
    					}
    				}
    			}
    			
    			if(chosenFromClusterIndex != -1 && chosenToClusterIndex != -1 && voterToMove != null) {
    				clusters.get(chosenFromClusterIndex).removeVoter(voterToMove);
    				clusters.get(chosenToClusterIndex).addVoter(voterToMove);
    			}
    			
    		}
    		fromClusterToAnalyze++;
    	}
    	calculateCentroids();
    }
    
    public List<Cluster> getClusters() {
    	return clusters;
    }
}