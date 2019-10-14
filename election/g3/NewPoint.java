package election.g3;

import java.util.Random;
 
public class NewPoint {
 
    private double x = 0;
    private double y = 0;
    private int clusterNumber = 0;
 
    public NewPoint(double x, double y)
    {
        this.setX(x);
        this.setY(y);
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getX()  {
        return this.x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getY() {
        return this.y;
    }
    
    public void setCluster(int n) {
        this.clusterNumber = n;
    }
    
    public int getCluster() {
        return this.clusterNumber;
    }
    
    // Calculate the distance between two points
    protected static double distance(NewPoint p, NewPoint centroid) {
        return Math.sqrt(Math.pow((centroid.getY() - p.getY()), 2) + Math.pow((centroid.getX() - p.getX()), 2));
    }
    
    // Create random point
    protected static NewPoint createRandomPoint() {
    	Random random = new Random();
    	double x = random.nextDouble() * 1000;
        double y = random.nextDouble() * Math.min(x, 1000 - x) * Math.sqrt(3);
    	return new NewPoint(x, y);
    }
    
    public String toString() {
    	return "(" + x + "," + y + ")";
    }
}
