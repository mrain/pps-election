package election.g5;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import election.sim.DistrictGenerator;
import election.sim.Polygon2D;
import election.sim.Run;
import election.sim.Voter;

public class RingDistrictGenerator implements DistrictGenerator {
	private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts, avgVotersPerDistrict;

    public boolean needsMoreVoters(double numVotersInDistrict)
    {
        return needsMoreVoters(numVotersInDistrict, 1);
    }

    public boolean needsMoreVoters(double numVotersInDistrict, int districts)
    {
        // We allow 1% deviation from avg
        double threshold = 0.01 * avgVotersPerDistrict;

        if (avgVotersPerDistrict*districts - numVotersInDistrict > threshold)
            return true;
        return false;
    }

    /*
     * Splits the map into 5 concentric triangles, and returns the position of their top vertices.
     * The most inner triangle contains 1 district, and each outer triangle contains 20 districts.
     */
    public List<Point2D> getConcentricTriangleTips(List <Voter> voters)
    {
        numVoters = voters.size();
        List<Point2D> triTips = new ArrayList<Point2D>();
        List<Polygon2D> triangles = new ArrayList<Polygon2D>();
        final double centerX = 500;
        final double centerY = 500*Math.tan(Math.PI/6);
        final double cos30 = Math.cos(Math.PI/6);
        final double sin30 = Math.sin(Math.PI/6);

        // Distance between vertices of triangle to center
        // TODO: Use a binary search algorithm instead of step; too slow
        double distFromCenter = 0;
        double step = 0.5;

        // Find most inner triangle
        Polygon2D innerTriangle;
        int innerVoterCount;
        double topX, topY, leftX, rightX, botY;

        do {
            distFromCenter += step;

            topX = centerX;
            topY = centerY + distFromCenter;

            leftX = centerX - distFromCenter*cos30;
            rightX = centerX + distFromCenter*cos30;

            botY = centerY - distFromCenter*sin30;

            innerTriangle = new Polygon2D();
            innerTriangle.append(topX, topY);
            innerTriangle.append(leftX, botY);
            innerTriangle.append(rightX, botY);
            System.out.println(topY);

            innerVoterCount = Run.countInclusion(voters, innerTriangle);
        } while (needsMoreVoters(innerVoterCount));
        System.out.println("found inner triangle");
        System.out.println(innerVoterCount);

        triangles.add(innerTriangle);
        triTips.add(new Point2D.Double(topX, topY));

        // Draw 3 more triangles around it, each with 20 districts
        Polygon2D triangle;
        int voterCount;

        for (int i = 0; i < 3; i++) {
            do {
                distFromCenter += step;

                topX = centerX;
                topY = centerY + distFromCenter;

                leftX = centerX - distFromCenter*cos30;
                rightX = centerX + distFromCenter*cos30;

                botY = centerY - distFromCenter*sin30;

                triangle = new Polygon2D();
                triangle.append(topX, topY);
                triangle.append(leftX, botY);
                triangle.append(rightX, botY);

                voterCount = Run.countInclusion(voters, triangle);
                System.out.println(topY);
            } while (needsMoreVoters(voterCount - innerVoterCount, 20));
            System.out.println("found an outer triangle");
            System.out.println(voterCount - innerVoterCount);

            innerVoterCount = voterCount;
            triangles.add(triangle);
            triTips.add(new Point2D.Double(topX, topY));
        }

        // Last outer triangle; not necessary but keeping the code for completeness
        Polygon2D outerTriangle = new Polygon2D();
        outerTriangle.append(topX, 500*Math.sqrt(3));
        outerTriangle.append(0, 0);
        outerTriangle.append(1000, 0);
        voterCount = Run.countInclusion(voters, outerTriangle);

        System.out.println("found an outer triangle");
        System.out.println(voterCount - innerVoterCount);

        triangles.add(outerTriangle);
        triTips.add(new Point2D.Double(topX, 500*Math.sqrt(3)));

        return triTips;
    }

    private Polygon2D getTriByVertex(Point2D vertex) {
    		Polygon2D tri = new Polygon2D();
    		double y = vertex.getY();
    		tri.append(vertex);
    		tri.append(250+y*Math.sqrt(3)/2, 250*Math.sqrt(3)-y/2);
    		tri.append(750-y*Math.sqrt(3)/2, 250*Math.sqrt(3)-y/2);
    		return tri;
    }

    private double findYByX(double x, Point2D p1, Point2D p2) {
    		double x1 = p1.getX();
    		double y1 = p1.getY();
    		double x2 = p2.getX();
    		double y2 = p2.getY();
    		double k = (y2-y1)/(x2-x1);
    		double b = y1-k*x1;
    		return k*x+b;
    }

    private Polygon2D findDist(Point2D currIn, Point2D currOut, Point2D innerEndpoint1, Point2D innerEndpoint2, Point2D outerEndpoint1, Point2D outerEndPoint2, double step, double lenRate, List<Voter> voters) {
    		for(double innerX = currIn.getX(); innerX <= innerEndpoint2.getX(); innerX += step) {
			double innerY = findYByX(innerX, innerEndpoint1, innerEndpoint2);
			double outerX = lenRate * (innerX - currIn.getX()) + currOut.getX();
			double outerY = findYByX(outerX, outerEndpoint1, outerEndPoint2);
			Polygon2D polygon = new Polygon2D();
			polygon.append(currIn);
			polygon.append(currOut);
			polygon.append(outerX, outerY);
			polygon.append(innerX, innerY);
			int p = Run.countInclusion(voters, polygon);
			if(p >= 0.9*(double)(avgVotersPerDistrict) && p <= 1.1*(double)(avgVotersPerDistrict)) {
				return polygon;
			}
		}
    		return null;
    }

    private Polygon2D findDist(Point2D vertexIn, Point2D vertexOut, Point2D currIn, Point2D currOut, Point2D innerEndpoint1, Point2D innerEndpoint2, Point2D outerEndpoint1, Point2D outerEndPoint2, double step, double lenRate, List<Voter> voters) {
		for(double innerX = currIn.getX(); innerX <= innerEndpoint2.getX(); innerX += step) {
			double innerY = findYByX(innerX, innerEndpoint1, innerEndpoint2);
			double outerX = lenRate * (innerX - currIn.getX()) + currOut.getX();
			double outerY = findYByX(outerX, outerEndpoint1, outerEndPoint2);
			Polygon2D polygon = new Polygon2D();
			polygon.append(vertexIn);
			polygon.append(currIn);
			polygon.append(currOut);
			polygon.append(vertexOut);
			polygon.append(outerX, outerY);
			polygon.append(innerX, innerY);
			int p = Run.countInclusion(voters, polygon);
			if(p >= 0.9*avgVotersPerDistrict && p <= 1.1*avgVotersPerDistrict) {
				return polygon;
			}
		}
		return null;
	}

    private List<Polygon2D> getDistrictsInRing(List<Voter> voters, List<Point2D> vertices) {
    		List<Polygon2D> results = new ArrayList<Polygon2D>();
    		double step = 0.5;
    		double dist_num = 20;
    		for(int v = 0; v < vertices.size()-1; v++) {
	    		Polygon2D outerTri = getTriByVertex(vertices.get(v));
	    		Polygon2D innerTri = getTriByVertex(vertices.get(v+1));
	    		Point2D currIn = vertices.get(v);
	    		Point2D currOut = vertices.get(v+1);
	    		double lenRate = (vertices.get(v+1).getY()/2 - 250 * Math.sqrt(3) / 3) / (vertices.get(v).getY()/2 - 250 * Math.sqrt(3) / 3);
	    		for(int i = 0; i < 3; i++) {
		    		while(results.size() < dist_num-1) {
		    			Polygon2D p;
		    			p = findDist(currIn, currOut, innerTri.getPoints().get(i), innerTri.getPoints().get((i+1)%3),
		    					outerTri.getPoints().get(i), outerTri.getPoints().get((i+1)%3), step, lenRate, voters);
		    			if(p == null) {
		    				break;
		    				//TODO: fix the corner
//		    				i++;
//		    				p = findDist(innerTri.getPoints().get(i), outerTri.getPoints().get(i), currIn, currOut,
//				    				innerTri.getPoints().get(i), innerTri.getPoints().get((i+1)%3), outerTri.getPoints().get(i), outerTri.getPoints().get((i+1)%3), step, lenRate, voters);
//				    		currIn = p.getPoints().get(5);
//				    		currOut = p.getPoints().get(4);
		    			}
		    			else {
		    				currIn = p.getPoints().get(3);
			    			currOut = p.getPoints().get(2);
		    			}
		    			results.add(p);
		    		}
	    		}
	    		Polygon2D p = new Polygon2D();
	    		p.append(currIn);
	    		p.append(currOut);
	    		p.append(outerTri.getPoints().get(0));
	    		p.append(innerTri.getPoints().get(0));
	    		results.add(p);
    		}
    		return results;
    }

	@Override
	public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
		numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        numDistricts = 243 / repPerDistrict;
        random = new Random(seed);
        avgVotersPerDistrict = 333333 / numDistricts;

        // 81 Districts
        if (repPerDistrict == 3) {
        		List<Point2D> vertices = new ArrayList<Point2D>();
        		for(int i = 0; i < 5; i++) {
        			vertices.add(new Point2D.Double(500, 500*Math.sqrt(3)/3 + 100*Math.sqrt(3)/15 * i));
        		}
			return getDistrictsInRing(voters, vertices);
        }
        return null;
	}

}
