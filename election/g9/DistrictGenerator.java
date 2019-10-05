package election.g9;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private double iLength = 1000 - 2*50 * Math.sqrt(3);
    private double iHeight = 500. * Math.sqrt(3) - 100.;

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;

        result.addAll(getInside(repPerDistrict));
        System.out.println(result.size());
        result.addAll(getOutside(repPerDistrict));
        System.out.println(result.size());
        return result;
    }

    public List<Polygon2D> getInside(int repPerDistrict) {
        double height = iHeight;
        double hstep = iLength / 6.0;
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        if (repPerDistrict == 3) {
            // 36 Districts;
            for (int i = 0; i < 6; ++ i) {
                double top = height * (6 - i) / 6.0;
                double btm = top - height / 6.0;
                double left = iLength / 2 - hstep / 2 * (i + 1);
                for (int j = 0; j <= i; ++ j) {
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(left + hstep * j, btm);
                    polygon.append(left + hstep * j + hstep, btm);
                    polygon.append(left + hstep * j + hstep / 2, top);
                    result.add(polygon);
                }
                for (int j = 0; j < i; ++ j) {
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(left + hstep * j + hstep / 2, top);
                    polygon.append(left + hstep * j + hstep, btm);
                    polygon.append(left + hstep * j + hstep * 3 / 2, top);
                    result.add(polygon);
                }
            }
        } else {
            // 108 districts
            Point2D top = new Point2D.Double(500., height);
            double step = iLength / (36 * 3);
            for (int i = 0; i < 36 * 3; ++ i) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(new Point2D.Double(step * i, 0.));
                polygon.append(new Point2D.Double(step * (i + 1), 0.));
                polygon.append(top);
                result.add(polygon);
            }
        }
        System.out.println(result.size());
        return result;
    }
    public List<Polygon2D> getOutside(int repPerDistrict) {
        double x = Math.sqrt(5000./15.*Math.sqrt(3)*(20.-Math.sqrt(3)));
        double y = Math.sqrt(5000./15.*Math.sqrt(3)*(20.-Math.sqrt(3)))/Math.sqrt(3);
        double h = 50.;
        double z = ((5000./15.*(20.-Math.sqrt(3))))/(y+h);
        double w = (1000.-(2.*(x+z)))/11.;
        

        System.out.println("x: "+ x + " y: " + y + " z: " + z);

        List<Polygon2D> result = new ArrayList<Polygon2D>();

        if(repPerDistrict == 3) {
            Polygon2D polygon = new Polygon2D();
            Polygon2D trap = new Polygon2D();

            polygon.append(0,0);
            polygon.append(x,0);
            polygon.append(x,y);
            result.add(polygon);

            System.out.println(polygon);


            trap.append(x,0);
            trap.append(x+z,0);
            trap.append(x,y);
            trap.append(x+z,h);
            result.add(trap);

            System.out.println(trap);

            System.out.println("HUH!");

            for(int i = 0; i < 11; i++) {
                Polygon2D temp = new Polygon2D();
                temp.append(x+z+w*(i),0);
                temp.append(x+z+w*(i),h);
                temp.append(x+z+w*(i+1),0);
                temp.append(x+z+w*(i+1),h);
                result.add(temp);
                System.out.println(temp);
            }

            polygon = new Polygon2D();
            trap = new Polygon2D();
            trap.append(x+z+w*(11), 0);
            trap.append(x+z+w*(11), h);
            trap.append(x+z+w*(11)+z, y);
            trap.append(x+z+w*(11)+z, 0);

            polygon.append(x+z+w*(11)+z, 0);
            polygon.append(x+z+w*(11)+z, y);
            polygon.append(x+z+w*(11)+z+x, 0);

            result.add(polygon);
            result.add(trap);

            System.out.println(polygon);
            System.out.println(trap);

            System.out.println("DONE!");

            //Adding in the left side
            double a = 60. * Math.PI/180.;
            double b = -30. * Math.PI/180.;
            double zx = z * Math.cos(a);
            double zy = z * Math.sin(a);
            double xx = x * Math.cos(a);
            double xy = x * Math.sin(a);
            double wx = w * Math.cos(a);
            double wy = w * Math.sin(a);

            System.out.println("xx: " + xx + " x: " + x);

            polygon = new Polygon2D();
            trap = new Polygon2D();
            Polygon2D polygon1 = new Polygon2D();
            Polygon2D trap1 = new Polygon2D();

            polygon.append(0,0);
            polygon.append(xx,xy);
            polygon.append(xx + y * Math.cos(b),xy + y * Math.sin(b));
            result.add(polygon);

            trap.append(xx, xy);
            trap.append(xx + y * Math.cos(b), xy + y * Math.sin(b));
            trap.append(xx+zx, xy+zy);
            trap.append(xx+zx + h * Math.cos(b), xy+zy + h * Math.sin(b));
            result.add(trap);

            System.out.println(polygon);
            System.out.println(trap);

            System.out.println("HUH!");

            for(int i = 0; i < 11; i++) {
                Polygon2D temp = new Polygon2D();
                temp.append(xx+zx+wx*(i), xy+zy+wy*(i));
                temp.append(xx+zx+wx*(i) + h * Math.cos(b), xy+zy+wy*(i) + h * Math.sin(b));
                temp.append(xx+zx+wx*(i+1),xy+zy+wy*(i+1));
                temp.append(xx+zx+wx*(i+1) + h * Math.cos(b) , xy+zy+wy*(i+1) + h * Math.sin(b));
                result.add(temp);
                System.out.println(temp);
            }

            System.out.println("YOOO");

            polygon = new Polygon2D();
            trap = new Polygon2D();
            trap.append(xx+zx+wx*(11), xy+zy+wy*(11));
            trap.append(xx+zx+wx*(11) + h * Math.cos(b), xy+zy+wy*(11) + h * Math.sin(b));
            trap.append(xx+zx+wx*(11) + zx, xy+zy+wy*(11) + zy);
            trap.append(xx+zx+wx*(11) * y * Math.cos(b), xy+zy+wy*(11) + y * Math.sin(b));

            polygon.append(xx+zx+wx*(11)+zx, xy+zy+wy*(11)+zy);
            polygon.append(xx+zx+wx*(11)+zx + y * Math.cos(b), xy+zy+wy*(11)+zy + y * Math.sin(b));
            polygon.append(xx+zx+wx*(11)+zx+xx, xy+zy+wy*(11)+zy+xy);

            result.add(polygon);
            result.add(trap);
            System.out.println(polygon);
            System.out.println(trap);

            System.out.println("DONE!");

            //Adding the last side 
            polygon = new Polygon2D();
            trap = new Polygon2D();

            polygon.append(0 + 2 * (500-0),0);
            polygon.append(xx+ 2 * (500-xx),xy);
            polygon.append(xx + y * Math.cos(b) + 2 * (500-(xx + y * Math.cos(b))),xy + y * Math.sin(b));
            result.add(polygon);

            trap.append(xx + 2 *(500- xx), xy);
            trap.append(xx + y * Math.cos(b) + 2 * (500 - (xx + y * Math.cos(b))), xy + y * Math.sin(b));
            trap.append(xx+zx + 2 * (500-(xx+zx)), xy+zy);
            trap.append(xx+zx + h * Math.cos(b) + 2 * (500-(xx+zx + h * Math.cos(b))), xy+zy + h * Math.sin(b));
            result.add(trap);
            System.out.println(polygon);
            System.out.println(trap);

            System.out.println("HUH!");

            for(int i = 0; i < 11; i++) {
                Polygon2D temp = new Polygon2D();
                temp.append(xx+zx+wx*(i) + 2 *(500-(xx+zx+wx*(i))), xy+zy+wy*(i));
                temp.append(xx+zx+wx*(i) + h * Math.cos(b) + 2 * (500-((xx+zx+wx*(i)) + h * Math.cos(b))), xy+zy+wy*(i) + h * Math.sin(b));
                temp.append(xx+zx+wx*(i+1) + 2 * (500 - (xx+zx+wx*(i+1))),xy+zy+wy*(i+1));
                temp.append(xx+zx+wx*(i+1) + h * Math.cos(b) + 2 * (500- (xx+zx+wx*(i+1) + h * Math.cos(b))), xy+zy+wy*(i+1) + h * Math.sin(b));
                result.add(temp);
                System.out.println(temp);
            }

            polygon = new Polygon2D();
            trap = new Polygon2D();
            trap.append(xx+zx+wx*(11) + 2 * (500 - (xx+zx+wx*(11))), xy+zy+wy*(11));
            trap.append(xx+zx+wx*(11) + h * Math.cos(b) + 2 * (500- (xx+zx+wx*(11) + h * Math.cos(b))), xy+zy+wy*(11) + h * Math.sin(b));
            trap.append(xx+zx+wx*(11) + zx + 2 * ( 500 - (xx+zx+wx*(11) + zx)), xy+zy+wy*(11) + zy);
            trap.append(xx+zx+wx*(11) * y * Math.cos(b) + 2 * (500 - (xx+zx+wx*(11) * y * Math.cos(b))), xy+zy+wy*(11) + y * Math.sin(b));

            polygon.append(xx+zx+wx*(11)+zx + 2 * (500 - (xx+zx+wx*(11)+zx)), xy+zy+wy*(11)+zy);
            polygon.append(xx+zx+wx*(11)+zx + y * Math.cos(b) + 2 * (500 - (xx+zx+wx*(11)+zx + y * Math.cos(b))), xy+zy+wy*(11)+zy + y * Math.sin(b));
            polygon.append(xx+zx+wx*(11)+zx+xx + 2 * (500 - (xx+zx+wx*(11)+zx+xx)), xy+zy+wy*(11)+zy+xy);

            result.add(polygon);
            result.add(trap);
            System.out.println(polygon);
            System.out.println(trap);
        }
        System.out.println("DONE!");
        return result;
    }
}
