package election.g3;

import java.util.ArrayList;
import java.util.List;

import election.sim.Voter;

public class Cluster implements Comparable<Cluster> {
	
	private List<Voter> voters;
	private NewPoint centroid;
	private int id;
	
	//Creates a new Cluster
	public Cluster(int id) {
		this.id = id;
		this.voters = new ArrayList<>();
		this.centroid = null;
	}

	public List<Voter> getVoters() {
		return voters;
	}
		
	public void addVoter(Voter voter) {
		voters.add(voter);
	}
	
	public void removeVoter(Voter voter) {
		voters.remove(voter);
	}

	public void setVoters(List<Voter> voters) {
		this.voters = voters;
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
		voters.clear();
	}
	
	public void plotCluster() {
		System.out.println("[Cluster: " + id + "]");
		System.out.println("[Centroid: " + centroid + "]");
		System.out.println("[Voter locations: \n");
		for(Voter voter : voters) {
			System.out.println(voter.getLocation());
		}
		System.out.println("]");
	}

	@Override
	public int compareTo(Cluster cluster) {
		if(voters == null || cluster.getVoters() == null || voters.size() == cluster.getVoters().size())
			return 0;
		if(voters.size() > cluster.getVoters().size())
			return -1;
		if(voters.size() < cluster.getVoters().size())
			return 1;
		return 0;
	}
}
