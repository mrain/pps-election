package election.g1;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;

public class City{
    private int cityID;
    private double xcoord;
    private double ycoord;
    private int population;
    private double radius;
    private ArrayList<Double> twoCityPref;

    public City(int cityID, double xcoord, double ycoord, int population, double radius, ArrayList<Double> twoCityPref){
        this.cityID = cityID;
        this.xcoord = xcoord;
        this.ycoord = ycoord;
        this.population = population;
        this.radius = radius;
        this.twoCityPref = twoCityPref;
    }

    public int getCityID(){
        return cityID;
    }

    public ArrayList<Double> getCoords(){
        ArrayList<Double> list = new ArrayList<>();
        list.add(xcoord);
        list.add(ycoord);
        return list;
    }

    public int getPopulation(){
        return population;
    }

    public double getRadius(){
        return radius;
    }

    public ArrayList<Double> getTwoCityPref(){
        return twoCityPref;
    }

    public String toString(){
        return "City " + cityID + " xcoord " + xcoord + " ycoord " + ycoord;
    }
}