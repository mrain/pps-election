package election.g7;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class RandomMapGenerator implements election.sim.MapGenerator {
    private static double scale = 1000.0;

    @Override
    public List<Voter> getVoters(int numVoters, int numParties, long seed) {
        List<Voter> ret = new ArrayList<Voter>();
        Random random = new Random(seed);
        //draw a triangle
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(0., 0.);
        triangle.lineTo(1000., 0.);
        triangle.lineTo(500., 500. * Math.sqrt(3));
        triangle.closePath();

        Double[][] populationMap = null;
        Double[][] prefmatrix=null;
        //read population
        try {
            String path = new File("").getAbsolutePath();
            populationMap = readCSVFile(path + "/election/g7/population.csv");
        } catch (Exception e) {
            String path = new File("").getAbsolutePath();
            System.out.println("Cannot read population.csv file in " + path);
        }
        //read preference
        try {
            String path = new File("").getAbsolutePath();
            prefmatrix = readCSVFile(path + "/election/g7/preference.csv");
        } catch (Exception e) {
            String path = new File("").getAbsolutePath();
            System.out.println("Cannot read preference.csv file in " + path);
        }

        //Count popluation for triangle.
        int count = 0;
        int popRow = populationMap.length;
        int popCol = populationMap[0].length;

        double heightRatio = 500. * Math.sqrt(3) / popRow;
        double widthRatio = 1000.0 / popCol;

        for (int i = 0; i < popRow; i++) {
            for (int j = 0; j < popCol; j++) {
                if (triangle.contains(widthRatio*j, heightRatio*(popRow - i - 1)))
                    count += populationMap[i][j];
            }
        }

        // scaling the preference matrix
        int prefrow = prefmatrix.length;
        int prefcol = prefmatrix[0].length;

        double prefheightRatio = 500. * Math.sqrt(3) / prefrow;
        double prefwidthRatio = 1000.0 / prefcol;

        double populationRatio = numVoters * 1.0 / count;
        count = 0;
        // Distribute the people according to the denstiy map.
        for (int i = 0; i < popRow; i++) {
            for (int j = 0; j < popCol; j++) {
                double landY = heightRatio*(popRow - i - 1), landX = widthRatio*j;
                if (!triangle.contains(landX, landY))
                    continue;
                int num = (int)(populationMap[i][j] * populationRatio);
                for (int k = 0; k < num; k++) {
                    double x, y;
                    do {
                        x = landX;
                        y = landY;
                        // random x offset from -widthRatio/2 to widthRatio/2
                        double randomX = widthRatio * random.nextDouble() - widthRatio/2;
                        double randomY = heightRatio * random.nextDouble() - heightRatio/2;
                        x += randomX;
                        y += randomY;
                    } while (!triangle.contains(x, y));
                    List<Double> preference = new ArrayList<Double>();
                    int x_in_prefm=(int)(x/prefwidthRatio); int y_in_prefm=prefmatrix.length-(int)(y/prefheightRatio);
                    double redprob;
                    if (x_in_prefm<prefmatrix.length && y_in_prefm<prefmatrix[0].length && prefmatrix[x_in_prefm][y_in_prefm]!=-1)
                        redprob=prefmatrix[x_in_prefm][y_in_prefm];
                    else
                        redprob=0.5;
                    //random generate a double: if <= redprob: blue else blue
                    double identifier=random.nextDouble();
                    if(identifier<=redprob){
                        preference.add(1.0);
                        preference.add(0.0);
                    }//red person
                    else{
                        preference.add(0.0);
                        preference.add(1.0);
                    }//blue person
                    ret.add(new Voter(new Point2D.Double(x, y), preference));
                }
            }

        }
        //Randomly distribute the left people.
        while(ret.size() < numVoters) {
            double x, y;
            do {
                x = random.nextDouble() * 1000.0;
                y = random.nextDouble() * 900.0;
            } while (!triangle.contains(x, y));
            List<Double> preference = new ArrayList<Double>();
            int x_in_prefm=(int)(x/prefwidthRatio);int y_in_prefm=prefmatrix.length-(int)(y/prefheightRatio);
            double redprob;
            if (x_in_prefm<prefmatrix.length && y_in_prefm<prefmatrix[0].length && prefmatrix[x_in_prefm][y_in_prefm]!=-1)
                redprob=prefmatrix[x_in_prefm][y_in_prefm];
            else
                redprob=0.5;
            //random generate a double: if <= redprob: blue else blue
            double identifier= random.nextDouble();
            if(identifier<=redprob){preference.add(1.0); preference.add(0.0);}//red person
            else{preference.add(0.0); preference.add(1.0);}//blue person
            ret.add(new Voter(new Point2D.Double(x, y), preference));
        }
        return ret;
    }


    private Double[][] readCSVFile(String csvFileName) throws IOException {

        String line = null;
        BufferedReader stream = null;
        List<List<Double>> csvData = new ArrayList<List<Double>>();

        try {
            stream = new BufferedReader(new FileReader(csvFileName));
            while ((line = stream.readLine()) != null) {
                String[] cells = line.split(",");
                List<Double> dataLine = new ArrayList<Double>(cells.length);
                for (String data : cells)
                    dataLine.add(Double.valueOf(data));
                csvData.add(dataLine);
            }
        } finally {
            if (stream != null)
                stream.close();
        }

        Double[][] mapData = new Double[csvData.size()][csvData.get(0).size()];
        int i = 0;

        for (List<Double> dataLine : csvData) {
            mapData[i++] = dataLine.toArray(new Double[dataLine.size()]);
        }
        return mapData;
    }


}