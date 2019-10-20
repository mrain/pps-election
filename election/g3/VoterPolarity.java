package election.g3;

import election.sim.Voter;
import java.util.List;
import java.util.ArrayList;

public class VoterPolarity {
	
	private Voter voter;
	private List<Double> polarityValues; // 1st val: p1 - p3, 2nd val: p1 - p3, 3rd val: p2 - p3
	private List<Polarity> polarities; // Define polarities for prop/opp

	enum Polarity {
		STRONG_PROP, WEAK_PROP, STRONG_OPP, WEAK_OPP;
	}
	
	public VoterPolarity(Voter voter) {		
		this.voter = voter;
		this.polarities = new ArrayList<>();
		List<Double> preferences = voter.getPreference();
		if(preferences.size() == 2) {
			Double party1PV = preferences.get(0) - preferences.get(1);
			Double party2PV = preferences.get(1) - preferences.get(0);
			polarityValues.add(party1PV);
			polarityValues.add(party2PV);
		}
		else if(preferences.size() == 3) {
			Double party1PV = 2 * preferences.get(0) - preferences.get(1) - preferences.get(2);
			Double party2PV = 2 * preferences.get(1) - preferences.get(0) - preferences.get(2);
			Double party3PV = 2 * preferences.get(2) - preferences.get(0) - preferences.get(1);
			polarityValues.add(party1PV);
			polarityValues.add(party2PV);
			polarityValues.add(party3PV);
		}
	}
	
	public Voter getVoter() {
		return voter;
	}
	
	public void setVoter(Voter voter) {
		this.voter = voter;
	}
}