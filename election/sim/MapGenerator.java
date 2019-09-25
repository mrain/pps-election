package election.sim;

import java.util.List;

public interface MapGenerator {
    // Generate a list of voters
    public abstract List<Voter> getVoters(int numVoters, int numParties, long seed);
}
