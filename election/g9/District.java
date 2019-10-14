package election.g9;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;


public class District {
    public Polygon2D polygon;
    public List<Voter> voters;

    /* District takes in a polygon, a list of voters (either more than are contained within
     * the district or all the voters contained within the district) and a boolean
     * that says whether the voters are the ones in the district or more than in the district. 
     */
    public District(Polygon2D initpolygon, List<Voter> initvoters, Boolean populated){
        polygon = initpolygon;
        if (populated){
            voters = initvoters;
        } else {
            voters = populateDistrict(polygon, initvoters);
        }
    }

    public District(){
        polygon = new Polygon2D();
        voters = new ArrayList<Voter>();
    }

    public List<Voter> populateDistrict(Polygon2D polygon, List<Voter> totalVoters){
        List<Voter> districtVoters = new ArrayList<Voter>();
        for (int i = 0; i < totalVoters.size(); i++){
            if (polygon.contains(totalVoters.get(i).getLocation())){
                districtVoters.add(totalVoters.get(i));
            }
        }
        return districtVoters;
    }
}
