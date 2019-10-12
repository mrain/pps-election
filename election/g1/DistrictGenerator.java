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

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        numDistricts = 243 / repPerDistrict;
        if (repPerDistrict != 3) System.out.println("WHY IS -rep != 3???");

        int rows = 30; // how small should the municipals be? there are n^2 of them
        Map<Pair<Integer, Integer>, Polygon2D> municipalMap = generateTriangularMunicipals(rows, repPerDistrict);

        Set<Pair<Integer, Integer>> remainingCoordinates = new HashSet<>(municipalMap.keySet());
        List<Pair<Integer, Integer>> startingCoordinates = getRandomCoordinates(municipalMap.keySet(), numDistricts);
        System.out.println("Number of starting coordinates: " + startingCoordinates.size());
        remainingCoordinates.removeAll(startingCoordinates);
        System.out.println("Number of remaining coordinates: " + remainingCoordinates.size());

        List<Municipal> municipals = convertCoordinatesToMunicipals(startingCoordinates, municipalMap);

        while(remainingCoordinates.size() > 0) {
            System.out.println("Number of remaining coordinates: " + remainingCoordinates.size());
            // this.printMunicipals(municipals, false);
            int numRemainingCoordinates = remainingCoordinates.size();
            for (Municipal muni : municipals) {
                List<Pair<Integer, Integer>> neighbors = muni.getNeighboringCoordinates();
                if (neighbors.size() == 0) continue;
                Collections.shuffle(neighbors);
                Pair<Integer, Integer> newNeighbor = null;
                for (Pair<Integer, Integer> neighbor : neighbors) {
                    Polygon2D neighborPolygon2D = municipalMap.get(neighbor);
                    if (remainingCoordinates.contains(neighbor)) {
                    //  && !muni.containsAllVertices(neighborPolygon2D)
                        newNeighbor = neighbor;
                        break;
                    }
                }
                if (newNeighbor != null) {
                    boolean success = muni.add(new Pair<Pair<Integer, Integer>, Polygon2D>(newNeighbor, municipalMap.get(newNeighbor)));
                    if (success) {
                        remainingCoordinates.remove(newNeighbor);
                    } else {
                        System.out.println("Could not add a newNeighbor");
                    }
                }
            }
            // if(numRemainingCoordinates == remainingCoordinates.size()) {
            //     this.printRemainingCoordinates(remainingCoordinates);
            //     throw new RuntimeException("could not add all coordinates");
            // }
        }
        this.printMunicipals(municipals, true);
        List<Polygon2D> result = municipals.stream().map(m -> m.getPolygon()).collect(Collectors.toList());
        System.out.println("COMPLETE!");
        return result;
    }

    private void printRemainingCoordinates(Set<Pair<Integer, Integer>> setOfPairs) {
        System.out.println("Remaining Coordinates: " + setOfPairs.size());
        for (Pair<Integer, Integer> pair : setOfPairs) {
            System.out.print(" x: " + pair.getKey() + " y: " + pair.getValue());
        }
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

    private List<Municipal> convertCoordinatesToMunicipals(List<Pair<Integer, Integer>> coordinates, Map<Pair<Integer, Integer>, Polygon2D> municipalMap) {
        List<Municipal> municipals = new ArrayList<>();
        for (Pair<Integer, Integer> coordinate : coordinates) {
            Polygon2D polygon = municipalMap.get(coordinate);
            municipals.add(new Municipal(new Pair<Pair<Integer, Integer>, Polygon2D>(coordinate, polygon)));
        }
        return municipals;
    }

    private Map<Pair<Integer, Integer>, Polygon2D> generateTriangularMunicipals(int rows, int repPerDistrict) {
        Map<Pair<Integer, Integer>, Polygon2D> municipalMap = new HashMap<>();
        numDistricts = 243 / repPerDistrict;
        double height = scale / 2.0 * Math.sqrt(3);
        int intRows = rows;
        double doubleRows = (double) intRows;
        double hstep = scale / doubleRows;
        for (int i = 0; i < intRows; ++ i) {
            double top = height * (intRows - i) / doubleRows;
            double btm = top - height / doubleRows;
            double left = scale / 2 - hstep / 2 * (i + 1);
            for (int j = 0; j <= i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                Pair<Integer, Integer> coordinates = new Pair<>(2*i, j);
                // System.out.println(" x: " + 2*i + " y: " + j);
                polygon.append(left + hstep * j, btm);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep / 2, top);
                municipalMap.put(coordinates, polygon);
            }
            for (int j = 0; j < i; ++ j) {
                Polygon2D polygon = new Polygon2D();
                Pair<Integer, Integer> coordinates = new Pair<>(2*i-1, j);
                // System.out.println(" x: " + (2*i-1) + " y: " + j);
                polygon.append(left + hstep * j + hstep / 2, top);
                polygon.append(left + hstep * j + hstep, btm);
                polygon.append(left + hstep * j + hstep * 3 / 2, top);
                municipalMap.put(coordinates, polygon);
            }
        }
        return municipalMap;
    }

    private List<Pair<Integer, Integer>> getRandomCoordinates(Set<Pair<Integer, Integer>> coordinates, int n) {
        List<Pair<Integer, Integer>> list = new ArrayList<>(coordinates);
        Collections.shuffle(list);
        return list.subList(0, n);
    }
    
}
