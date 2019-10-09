package election.g1;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class MapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;
    private static int numCentersOfInfluence = 81;
    private ArrayList<City> cities = new ArrayList<>();

    private void populateCities(){
        try{
            FileReader pathToFile = new FileReader("election/g1/twoCities.csv");

            // create an instance of BufferedReader
            BufferedReader br = new BufferedReader(pathToFile);

            // read the first line from the text file
            String line = br.readLine();

            // loop until all lines are read
            while (line != null) {
                String[] attributes = line.split(",");

                City city = createCity(attributes);
                cities.add(city);

                // read next line before looping
                // if end of file reached, line would be null
                line = br.readLine();
            }
        }

        catch (Exception e) {
            System.out.println(e);
        }

    }
    private static City createCity(String[] metadata){
        int cityID = Integer.parseInt(metadata[0]);
        double ycoord = Double.parseDouble(metadata[1]);
        double xcoord = Double.parseDouble(metadata[2]);
        int population = Integer.parseInt(metadata[3]);
        double radius = Double.parseDouble(metadata[4]);
        ArrayList<Double> twoCityPref = new ArrayList<>();
        twoCityPref.add(Double.parseDouble(metadata[5]));
        twoCityPref.add(Double.parseDouble(metadata[6]));
        return new City(cityID, xcoord, ycoord, population, radius, twoCityPref);
    }

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        int noise = 100000;
        int n1 = (int) (noise * 0.60);
        int n2 = noise - n1;
        Random rand = new Random();
        populateCities();
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();
        List<Voter> voters = new ArrayList<>(); 
        for(int i = 0; i < n1; i++){
            double rand1 = Math.random();
            double rand2 = Math.random();
            List<Double> prefs = new ArrayList<>();
            if(rand1 > rand2){
                prefs.add(rand1);
                prefs.add(rand2);
            }
            else{
                prefs.add(rand2);
                prefs.add(rand1);
            }
            double x;
            double y;
            do{
                // double rp = r * Math.random();
                // yp = r * Math.random();
                // xp = Math.sqrt(Math.pow(rp, 2) - Math.pow(yp, 2));
                // yp = yp * neg[(int) (Math.random() * 2)]; 
                // xp = xp * neg[(int) (Math.random() * 2)]; 
                x = Math.random() * 1000;
                y = Math.random() * 900;
            }
            while(!triangle.contains(x, y));
            voters.add(new Voter(new Point2D.Double(x, y), prefs));
        }
        for(int i = 0; i < n2; i++){
            double rand1 = Math.random();
            double rand2 = Math.random();
            List<Double> prefs = new ArrayList<>();
            if(rand1 < rand2){
                prefs.add(rand1);
                prefs.add(rand2);
            }
            else{
                prefs.add(rand2);
                prefs.add(rand1);
            }
            double x;
            double y;
            do{
                // double rp = r * Math.random();
                // yp = r * Math.random();
                // xp = Math.sqrt(Math.pow(rp, 2) - Math.pow(yp, 2));
                // yp = yp * neg[(int) (Math.random() * 2)]; 
                // xp = xp * neg[(int) (Math.random() * 2)]; 
                x = Math.random() * 1000;
                y = Math.random() * 900;
            }
            while(!triangle.contains(x, y));
            voters.add(new Voter(new Point2D.Double(x, y), prefs));
        }
        for(City c : cities){
            int pTotal = c.getPopulation();
            double pref1 = c.getTwoCityPref().get(0);
            double pref2 = c.getTwoCityPref().get(1);
            int p1 = (int) (pTotal * pref1);
            int p2 = pTotal - p1;
            double x = c.getCoords().get(0);
            double y = c.getCoords().get(1);
            double r = c.getRadius() * 200;
            int[] neg = new int[2];
            neg[0] = -1;
            neg[1] = 1;

            for(int i = 0; i < p1; i++){
                double rand1 = Math.random();
                double rand2 = Math.random();
                List<Double> prefs = new ArrayList<>();
                if(rand1 > rand2){
                    prefs.add(rand1);
                    prefs.add(rand2);
                }
                else{
                    prefs.add(rand2);
                    prefs.add(rand1);
                }
                double xp;
                double yp;
                do{
                    // double rp = r * Math.random();
                    // yp = r * Math.random();
                    // xp = Math.sqrt(Math.pow(rp, 2) - Math.pow(yp, 2));
                    // yp = yp * neg[(int) (Math.random() * 2)]; 
                    // xp = xp * neg[(int) (Math.random() * 2)]; 
                    double rp = r * Math.abs(rand.nextGaussian()) / 1.5;
                    double theta = Math.random() * Math.PI * 2;
                    yp = rp * Math.sin(theta);
                    xp = rp * Math.cos(theta);
                    yp = yp * neg[(int) (Math.random() * 2)]; 
                    xp = xp * neg[(int) (Math.random() * 2)]; 
                }
                while(!triangle.contains(x + xp, y + yp));
                voters.add(new Voter(new Point2D.Double(x + xp, y + yp), prefs));
            }
            for(int i = 0; i < p2; i++){
                double rand1 = Math.random();
                double rand2 = Math.random();
                List<Double> prefs = new ArrayList<>();
                if(rand1 < rand2){
                    prefs.add(rand1);
                    prefs.add(rand2);
                }
                else{
                    prefs.add(rand2);
                    prefs.add(rand1);
                }
                double xp;
                double yp;
                do{
                    // double rp = r * Math.random();
                    // yp = r * Math.random();
                    // xp = Math.sqrt(Math.pow(rp, 2) - Math.pow(yp, 2));
                    // yp = yp * neg[(int) (Math.random() * 2)]; 
                    // xp = xp * neg[(int) (Math.random() * 2)]; 
                    double rp = r * Math.abs(rand.nextGaussian()) / 1.5;
                    double theta = Math.random() * Math.PI * 2;
                    yp = rp * Math.sin(theta);
                    xp = rp * Math.cos(theta);
                    yp = yp * neg[(int) (Math.random() * 2)]; 
                    xp = xp * neg[(int) (Math.random() * 2)]; 
                }
                while(!triangle.contains(x + xp, y + yp));
                voters.add(new Voter(new Point2D.Double(x + xp, y + yp), prefs));
            }
        }
        return voters;
    }
}
