package election.g3;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class MapGenerator implements election.sim.MapGenerator {

	private static final double CITY_INFLUENCE = 1000.0;
	
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
        
        List<List<Double>> cityPreferences = getCityPreferences(numCities, numParties);
        
        for(int i = 0; i < numVoters; i++) {
        	int cityId = random.nextInt(numCities);
        	Point2D.Double cityLocationForVoter = locations.get(cityId);
        	List<Double> voterCityPreferences = cityPreferences.get(cityId);
        	double x, y;
        	double distance;
        	do {
            	double angle = random.nextDouble() * 2 * Math.PI;
        		distance = random.nextGaussian() * 300;
            	x = cityLocationForVoter.x + distance * Math.cos(angle);
            	y = cityLocationForVoter.y + distance * Math.sin(angle);
        	} while (!triangle.contains(x, y));
        	        	
        	List<Double> preferences = new ArrayList<>();
        	for(int j = 0; j < numParties; j++) {
        		preferences.add(CITY_INFLUENCE*voterCityPreferences.get(j)/Math.pow(distance, 2) + random.nextDouble());
        	}
        	
            voters.add(new Voter(new Point2D.Double(x, y), scalePreferences(preferences)));
        }
        
        return voters;
    }
    
    protected List<List<Double>> getCityPreferences(int numCities, int numParties) {
    	List<List<Double>> preferences = new ArrayList<>();
    	for (int i = 0; i < numCities; i++) {
    		List<Double> cityPreferences = new ArrayList<>();
    		for (int j = 0; j < numParties; j++) {
    			cityPreferences.add(random.nextDouble());
    			System.out.println("City " + i + ": " + cityPreferences.get(cityPreferences.size() - 1));
    		}
    		preferences.add(cityPreferences);
    	}
    	return preferences;
    }
    
    public List<Point2D.Double> getCityLocations(int numCities) {
		List<Point2D.Double> cities = new ArrayList<>();
		
    	for(int i = 0; i < numCities; i++) {
            double x, y;
            do {
                x = random.nextGaussian() * 300.0 + 500.0;
                y = random.nextDouble() * Math.min(x, 1000.0 - x) * Math.sqrt(3);
            } while (!triangle.contains(x, y));
            cities.add(new Point2D.Double(x, y));
    	}
    	
    	return cities;
    }
    
    protected List<Double> scalePreferences(List<Double> preferences) {
    	double max = Double.MIN_VALUE;
    	for (int i = 0; i < preferences.size(); i++) {
    		max = Math.max(preferences.get(i), max);
    	}
    	if (max > 1) {
    		for (int i = 0; i < preferences.size(); i++) {
        		preferences.set(i, preferences.get(i) / max);
        	}
    	}
    	return preferences;
    }
    
    protected List<Double> getVoterCityPreference(Point2D.Double voterPos, List<List<Double>> cityPreferences) {
    	
    	return null;
    }
}