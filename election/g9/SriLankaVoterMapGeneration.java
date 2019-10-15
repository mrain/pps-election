package election.g9;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import election.sim.Voter;

public class SriLankaVoterMapGeneration implements election.sim.MapGenerator{

    private static double scale = 1000.0;

    private static String PATH_TO_POPULATION_COORDINATES = "<Path to population coordinates>"; // Output of PopulationDistributionCoordinateGenerator
    private static String PATH_TO_PARTY_COORDINATES = "<Path to party coordinates>"; // Output of PartyDistributionCoordinateGenerator
    
    private static int COLOR_CODE_COUNT = 5;
    private static KDTree kdtParty;
    private static KDTree kdtPopulation;
    //private static List<KDTree> kdtPopulation;
    
    private static int TOTAL_VOTERS = 333333;
    private static int RANDOM_POINTS = 33333;
    private static int TOTAL_MINUS_RANDOM_VOTERS = 333333 - RANDOM_POINTS;
    private static int PARTY1_DIST = (int)(TOTAL_MINUS_RANDOM_VOTERS * 0.4928);
    private static int PARTY2_DIST = (int)(TOTAL_MINUS_RANDOM_VOTERS * 0.4573);
    private static int PARTY3_DIST = (int)(TOTAL_MINUS_RANDOM_VOTERS * 0.0499);
    
    static int counter = 0;
    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
    	try {
			init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        List<Voter> ret = new ArrayList<Voter>();
        Random random = new Random();

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        for (int i = 0; i < numVoters - RANDOM_POINTS;) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            
            int rand = random.nextInt(10);
            double[] coordinates = {x, y};
            KDNode kdn = kdtPopulation.find_nearest(coordinates);
            KDNode kdnParty = kdtParty.find_nearest(coordinates);
            int region = Character.getNumericValue(kdn.color);
            int party = 0;
            if(kdnParty.color == 'g') {
            	party = 1;
            }else if(kdnParty.color == 'b') {
            	party = 2;
            }else {
            	party = 3;
            }
            /*if(region == 2) {
            	region = 1;
            }
            
            if(region == 4) {
            	region = 5;
            }*/
            
            if(rand / region > 2) {
            	continue;
            }else {
            	
            }
            
            if(PARTY1_DIST > 0 && party == 1) {
            	PARTY1_DIST --;
            }else if(PARTY2_DIST > 0 && party == 2) {
            	PARTY2_DIST --;
            }else if(PARTY3_DIST > 0 && party == 3) {
            	PARTY3_DIST --;
            }else {
            	continue;
            }
            
            
            i++;
            List<Double> pref = getPrefBasedOnMapCoordinates(x, y, numParties);
            //for (int j = 0; j < numParties; ++ j)
            //    pref.add(random.nextDouble());
            ret.add(new Voter(new Point2D.Double(x, y), pref));
            System.out.println(i + " " +  PARTY1_DIST + " " + PARTY2_DIST + " " + PARTY3_DIST);
        }
        for (int i = 0; i < RANDOM_POINTS; i++) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            
            List<Double> pref = new ArrayList<Double>();
            for (int j = 0; j < numParties; ++ j)
                pref.add(random.nextDouble());
            ret.add(new Voter(new Point2D.Double(x, y), pref));
            System.out.println(i);
        }
        System.out.println(counter);
        return ret;
    }
    
    /**
     * Initialize parameters and data structures
     * @throws IOException
     */
    public void init() throws IOException {

    	BufferedReader reader = new BufferedReader(new FileReader(PATH_TO_PARTY_COORDINATES));
    	int lines = 0;
    	while (reader.readLine() != null) lines++;
    	reader.close();
    	kdtParty = new KDTree(lines);
    	
    	try (BufferedReader br = new BufferedReader(new FileReader(new File(PATH_TO_PARTY_COORDINATES)))) {
    		String line;
    	    while ((line = br.readLine()) != null) {
    	    	String[] tokens = line.split(", ");
    	    	double x = Double.parseDouble(tokens[0]);
    	    	double y = Double.parseDouble(tokens[1]);
    	    	char color = tokens[2].toCharArray()[0];
    	    	double coordinate[] = {x, y};
    	    	kdtParty.add(coordinate, color);
    	    }
    	} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// Initialize population points
    	reader = new BufferedReader(new FileReader(PATH_TO_POPULATION_COORDINATES));
    	lines = 0;
    	while (reader.readLine() != null) lines++;
    	reader.close();
    	kdtPopulation = new KDTree(lines);
    	
    	try (BufferedReader br = new BufferedReader(new FileReader(new File(PATH_TO_POPULATION_COORDINATES)))) {
    		String line;
    	    while ((line = br.readLine()) != null) {
    	    	String[] tokens = line.split(", ");
    	    	double x = Double.parseDouble(tokens[0]);
    	    	double y = Double.parseDouble(tokens[1]);
    	    	char color = tokens[2].toCharArray()[0];
    	    	double coordinate[] = {x, y};
    	    	kdtPopulation.add(coordinate, color);
    	    }
    	} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	/*kdtPopulation = new ArrayList<KDTree>();
    	List<Integer> counts = new ArrayList<Integer>(0);
    	for(int i = 0; i < COLOR_CODE_COUNT; i ++) {
    		counts.add(0);
    	}
    	reader = new BufferedReader(new FileReader("/home/shirish/Desktop/Semester-1/PPS/Project-2/coordinates_population.txt"));
    	String line;
    	while ((line = reader.readLine()) != null) {
    		String[] tokens = line.split(", ");
    		int counter = Integer.parseInt(tokens[2]);
    		counts.set(counter, counts.get(counter) + 1);
    	}
    	reader.close();

    	for(int i = 0; i < COLOR_CODE_COUNT; i ++) {
    		kdtPopulation.add(new KDTree(counts.get(i)));
    	}
    	
    	reader = new BufferedReader(new FileReader("/home/shirish/Desktop/Semester-1/PPS/Project-2/coordinates_population.txt"));
    	while ((line = reader.readLine()) != null) {
    		String[] tokens = line.split(", ");
	    	double x = Double.parseDouble(tokens[0]);
	    	double y = Double.parseDouble(tokens[1]);
	    	int counter = Integer.parseInt(tokens[2]);
	    	char density = tokens[2].toCharArray()[0];
	    	double coordinate[] = {x, y};
	    	KDTree kdt = kdtPopulation.get(counter);
	    	kdt.add(coordinate, density);
	    	kdtPopulation.set(counter, kdt);
    	}
    	reader.close();*/
    }
    
    /**
     * Get party preference based on region of the voter in Sri Lanka
     * (Works for three party)
     * @param voterX
     * @param voterY
     * @param numParties
     * @return
     */
    public List<Double> getPrefBasedOnMapCoordinates(double voterX, double voterY, int numParties) {
        List<Double> pref = new ArrayList<Double>();
        double[] coordinates = {voterX, voterY};
        KDNode kdn = kdtParty.find_nearest(coordinates);
        
        if(kdn.color == 'b') {
        	pref.add(1.0);
        	pref.add(0.0);
        	pref.add(0.0);
        }else if(kdn.color == 'g') {
        	pref.add(0.0);
        	pref.add(1.0);
        	pref.add(0.0);
        }else if(kdn.color == 'y'){
        	pref.add(0.0);
        	pref.add(0.0);
        	pref.add(1.0);
        	counter++;
        }
        
        return pref;
    }
    
    /**
     * Calculate euclidean distance between two points
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public double calculateDistanceBetweenPoints(double x1, double y1, double x2, double y2) {       
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }
}
