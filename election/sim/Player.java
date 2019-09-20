package election.sim;

import java.util.List;

public abstract class Player {
    public Player() {}

    public abstract void init(int numVoters, int numParties, int numDivision, long seed);
    public abstract List<Polygon2D> getDistricts(List<Voter> voters);
}
