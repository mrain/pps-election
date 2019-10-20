package election.g9;

import java.awt.geom.*;
import java.io.*;
import java.util.*;
import election.sim.*;
import java.io.*; 
import java.text.DecimalFormat;


public class Calculator {
    public static void main(String[] args) {
        List<Polygon2D> dList = new ArrayList<>();
        List<Voter> vList = new ArrayList<>();
        //System.out.println(loadDistricts(dList));
        //System.out.println(loadVoter(vList));
        loadDistricts(dList);
        loadVoter(vList);
        int[] totalN = new int[3];
        double[] totalP = new double[3];
        int[] totalVotes = new int[3];
        int i = 1;
        int[] rep = new int[3];
        for(Polygon2D p : dList) {
            int[] num = getTotalVoters(p, vList);
            int total = num[0] + num[1] + num[2];
            totalVotes[0] += num[0];
            totalVotes[1] += num[1];
            totalVotes[2] += num[2];
            double one = Double.valueOf(num[0])/Double.valueOf(total);
            double two = Double.valueOf(num[1])/Double.valueOf(total);
            double three = Double.valueOf(num[2])/Double.valueOf(total);
            DecimalFormat df = new DecimalFormat("##.##%");
            String o = df.format(one);
            String t = df.format(two);
            String th = df.format(three);
            //System.out.println(i++ + ": " + num[0] + ", " + num[1] + ", " + num[2]);

            double[] waste = new double[3];
            waste[0] = one;
            waste[1] = two;
            waste[2] = three;

            for(int j = 0; j < 3; j++) {
                int max = getMax(waste);
                if(waste[max] >= .25) {
                    rep[max]++;
                    waste[max] -= .25;
                }
                else {
                    waste[max] = 0;
                    rep[max]++;
                }
            }

            double totalWaste = waste[0] + waste[1] + waste[2];
            double voteWasted = total * totalWaste;
            int iVote = (int)voteWasted;
            totalP[0] += waste[0];
            totalP[1] += waste[1];
            totalP[2] += waste[2];

            totalN[0] += (int)(waste[0] * total);
            totalN[1] += (int)(waste[1] * total);
            totalN[2] += (int)(waste[2] * total);

            System.out.println(i++ + ": " + o + ", " + t + ", " + th + " Total: " + total);
            String wo = df.format(waste[0]);
            String wt = df.format(waste[1]);
            String wth = df.format(waste[2]);
            String wp = df.format(totalWaste);

            System.out.println("wasted: " + wo +", " + wt + ", " + wth + " Wasted %: " + wp + " Wasted Votes: " + iVote);
        }
        int totalVote = totalVotes[0] + totalVotes[1] + totalVotes[2];
        int[] popRep = new int[3];
        popRep[0] = (int)((Double.valueOf(totalVotes[0]) / Double.valueOf(totalVote)) * 243.);
        popRep[1] = (int)((Double.valueOf(totalVotes[1]) / Double.valueOf(totalVote)) * 243.);
        popRep[2] = (int)((Double.valueOf(totalVotes[2]) / Double.valueOf(totalVote)) * 243.);
        System.out.println();
        System.out.println("Total representatives: " + rep[0] + ", " + rep[1] + ", " + rep[2]);
        System.out.println("Pop representatives: " + popRep[0] + ", " + popRep[1] + ", " + popRep[2]);
        System.out.println("Total Votes: " + totalVotes[0] + ", " + totalVotes[1] + ", " + totalVotes[2]);
        System.out.println("TotalP: " + totalP[0] + ", " + totalP[1] + ", " + totalP[2]);
        System.out.println("TotalN: " + totalN[0] + ", " + totalN[1] + ", " + totalN[2]);

        double[] totalTRep = new double[3];
        totalTRep[0] = totalN[0] / rep[0];
        totalTRep[1] = totalN[1] / rep[1];
        totalTRep[2] = totalN[2] / rep[2];
        System.out.println("TotalN to Rep: " + totalTRep[0] + ", " + totalTRep[1] + ", " + totalTRep[2]);

        double[] popTRep = new double[3];
        popTRep[0] = totalN[0] / popRep[0];
        popTRep[1] = totalN[1] / popRep[1];
        popTRep[2] = totalN[2] / popRep[2];
        System.out.println("TotalN to Rep: " + popTRep[0] + ", " + popTRep[1] + ", " + popTRep[2]);
    }

    public static int getMax(double[] input) {
        double max = 0;
        int index = 0;
        for(int i = 0; i < 3; i++) {
            if(input[i] > max) {
                max = input[i];
                index = i;
            }
        }
        return index;
    }

    public static int[] getTotalVoters(Polygon2D district, List<Voter> vList) {
        int[] count = new int[3];
        for(Voter v: vList) {
            if(district.contains(v.getLocation())) {
                double max = 0;
                int index = 0;
                for(int i = 0; i < 3; i++) {
                    if(v.getPreference().get(i) > max) {
                        max = v.getPreference().get(i);
                        index = i;
                    }
                }
                count[index]++;
            }
        }
        return count;
    }

    public static int loadDistricts(List<Polygon2D> list) {
        try
        {
            FileReader input = new FileReader("/Users/Daniel/4444/temp/election/g9/sriDistrict");
            BufferedReader bufRead = new BufferedReader(input);
            String myLine = null;    
            while ( (myLine = bufRead.readLine()) != null)
            {    
                String[] parts = myLine.split(" ");
                int size = Integer.parseInt(parts[0]);
                Polygon2D polygon = new Polygon2D();
                for(int i = 0; i < size; i++) {
                    Double x = Double.parseDouble(parts[2 * i + 1]);
                    Double y = Double.parseDouble(parts[2 * i + 2]);
                    polygon.append(x, y);
                }
                list.add(polygon);

            }
            return list.size();
            //for(Polygon2D p: list) System.out.println(p);
        }
        catch (FileNotFoundException ex)  
        {
            // insert code to run when exception occurs
            //System.out.println("is this happening?");
        }
        catch (IOException ex) 
        {
            //System.out.println("or this?");
        }
        return 0;
    }

    public static int loadVoter(List<Voter> list) {
        try
        {
            FileReader input = new FileReader("/Users/Daniel/4444/temp/election/g9/sriPop");
            BufferedReader bufRead = new BufferedReader(input);
            String myLine = null;    
            while ( (myLine = bufRead.readLine()) != null)
            {    
                String[] parts = myLine.split(" ");
                Double x = Double.parseDouble(parts[0]);
                Double y = Double.parseDouble(parts[1]);
                List<Double> pref = new ArrayList<Double>();
                pref.add(Double.parseDouble(parts[2]));
                pref.add(Double.parseDouble(parts[3]));
                pref.add(Double.parseDouble(parts[4]));
                Voter voter = new Voter(new Point2D.Double(x, y), pref);
                list.add(voter);
               // System.out.println(voter);
            }
            return list.size();
            //for(Polygon2D p: list) System.out.println(p);
        }
        catch (FileNotFoundException ex)  
        {
            // insert code to run when exception occurs
            //System.out.println("is this happening?");
        }
        catch (IOException ex) 
        {
            //System.out.println("or this?");
        }
        return 0;
    }


}