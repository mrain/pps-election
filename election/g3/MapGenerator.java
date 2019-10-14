package election.g3;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class MapGenerator implements election.sim.MapGenerator {

    private Random random;
    private Path2D triangle;
    private List<TerritoryObject> territoryObjects = new ArrayList<>();
    
    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> voters = new ArrayList<Voter>();
        random = new Random();
        
        triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();
        
        assignTerritoryObjects(numVoters);
        
        for(TerritoryObject territory : territoryObjects) {
        	int population = territory.population;
        	Point2D.Double centroid = territory.centroid;
        	double radius = territory.radius;
            for(int i = 0; i < population; i++) {
            	double x, y;
            	double distance;
            	do {
                	double angle = random.nextDouble() * 2 * Math.PI;
            		distance = random.nextDouble() * radius;
                	x = centroid.x + distance * Math.cos(angle);
                	y = centroid.y + distance * Math.sin(angle);
            	} while (!triangle.contains(x, y));
            	        	
            	List<Double> preferences = new ArrayList<>();
                territory.voters.add(new Voter(new Point2D.Double(x, y), preferences));
            }
        }
        for(TerritoryObject territory : territoryObjects) {
        	int population = territory.population;
        	int maxVotersForParty1 = (int) Math.floor(population * territory.party1Percentage);
        	int maxVotersForParty2 = numParties == 2 ? population - maxVotersForParty1 : (int) Math.floor(population * territory.party2Percentage);
        	int maxVotersForParty3 = numParties == 3 ? population - maxVotersForParty1 - maxVotersForParty2 : 0;
        	int voterIndex = 0;
        	int maxVotersForMajorityParty = (int) Math.max(maxVotersForParty1, Math.max(maxVotersForParty2, maxVotersForParty3));
        	int maxVotersForSecondaryParty, maxVotersForTernaryParty;
        	int majorityPartyNumber, secondaryPartyNumber, ternaryPartyNumber;

        	if(maxVotersForMajorityParty == maxVotersForParty1) {
        		majorityPartyNumber = 0;
            	maxVotersForSecondaryParty = (int) Math.max(maxVotersForParty2, maxVotersForParty3);
            	if(maxVotersForSecondaryParty == maxVotersForParty2) {
            		secondaryPartyNumber = 1;
            		maxVotersForTernaryParty = maxVotersForParty3;
            		ternaryPartyNumber = 2;
            	}
            	else {
            		secondaryPartyNumber = 2;
            		maxVotersForTernaryParty = maxVotersForParty2;
            		ternaryPartyNumber = 1;
            	}
        	}
        	else if(maxVotersForMajorityParty == maxVotersForParty2) {
        		majorityPartyNumber = 1;
            	maxVotersForSecondaryParty = (int) Math.max(maxVotersForParty1, maxVotersForParty3);
            	if(maxVotersForSecondaryParty == maxVotersForParty1) {
            		secondaryPartyNumber = 0;
            		maxVotersForTernaryParty = maxVotersForParty3;
            		ternaryPartyNumber = 2;
            	}
            	else {
            		secondaryPartyNumber = 2;
            		maxVotersForTernaryParty = maxVotersForParty1;
            		ternaryPartyNumber = 0;
            	}
        	}
        	else {
        		majorityPartyNumber = 2;
            	maxVotersForSecondaryParty = (int) Math.max(maxVotersForParty1, maxVotersForParty2);
            	if(maxVotersForSecondaryParty == maxVotersForParty1) {
            		secondaryPartyNumber = 0;
            		maxVotersForTernaryParty = maxVotersForParty2;
            		ternaryPartyNumber = 1;
            	}
            	else {
            		secondaryPartyNumber = 1;
            		maxVotersForTernaryParty = maxVotersForParty1;
            		ternaryPartyNumber = 0;
            	}
        	}

        	for(int i = 0; i < maxVotersForMajorityParty; i++) {
        		Voter voter = territory.voters.get(voterIndex);
            	List<Double> preferences = voter.getPreference();
            	addPreferences(numParties, preferences, majorityPartyNumber);
            	voters.add(voter);
            	voterIndex++;
        	}
        	for(int i = 0; i < maxVotersForSecondaryParty; i++) {
        		Voter voter = territory.voters.get(voterIndex);
            	List<Double> preferences = voter.getPreference();
            	addPreferences(numParties, preferences, secondaryPartyNumber);
            	voters.add(voter);
            	voterIndex++;
        	}
        	if(ternaryPartyNumber != -1 && maxVotersForTernaryParty > 0) {
            	for(int i = 0; i < maxVotersForTernaryParty; i++) {
            		Voter voter = territory.voters.get(voterIndex);
                	List<Double> preferences = voter.getPreference();
                	addPreferences(numParties, preferences, ternaryPartyNumber);
                	voters.add(voter);
                	voterIndex++;
            	}        		
        	}
        }

        return voters;
    }
    
    private void assignTerritoryObjects(int numVoters) {
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(469, 772), 93.75, 17553, 0.0366, 0.1276, 0.8358)); // Tamil Nadu
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(563, 804), 93.75, 8128, 0.1293, 0.3727, 0)); // Kerala
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(406, 647), 93.75, 12061, 0.0096, 0.0, 0)); // Andhra Pradesh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(563, 616), 140.625, 14864, 0.5138, 0.3188, 0.0)); // Karnataka
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(469, 522), 70.3125, 8516, 0.1945, 0.2948, 0.0)); // Telangana
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(563, 397), 187.5, 27340, 0.2759, 0.1627, 0.0)); // Maharashtra
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(531, 219), 187.5, 17669, 0.5800, 0.3450, 0.0)); // Madhya Pradesh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(781, 250), 140.625, 14704, 0.6221, 0.3211, 0.0)); // Gujarat
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(844, 125), 187.5, 16677, 0.5847, 0.3424, 0.0)); // Rajasthan
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(906, 31), 70.3125, 6749, 0.0963, 0.4012, 0.0)); // Punjab
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(844, 0), 70.3125, 1670, 0.6911, 0.2730, 0.0)); // Himachal Pradesh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(781, 0), 70.3125, 6167, 0.5802, 0.2842, 0.0)); // Haryana
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(719, 0), 70.3125, 2453, 0.6101, 0.3140, 0.0)); // Uttarakhand
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(531, 63), 140.625, 48613, 0.4956, 0.0631, 0.0)); // Uttar Pradesh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(406, 63), 93.75, 25326, 0.2358, 0.0770, 0.0)); // Bihar
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(313, 31), 46.875, 148, 0.0, 0, 0.0)); // Sikkim
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(281, 47), 46.875, 893, 0.4903, 0.2534, 0.0)); // Tripura
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(344, 188), 70.3125, 8025, 0.5096, 0.1563, 0.0)); // Jharkhand
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(344, 469), 93.75, 10212, 0.3837, 0.1381, 0.0)); // Odisha
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(406, 313), 140.625, 6215, 0.5070, 0.4091, 0.0)); // Chhattisgarh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(250, 313), 70.3125, 22207, 0.4025, 0.0561, 0.0)); // West Bengal 
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(219, 16), 46.875, 721, 0.0793, 0.4828, 0.0)); // Meghalaya
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(250, 188), 46.875, 266, 0.0, 0, 0.0)); // Mizoram
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(188, 94), 70.3125, 7592, 0.3605, 0.3544, 0.0)); // Assam
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(63, 31), 70.3125, 336, 0.5822, 0.2069, 0.0)); // Arunachal Pradesh
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(94, 125), 46.875, 481, 0.0, 0.4811, 0.0)); // Nagaland
    	territoryObjects.add(new TerritoryObject(new Point2D.Double(156, 188), 46.875, 625, 0.3422, 0.2463, 0.0)); // Manipur
 
        int totalPopulation = 0;
        for(TerritoryObject territoryObject : territoryObjects)
            totalPopulation += territoryObject.population;
        for(int i = 0; i < numVoters - totalPopulation; i++)
            territoryObjects.get(random.nextInt(territoryObjects.size())).population++;

        for(int i = 0; i < territoryObjects.size(); i++)
            System.out.println("Territory " + (i + 1) + ": " + territoryObjects.get(i).population + " voters");

    }
    
    private void addPreferences(int numParties, List<Double> preferences, int partyNumber) {
		if(numParties == 2) {
    		switch(partyNumber) {
    		case 0: {
    			preferences.add(random.nextDouble() / 2 + 0.5);
    			preferences.add(random.nextDouble() / 2);
    			break;
    		}
    		case 1: {
    			preferences.add(random.nextDouble() / 2);
    			preferences.add(random.nextDouble() / 2 + 0.5);
    			break;
    		}
    		}
		}
		else if(numParties == 3) {
    		switch(partyNumber) {
    		case 0: {
    			preferences.add(random.nextDouble() / 2 + 0.5);
    			preferences.add(random.nextDouble() / 2);
    			preferences.add(random.nextDouble() / 2);
    			break;
    		}
    		case 1: {
    			preferences.add(random.nextDouble() / 2);
    			preferences.add(random.nextDouble() / 2 + 0.5);
    			preferences.add(random.nextDouble() / 2);
    			break;
    		}
    		case 2: {
    			preferences.add(random.nextDouble() / 2);
    			preferences.add(random.nextDouble() / 2);
    			preferences.add(random.nextDouble() / 2 + 0.5);
    			break;
    		}
    		}
		}

    }
    
    class TerritoryObject {
    	
    	public Point2D.Double centroid;
    	public double radius;
    	public int population;
    	public double party1Percentage;
    	public double party2Percentage;
    	public double party3Percentage;
    	public List<Voter> voters = new ArrayList<>();
    	
    	TerritoryObject() {}

    	TerritoryObject(Point2D.Double centroid,
    					double radius,
    					int population,
    					double party1Percentage,
    					double party2Percentage,
    					double party3Percentage) {
    		this.centroid = centroid;
    		this.radius = radius;
    		this.population = population;
    		this.party1Percentage = party1Percentage;
    		this.party2Percentage = party2Percentage;
    		this.party3Percentage = party3Percentage;
    	}
    }
}