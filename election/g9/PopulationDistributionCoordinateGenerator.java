package election.g9;
import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 REF: https://stackoverflow.com/questions/18873017/where-to-get-the-jar-for-opencv
 * 
 */

/**
 * @author shirish
 *
 */
public class PopulationDistributionCoordinateGenerator {
	
	private static int Y_MAX = 866;
	private static int X_MAX = 1000;
	
	private static String file = "<Path to population distribution file>"; // maps/g9/population_distribution.png

	private static Color color1 = new Color(255, 255, 129);
	private static Color color2 = new Color(251, 209, 88);
	private static Color color3 = new Color(244, 170, 49);
	private static Color color4 = new Color(173, 83, 20);
	private static Color color5 = new Color(108, 0, 0);
	private static List<Color> colors = new ArrayList<Color>();
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		colors.add(color1);
		colors.add(color2);
		colors.add(color3);
		colors.add(color4);
		colors.add(color5);
		
        System.out.println("Welcome to OpenCV " + Core.VERSION);
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat m  = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("m = " + m.dump());
        
        //Instantiating the Imagecodecs class 
        Imgcodecs imageCodecs = new Imgcodecs(); 
       
        //Reading the Image from the file  
        Mat matrix = imageCodecs.imread(file, imageCodecs.IMREAD_COLOR); 
        System.out.println("Image Loaded");
        
        int resizeCounter= 0;
        for(int i = 1; i < matrix.rows(); i ++) {
        	ArrayList<ArrayList<Double>> coordinates = lrcoords(i, X_MAX);
        	double leftX = Math.floor(coordinates.get(0).get(0));
        	double rightX = Math.floor(coordinates.get(1).get(0));
        	double threelandRowDistance = Math.floor(rightX - leftX);
        	
        	boolean startFlag = true;
        	int startCol = 0;
        	int endCol = 0;
        	List<double[]> values = new ArrayList<double[]>();
        	//System.out.println(coordinates.get(0).get(1) + ": " + coordinates.get(0).get(0) + " " + coordinates.get(1).get(0));
        	for(int j = 0; j < matrix.cols(); j ++) {
	        	double[] bgrValues = matrix.get(i, j);
	        	boolean isNotEmpty = bgrValues[0] != 0.0 || bgrValues[1] != 0.0 || bgrValues[2] != 0.0;
	        	boolean isboundary = Math.abs(bgrValues[1] - bgrValues[0]) < 1; 
	        	if(isNotEmpty && !isboundary) {
	        		if(startFlag) {
	        			startCol = j;
	        			startFlag = false;
	        		}else {
	        			endCol = j;
	        			values.add(bgrValues);
	        			//result.put(i, j, bgrValues);
	        		}
	        		
	        		//System.out.println(test[2] + " " + test[1] + " " + test[0]);
	        	}
        	}
        	if(values.size() == 0) {
        		resizeCounter++;
        	}
        }
        Mat resizeimage = new Mat();
        Size sz = new Size(1000, 866 + resizeCounter);
        Imgproc.resize(matrix, resizeimage, sz );
        
        // Rotate Image to Match the triangle coordinate system
        Mat rotatedMat = new Mat(resizeimage.rows(), resizeimage.cols(), CvType.CV_8UC3);
        Core.rotate(resizeimage, rotatedMat, Core.ROTATE_180); //ROTATE_180 or ROTATE_90_COUNTERCLOCKWISE
        
        Mat result = new Mat(866, 1000, CvType.CV_8UC3, Scalar.all(255));
        
        FileWriter fileWriter = new FileWriter("/home/shirish/Desktop/Semester-1/PPS/Project-2/coordinates_population.txt");
		PrintWriter printWriter = new PrintWriter(fileWriter);
		int rowCounter  = 0;
        for(int i = 1; i < rotatedMat.rows(); i ++) {
        	boolean checkRowAddition = false;
        	System.out.println("************************");
        	ArrayList<ArrayList<Double>> coordinates = lrcoords(i, X_MAX);
        	double leftX = Math.floor(coordinates.get(0).get(0));
        	double rightX = Math.floor(coordinates.get(1).get(0));
        	double threelandRowDistance = Math.floor(rightX - leftX);
        	
        	boolean startFlag = true;
        	int startCol = 0;
        	int endCol = 0;
        	List<double[]> values = new ArrayList<double[]>();
        	//System.out.println(coordinates.get(0).get(1) + ": " + coordinates.get(0).get(0) + " " + coordinates.get(1).get(0));
        	for(int j = 0; j < rotatedMat.cols(); j ++) {
	        	double[] bgrValues = rotatedMat.get(i, j);
	        	boolean isNotEmpty = bgrValues[0] != 0.0 || bgrValues[1] != 0.0 || bgrValues[2] != 0.0;
	        	boolean isboundary = Math.abs(bgrValues[1] - bgrValues[0]) < 1; 
	        	if(isNotEmpty && !isboundary) {
	        		if(startFlag) {
	        			startCol = j;
	        			startFlag = false;
	        		}else {
	        			endCol = j;
	        			values.add(bgrValues);
	        			//System.out.println(bgrValues[0] + " - " + bgrValues[1] + " - " + bgrValues[2]);
	        			//result.put(i, j, bgrValues);
	        		}
	        		
	        		//System.out.println(test[2] + " " + test[1] + " " + test[0]);
	        	}
        	}
        	
        	Random random = new Random();
        	int mapRowDistance = (endCol - startCol);
        	for(int k = 0; k < threelandRowDistance; k ++) {
        		if(mapRowDistance <= threelandRowDistance) {
        			if(values.size() == 0 || values.size() == threelandRowDistance) {
        				break;
        			}
        			// Intrapolate
        			int rand = (int) (random.nextInt((int)mapRowDistance));
        			if(rand <= 0 || rand >= values.size() - 1) {
        				rand = 1;
        			}
        			double[] first = values.get(rand - 1);
        			double[] second = values.get(rand + 1);
        			double[] current = {0 ,0 , 0};
        			current[0] = (first[0] + second[0])/2;
        			current[1] = (first[1] + second[1])/2;
        			current[2] = (first[2] + second[2])/2;
        			current = getNearestColor(current);
        			values.add(rand, current);
        			mapRowDistance = values.size();
        		}else {
        			// Reduce size to match threeland size
        			int reduction = (int) (mapRowDistance - threelandRowDistance);
        			double ratio = values.size()/reduction;
        			
        			for(int j = 0; j < reduction; j++) {
        				int rand = (int) (random.nextInt(values.size()));
        				if(values.size() > threelandRowDistance) {
        					values.remove(rand);
        				}
        			}
        		}
        	}
        	
			for(int k = 0; k < values.size(); k++) {
				if(values.size() != 0) {
					result.put(i, (int)rightX - k, values.get(k));
					double[] val = values.get(k);
					String color = getNearestColorCode(val);
					printWriter.println(((int)rightX - k) + ", " + rowCounter + ", " + color);
					checkRowAddition = true;
				}
			}
			if(checkRowAddition)
				rowCounter ++;
			printWriter.flush();
			
        	System.out.println(threelandRowDistance + ": " + (endCol - startCol) + " " + values.size());
        }
        printWriter.close();
        
        /*try {
			FileWriter fileWriter = new FileWriter("/home/shirish/Desktop/Semester-1/PPS/Project-2/coordinates.txt");
			PrintWriter printWriter = new PrintWriter(fileWriter);
			for(int i = 1; i < result.rows(); i ++) {
				for(int j = 0; j < result.cols(); j ++) {
					double[] val = result.get(i, j);
					String color = getNearestColorCode(val);
					printWriter.println(i + ", " + j + ", " + color);
				}
			}
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        Imgcodecs.imwrite("/home/shirish/Desktop/Semester-1/PPS/Project-2/raw_population.jpg", rotatedMat);
        Imgcodecs.imwrite("/home/shirish/Desktop/Semester-1/PPS/Project-2/result_population.jpg", result);
    }
	
	/**
	 * Calculate nearest color
	 * @param color
	 * @return
	 */
	public static double[] getNearestColor(double[] colorValues) {
		Color color = new Color((int)colorValues[2], (int)colorValues[1], (int)colorValues[0]);
		Color finalColor = new Color(0, 0 ,0);
		double shortest = 1000000;
		List<Double> distances = new ArrayList<Double>();
		
		// https://stackoverflow.com/questions/6334311/whats-the-best-way-to-round-a-color-object-to-the-nearest-color-constant
		for(int j = 0; j < colors.size(); j++) {
			distances.add(colorDistance(color, colors.get(j)));
		}
		
		for(int i = 0; i < distances.size(); i++) {
			if(distances.get(i) < shortest) {
				shortest = distances.get(i);
				finalColor = colors.get(i);
			}
		}
		
		double[] finalColorCodes = {finalColor.getBlue(), finalColor.getGreen(), finalColor.getRed()};
		
		return finalColorCodes;
	}
	
	/**
	 * Calculate nearest color
	 * @param color
	 * @return
	 */
	public static String getNearestColorCode(double[] colorValues) {
		Color color = new Color((int)colorValues[2], (int)colorValues[1], (int)colorValues[0]);
		String finalColor = "";
		double shortest = 1000000;
		List<Double> distances = new ArrayList<Double>();
		
		// https://stackoverflow.com/questions/6334311/whats-the-best-way-to-round-a-color-object-to-the-nearest-color-constant
		for(int j = 0; j < colors.size(); j++) {
			distances.add(colorDistance(color, colors.get(j)));
		}
		
		for(int i = 0; i < distances.size(); i++) {
			if(distances.get(i) < shortest) {
				shortest = distances.get(i);
				finalColor = (i+1) + "";
			}
		}
		
		return finalColor;
	}
	
	/**
	 * Find color distance
	 * @param c1
	 * @param c2
	 * @return
	 */
	static double colorDistance(Color c1, Color c2)
	{
	    int red1 = c1.getRed();
	    int red2 = c2.getRed();
	    int rmean = (red1 + red2) >> 1;
	    int r = red1 - red2;
	    int g = c1.getGreen() - c2.getGreen();
	    int b = c1.getBlue() - c2.getBlue();
	    return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
	}
	
	/* lrcoords
     * returns a list that contains two entries, each a coordinate
     * the first coordinate is the left, the second is the right
     */
    private static ArrayList<ArrayList<Double>> lrcoords (double y, double xmax){
        ArrayList<ArrayList<Double>> lrcoords = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> lcoord = new ArrayList<>();
        lcoord.add(500 * y/(500 * Math.sqrt(3)));
        lcoord.add(y);
        lrcoords.add(lcoord);
        
        ArrayList<Double> rcoord = new ArrayList<>();
        rcoord.add(xmax - ((y * 500)/(500 * Math.sqrt(3))));
        rcoord.add(y);
        lrcoords.add(rcoord);
        
        return lrcoords;
    }

}
