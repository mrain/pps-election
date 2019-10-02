package election.g3;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class MapGenerator implements election.sim.MapGenerator {

    private Random random;
    private Path2D triangle;
    
    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> voters = new ArrayList<Voter>();
       
        triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();
        
        
        random = new Random(seed);
        int numCities = random.nextInt(6) + 5;
        List<Point2D.Double> locations = getCityLocations(numCities);        
        
        for(int i = 0; i < numVoters; i++) {
        	Point2D.Double cityLocationForVoter = locations.get(random.nextInt(numCities));
        	double x, y;
        	
        	do {
            	double angle = random.nextDouble() * 2 * Math.PI;
        		double distance = random.nextGaussian() * 300;
            	x = distance * Math.cos(angle);
            	y = distance * Math.sin(angle);
        	} while (!triangle.contains(x, y));
        	        	
        	List<Double> preferences = new ArrayList<>();
        	for(int j = 0; j < numParties; j++) {
        		preferences.add(random.nextDouble());
        	}
        	
            voters.add(new Voter(new Point2D.Double(x, y), preferences));
        }
        
        return voters;
    }
    
    public List<Point2D.Double> getCityLocations(int numCities) {
		List<Point2D.Double> cities = new ArrayList<>();
		
    	for(int i = 0; i < numCities; i++) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = Math.min(x, 1000.0 - x) * Math.sqrt(3);
            } while (!triangle.contains(x, y));
            cities.add(new Point2D.Double(x, y));
    	}
    	
    	return cities;
    }
}