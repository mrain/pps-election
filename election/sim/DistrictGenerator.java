package election.sim;

import java.util.List;

public interface DistrictGenerator {
    // repPerDistrict could be 1 or 3. You should output 243 or 81 districts respectively.
    // Number of parties can be obtained from voter's preference list
    public abstract List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed);
}
