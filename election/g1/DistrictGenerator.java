package election.g1;

import java.util.*;
import java.util.stream.Collectors;
import election.sim.*;
import java.awt.geom.*;
import javafx.util.Pair;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private Map<Pair<Integer, Integer>, Municipal> municipalMap = new HashMap<>();
    private Map<Pair<Integer, Integer>, Set<Voter>> voterMap = new HashMap<>();
    private final int ROWS = 100;
    private int totalCount = 0;
    private int averagePopulation;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        numDistricts = 243 / repPerDistrict;
        averagePopulation = numVoters/numDistricts;

        this.generateVoterMap(voters);
        this.generateMunicipalMap(repPerDistrict, voters);

        Set<Pair<Integer, Integer>> remainingCoordinates = new HashSet<>(this.municipalMap.keySet());

        List<Pair<Integer, Integer>> startingCoordinates = getStartingCoordinates(voters, remainingCoordinates, numDistricts);

        System.out.println("Number of starting coordinates: " + startingCoordinates.size());
        remainingCoordinates.removeAll(startingCoordinates);
        System.out.println("Number of remaining coordinates: " + remainingCoordinates.size());

        List<Municipal> municipals = startingCoordinates.stream().map(c -> this.municipalMap.get(c)).collect(Collectors.toList());
        System.out.println(totalCount);
        for (int i = -10; i <= 10; i++) {
            combineMunicipals(municipals, remainingCoordinates, (int) (1+ (i*1.0)/100)*averagePopulation);
        }
        // this.printMunicipals(municipals, true);
        List<Polygon2D> result = municipals.stream().map(m -> m.getPolygon()).collect(Collectors.toList());
        System.out.println("COMPLETE!");
        System.out.println(voters.size());
        return result;
    }

    // combine more and more municipals subject to a maximum population.
    private void combineMunicipals(List<Municipal> municipals, Set<Pair<Integer, Integer>> remainingCoordinates, int maxPopulation) {
        while(remainingCoordinates.size() > 0) {
            System.out.println("Number of remaining coordinates: " + remainingCoordinates.size());
            int numRemainingCoordinates = remainingCoordinates.size();
            for (Municipal muni : municipals) {
                List<Pair<Integer, Integer>> neighbors = muni.getNeighboringCoordinates();
                neighbors.retainAll(remainingCoordinates);
                if (neighbors.size() == 0) continue;
                Collections.shuffle(neighbors);
                Pair<Integer, Integer> newNeighbor = null;
                for (Pair<Integer, Integer> neighbor : neighbors) {
                    Municipal neighborMunicipal = this.municipalMap.get(neighbor);
                    if (remainingCoordinates.contains(neighbor) && muni.canAdd(neighbor, neighborMunicipal, maxPopulation)) {
                        newNeighbor = neighbor;
                        break;
                    }
                }
                if (newNeighbor != null) {
                    Municipal muniToAdd = this.municipalMap.get(newNeighbor);
                    boolean success = muni.add(newNeighbor, muniToAdd.getPolygon(), muniToAdd.getVoters());
                    if (success) {
                        remainingCoordinates.remove(newNeighbor);
                    } else {
                        System.out.println("Could not add a newNeighbor");
                    }
                }
            }
            if(numRemainingCoordinates == remainingCoordinates.size()) {
                this.printRemainingCoordinates(remainingCoordinates);
                return;
            }
        }
    }

    private void printRemainingCoordinates(Set<Pair<Integer, Integer>> setOfPairs) {
        System.out.println("Remaining Coordinates: " + setOfPairs.size());
        for (Pair<Integer, Integer> pair : setOfPairs) {
            System.out.print(" x: " + pair.getKey() + " y: " + pair.getValue());
        }
        
        System.out.println();
        System.out.println();
    }

    private void printMunicipals(List<Municipal> municipals, boolean detailed) {
        int countTriangles = 0;
        for (Municipal muni : municipals) {
            countTriangles += muni.getNumTriangles();
            if (detailed) muni.print();
        }
        System.out.println("Total Municipals: " + countTriangles);
    }

    // private List<Municipal> convertCoordinatesToMunicipals(List<Pair<Integer, Integer>> coordinates, Map<Pair<Integer, Integer>, Polygon2D> municipalMap) {
    //     List<Municipal> municipals = new ArrayList<>();
    //     for (Pair<Integer, Integer> coordinate : coordinates) {
    //         Polygon2D polygon = municipalMap.get(coordinate);
    //         municipals.add(new Municipal(new Pair<Pair<Integer, Integer>, Polygon2D>(coordinate, polygon)));
    //     }
    //     return municipals;
    // }

    private void generateMunicipalMap(int repPerDistrict, List<Voter> originalVoters) {
        List<Voter> allVoters = new ArrayList<Voter>(originalVoters);
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        int intRows = ROWS;
        double doubleRows = (double) intRows;
        double hstep = scale / doubleRows;
        for (double i = 0.0; i < doubleRows; ++ i) {
            double top = height * (doubleRows - i) / doubleRows;
            double btm = top - height / doubleRows;
            double left = scale / 2 - hstep / 2 * (i + 1);
            for (double j = 0.0; j <= i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                Pair<Integer, Integer> coordinates = new Pair<>((int)(2*i), (int)j);
                // System.out.println(" x: " + 2*i + " y: " + j);
                polygon.append(left + hstep * j + hstep / 2.0, top);
                polygon.append(left + hstep * j, btm);
                polygon.append(left + hstep * j + hstep, btm);
                List<Voter> voters = new ArrayList<>();
                if (this.voterMap.containsKey(coordinates)) {                
                    for (Voter potentialVoter : this.voterMap.get(coordinates)) {
                        if (polygon.contains(potentialVoter.getLocation())) {
                            voters.add(potentialVoter);
                            totalCount++;
                        }
                    }
                } else {
                    System.out.println("Warning: Coordinate has no voters");
                }
                Municipal municipal = new Municipal(coordinates, polygon, voters);
                this.municipalMap.put(coordinates, municipal);
            }
            for (double j = 0.0; j < i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                Pair<Integer, Integer> coordinates = new Pair<>((int)(2*i-1), (int)j);
                // System.out.println(" x: " + (2*i-1) + " y: " + j);
                polygon.append(left + hstep * j + hstep * 3.0 / 2.0, top);
                polygon.append(left + hstep * j + hstep / 2.0, top);
                polygon.append(left + hstep * j + hstep, btm);
                List<Voter> voters = new ArrayList<>();
                if (this.voterMap.containsKey(coordinates)) {                
                    for (Voter potentialVoter : this.voterMap.get(coordinates)) {
                        if (polygon.contains(potentialVoter.getLocation())) {
                            voters.add(potentialVoter);
                            totalCount++;
                        }
                    }
                } else {
                    System.out.println("Warning: Coordinate has no voters");
                }
                Municipal municipal = new Municipal(coordinates, polygon, voters);
                this.municipalMap.put(coordinates, municipal);
            }
        }
    }

    private void generateVoterMap(List<Voter> voters) {
        for (Voter voter : voters) {
            List<Pair<Integer, Integer>> potentialCoordinates = getPotentialCoordinates(voter.getLocation());
            // System.out.println("Voter Location: " + voter.getLocation());
            // System.out.println("Potential Coordinates: "); 
            for (Pair<Integer, Integer> coordinate : potentialCoordinates) {
                // System.out.print(" x: " + coordinate.getKey() + " y: " + coordinate.getValue() + " ");
                if (!voterMap.containsKey(coordinate)) {
                    Set<Voter> voterSet = new HashSet<>();
                    voterMap.put(coordinate, voterSet);
                }
                voterMap.get(coordinate).add(voter);
            }
        }
        // System.out.println("Voter keys: " + voterMap.keySet().size());
        //  for (Pair<Integer, Integer> coordinate : voterMap.keySet()) {
        //      System.out.println(coordinate);
        //  }
    }

    private List<Pair<Integer, Integer>> getPotentialCoordinates(Point2D point) {
        // This inverts the triangle construction process a bit to map voters
        int intRows = ROWS;
        double doubleRows = (double) intRows;
        double x = point.getX();
        double y = point.getY();
        double height = scale / 2.0 * Math.sqrt(3);
        double triHeight = height / doubleRows; //height of small triangle
        double triSide = scale / doubleRows; //side lenth of small triangle

        // a little confusing that the x-coordinate is based on the y-value, but that's the way it is   
        int row = (int) (intRows - Math.ceil((doubleRows*y/height))); //row number for current point
        int xcoord = 2 * row;

        int triCount = row + 1; //number of triangles in current row
        double left = scale/2.0 - (triCount * triSide / 2); // x cord for left end of the row
        int ycoord = (int) Math.floor((x - left) / triSide);

        List<Pair<Integer, Integer>> potentialCoordinates = new ArrayList<>();
        potentialCoordinates.add(new Pair<>(xcoord, ycoord));
        potentialCoordinates.add(new Pair<>(xcoord-1, ycoord));
        potentialCoordinates.add(new Pair<>(xcoord-1, ycoord-1));
        return potentialCoordinates;
    }

    // private List<Pair<Integer, Integer>> getRandomCoordinatesAndThreeCorners(Set<Pair<Integer, Integer>> remainingCoordinates, int n) {
    //     List<Pair<Integer, Integer>> result = new ArrayList<>();
    //     Pair<Integer, Integer> top = new Pair<>(0,0);
    //     Pair<Integer, Integer> bottomLeft = new Pair<>(2*(ROWS-1), 0);
    //     Pair<Integer, Integer> bottomRight = new Pair<>(2*(ROWS-1), ROWS-1);
    //     result.add(top); remainingCoordinates.remove(top);
    //     result.add(bottomLeft); remainingCoordinates.remove(bottomLeft);
    //     result.add(bottomRight); remainingCoordinates.remove(bottomRight);
    //     List<Pair<Integer, Integer>> list = new ArrayList<>(remainingCoordinates);
    //     Collections.shuffle(list);
    //     result.addAll(list.subList(0, n-3));
    //     return result;
    // }

    private List<Pair<Integer, Integer>> getStartingCoordinates(List<Voter> voters, Set<Pair<Integer, Integer>> remainingCoordinates, int numDistricts) {
        // ONLY PICK 1 POTENTIAL COORDINATE FROM EACH VOTER
        // STOP PICKING COORDINATES AFTER YOU HAVE 81
        // DO NOT REMOVE COORDINATES FROM REMAINING COORDINATES IF NOT INCLUDED IN FIRST 81!!!
        // TODO: FIX THIS TOMORROW MORNING
        Collections.shuffle(voters);
        List<Pair<Integer, Integer>> startingCoordinates = new ArrayList<>();
        int i = 0;
        while (startingCoordinates.size() < numDistricts && i < voters.size()) {
            Voter voter = voters.get(i);
            for (Pair<Integer, Integer> potentialCoordinate : this.getPotentialCoordinates(voter.getLocation())) {
                if (remainingCoordinates.contains(potentialCoordinate)) {
                    startingCoordinates.add(potentialCoordinate);
                    remainingCoordinates.remove(potentialCoordinate);
                    break;
                }
            }
            i++;
        }
        return startingCoordinates;
    }
    
}
