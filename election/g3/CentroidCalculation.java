package election.g3;

import java.util.List;

import election.sim.Voter;

public class CentroidCalculation {
	
	List<Cluster> clusters;
	
	public CentroidCalculation(List<Cluster> clusters) {
		this.clusters = clusters;
	}
	
	public void calculateCentroids() {
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
}