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
    private int numVoters, numParties, numDistricts;
	private static int avgVotersPerDistrict;
	private double th = 0.01;
	private static double gmth = 0.02;

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
     * Splits the map into 5 (if repPerDistrict=3) or 12 (if repPerDistrict=1) concentric triangles;
     * returns the position of their top vertices.
     * The most inner triangle contains 1 district, and each outer triangle contains 20/22 districts.
     */
    public List<Point2D> getConcentricTriangleTips(List <Voter> voters, int repPerDistrict)
    {
        numVoters = voters.size();

        int numTriangles;
        if (repPerDistrict == 1)
            numTriangles = 12;
        else if (repPerDistrict == 3)
            numTriangles = 5;
        else
            return null;

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
            // System.out.println(topY);

            innerVoterCount = Run.countInclusion(voters, innerTriangle);
        } while (needsMoreVoters(innerVoterCount));
        System.out.println("found inner triangle");
        System.out.println(innerVoterCount);

        triangles.add(innerTriangle);
        triTips.add(new Point2D.Double(topX, topY));

        // Draw numTriangles-1 more triangles around it, each with 20/22 districts
        Polygon2D triangle;
        int voterCount;

        for (int i = 0; i < numTriangles - 2; i++) {
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
            } while (needsMoreVoters(voterCount - innerVoterCount, repPerDistrict == 3 ? 20:22));
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

    //Restore triangles from the topmost endpoint
    private Polygon2D getTriByVertex(Point2D vertex) {
    		Polygon2D tri = new Polygon2D();
    		double y = vertex.getY();
    		tri.append(vertex);
    		tri.append(250+y*Math.sqrt(3)/2, 250*Math.sqrt(3)-y/2);
    		tri.append(750-y*Math.sqrt(3)/2, 250*Math.sqrt(3)-y/2);
    		return tri;
    }

    //Find a point with specific x-coordinate on a line p1p2
    private double findYByX(double x, Point2D p1, Point2D p2) {
    		double x1 = p1.getX();
    		double y1 = p1.getY();
    		double x2 = p2.getX();
    		double y2 = p2.getY();
    		double k = (y2-y1)/(x2-x1);
    		double b = y1-k*x1;
    		return k*x+b;
    }

    //Fix one edge and find the opposite edge to ensure population within 0.9-1.1 average
    private Polygon2D findDist(Point2D currIn, Point2D currOut, Point2D innerEndpoint1, Point2D innerEndpoint2,
    		Point2D outerEndpoint1, Point2D outerEndPoint2, double step, double lenRate, List<Voter> voters, boolean reverse, int repPerDistrict, int party, boolean gmable, double popRate) {
    		double efgp;
    		double max_efgp = 0;
    		Polygon2D result = null;
    		double dir_step = step;
    		if(reverse)
    			dir_step = -dir_step;
    		for(double innerX = currIn.getX(); ; innerX += dir_step) {
    			if(reverse && innerX < innerEndpoint2.getX())
    				break;
    			if(!reverse && innerX > innerEndpoint2.getX())
    				break;
			double innerY = findYByX(innerX, innerEndpoint1, innerEndpoint2);
			double outerX = lenRate * (innerX - currIn.getX()) + currOut.getX();
			double outerY = findYByX(outerX, outerEndpoint1, outerEndPoint2);
			Polygon2D polygon = new Polygon2D();
			polygon.append(currIn);
			polygon.append(currOut);
			polygon.append(outerX, outerY);
			polygon.append(innerX, innerY);
			int p = Run.countInclusion(voters, polygon);
			if(gmable) {
				if(p >= 0.9*(double)(avgVotersPerDistrict) && p <= 1.1*(double)(avgVotersPerDistrict)) {
					efgp = efficiencyGap(voters, polygon, repPerDistrict, party);
					if(efgp > max_efgp) {
						max_efgp = efgp;
						result = polygon;
					}
				}
				else if(p >= 1.1*(double)(avgVotersPerDistrict))
					break;
			}
			else {
				if(p >= (1.0 - th)*popRate && p <= (1.0 + th)*popRate) {
					return polygon;
				}
			}
		}
    		return result;
    	}

    // Find a district at the corner of the ring
    private Polygon2D findDistAtCorner(Point2D currIn, Point2D currOut, Point2D innerEndpoint1, Point2D innerEndpoint2,
    		Point2D outerEndpoint1, Point2D outerEndPoint2, double step, double lenRate, List<Voter> voters, boolean reverse, int repPerDistrict, int party, boolean gmable, double popRate) {
    		double efgp;
		double max_efgp = 0;
		Polygon2D result = null;
    		double dir_step = step;
		if(reverse)
			dir_step = -dir_step;
		for(double innerX = innerEndpoint1.getX(); ; innerX += dir_step) {
			if(reverse && innerX < innerEndpoint2.getX())
				break;
			if(!reverse && innerX > innerEndpoint2.getX())
				break;
			double innerY = findYByX(innerX, innerEndpoint1, innerEndpoint2);
			double outerX = lenRate * (innerX - innerEndpoint1.getX()) + outerEndpoint1.getX();
			double outerY = findYByX(outerX, outerEndpoint1, outerEndPoint2);
			Polygon2D polygon = new Polygon2D();
			polygon.append(innerEndpoint1);
			polygon.append(currIn);
			polygon.append(currOut);
			polygon.append(outerEndpoint1);
			polygon.append(outerX, outerY);
			polygon.append(innerX, innerY);
			int p = Run.countInclusion(voters, polygon);
			if(gmable) {
				if(p >= 0.9 * avgVotersPerDistrict && p <= 1.1 * avgVotersPerDistrict) {
					efgp = efficiencyGap(voters, polygon, repPerDistrict, party);
					if(efgp > max_efgp) {
						max_efgp = efgp;
						result = polygon;
					}
				}
				else if(p >= 1.1*(double)(avgVotersPerDistrict))
					break;
			}
			else {
				if(p >= (1.0 - th)*popRate && p <= (1.0 + th)*popRate) {
					return polygon;
				}
			}
		}
		return result;
	}

    // When the region do not have no enough population, use three edges
    private Polygon2D findDistAtTwoCorners(Point2D currIn, Point2D currOut, Point2D innerEndpoint1, Point2D innerEndpoint2, Point2D innerEndpoint3,
    		Point2D outerEndpoint1, Point2D outerEndpoint2, Point2D outerEndPoint3, double step, double lenRate, List<Voter> voters, int repPerDistrict, int party, boolean gmable, double popRate) {
		double efgp;
		double max_efgp = 0;
		Polygon2D result = null;
		for(double innerX = innerEndpoint2.getX(); innerX <= innerEndpoint3.getX(); innerX += step) {
			double innerY = findYByX(innerX, innerEndpoint2, innerEndpoint3);
			double outerX = lenRate * (innerX - innerEndpoint2.getX()) + outerEndpoint2.getX();
			double outerY = findYByX(outerX, outerEndpoint2, outerEndPoint3);
			Polygon2D polygon = new Polygon2D();
			polygon.append(innerEndpoint2);
			polygon.append(innerEndpoint1);
			polygon.append(currIn);
			polygon.append(currOut);
			polygon.append(outerEndpoint1);
			polygon.append(outerEndpoint2);
			polygon.append(outerX, outerY);
			polygon.append(innerX, innerY);
			int p = Run.countInclusion(voters, polygon);
			if(gmable) {
				if(p >= 0.9*avgVotersPerDistrict && p <= 1.1*avgVotersPerDistrict) {
					efgp = efficiencyGap(voters, polygon, repPerDistrict, party);
					if(efgp > max_efgp) {
						max_efgp = efgp;
						result = polygon;
					}
				}
				else if(p >= 1.1*(double)(avgVotersPerDistrict))
					break;
			}
			else {
				if(p >= (1.0 - th)*popRate && p <= (1.0 + th)*popRate) {
					return polygon;
				}
			}
		}
		return result;
	}
    
    private static boolean isGerryManderable(int undistricted_population, int undistricted_num) {
    		if((double)undistricted_population / undistricted_num < (1.0-gmth) * avgVotersPerDistrict || (double)undistricted_population / undistricted_num > (1.0+gmth) * avgVotersPerDistrict)
    			return false;
    		return true;
    }

    // Districting in a ring
    private List<Polygon2D> getDistrictsInRing(List<Voter> voters, Point2D innerVertex, Point2D outerVertex, int repPerDistrict, int party) {
    		List<Polygon2D> results = new ArrayList<Polygon2D>();
    		double step = 0.1;
    		int dist_num = repPerDistrict == 3 ? 20 : 22;
    		Polygon2D outerTri = getTriByVertex(outerVertex);
    		Polygon2D innerTri = getTriByVertex(innerVertex);
    		Point2D currIn = innerVertex;
    		Point2D currOut = outerVertex;
    		double lenRate = (outerVertex.getY()/2 - 250 * Math.sqrt(3) / 3) / (innerVertex.getY()/2 - 250 * Math.sqrt(3) / 3);
    		int total_population = Run.countInclusion(voters, outerTri) - Run.countInclusion(voters, innerTri);
    		boolean gmable = true;
    		double popRate = 0;
    		for(int i = 0; i < 3; i++) {
    			System.out.println("Edge: " + Integer.toString(i));
	    		while(results.size() < dist_num-1) {
	    			System.out.println(gmable);
	    			Polygon2D p;
	    			if(i == 1)
	    				p = findDist(currIn, currOut, innerTri.getPoints().get(i), innerTri.getPoints().get((i+1)%3),
	    					outerTri.getPoints().get(i), outerTri.getPoints().get((i+1)%3), step, lenRate, voters, true, repPerDistrict, party, gmable, popRate);
	    			else
	    				p = findDist(currIn, currOut, innerTri.getPoints().get(i), innerTri.getPoints().get((i+1)%3),
		    					outerTri.getPoints().get(i), outerTri.getPoints().get((i+1)%3), step, lenRate, voters, false, repPerDistrict, party, gmable, popRate);
	    			if(p == null) {
	    				if(i == 0) {
	    					System.out.println("1111");
	    					p = findDistAtCorner(currIn, currOut, innerTri.getPoints().get(1), innerTri.getPoints().get(2),
	    						outerTri.getPoints().get(1), outerTri.getPoints().get(2), step, lenRate, voters, true, repPerDistrict, party, gmable, popRate);
	    					if(p == null) {
	    						System.out.println("3333");
	    						p = findDistAtTwoCorners(currIn, currOut, innerTri.getPoints().get(1), innerTri.getPoints().get(2), innerTri.getPoints().get(0),
	    						outerTri.getPoints().get(1), outerTri.getPoints().get(2), outerTri.getPoints().get(0), step, lenRate, voters, repPerDistrict, party, gmable, popRate);
	    						 i++;
	    					}
	    					total_population -= Run.countInclusion(voters, p);
	    	    				results.add(p);
	    	    				if(gmable) {
	    	    					gmable = isGerryManderable(total_population, dist_num - results.size());
	    	    					if(!gmable)
	    	    						popRate = (double)total_population/(dist_num - results.size());
	    	    				}
	    					currIn = p.getPoints().get(p.getPoints().size()-1);
		    	    			currOut = p.getPoints().get(p.getPoints().size()-2);
		    	    			System.out.println("Polygon num: " + Integer.toString(results.size()));
	    					break;
	    				}
	    				else if(i == 1) {
	    					System.out.println("2222");
	    					p = findDistAtCorner(currIn, currOut, innerTri.getPoints().get(2), innerTri.getPoints().get(0),
		    						outerTri.getPoints().get(2), outerTri.getPoints().get(0), step, lenRate, voters, false, repPerDistrict, party, gmable, popRate);
	    					System.out.println(Run.countInclusion(voters, p));
	    					total_population -= Run.countInclusion(voters, p);
		    				results.add(p);
		    				if(gmable) {
    	    						gmable = isGerryManderable(total_population, dist_num - results.size());
    	    						if(!gmable)
	    	    						popRate = (double)total_population/(dist_num - results.size());
		    				}
				    		currIn = p.getPoints().get(5);
				    		currOut = p.getPoints().get(4);
				    		System.out.println("Polygon num: " + Integer.toString(results.size()));
				    		break;
	    				}
	    			}
    				currIn = p.getPoints().get(p.getPoints().size()-1);
	    			currOut = p.getPoints().get(p.getPoints().size()-2);
	    			System.out.println(Run.countInclusion(voters, p));
	    			total_population -= Run.countInclusion(voters, p);
	    			results.add(p);
	    			if(gmable) {
						gmable = isGerryManderable(total_population, dist_num - results.size());
						if(!gmable)
    						popRate = (double)total_population/(dist_num - results.size());
				}
	    			System.out.println("Polygon num: " + Integer.toString(results.size()));
	    		}
    		}
    		// We do not check population in the last polygon of a ring now. It should be a limitation for gerrymandering
    		Polygon2D p = new Polygon2D();
    		p.append(currIn);
    		p.append(currOut);
    		if(currIn.getX() > 500 && currIn.getY() > innerTri.getPoints().get(1).getY()) {
    			p.append(outerTri.getPoints().get(1));
    			p.append(outerTri.getPoints().get(2));
    			p.append(outerTri.getPoints().get(0));
    			p.append(innerTri.getPoints().get(0));
    			p.append(innerTri.getPoints().get(2));
    			p.append(innerTri.getPoints().get(1));
    		}
    		else if(currIn.getY() == innerTri.getPoints().get(1).getY()) {
    			p.append(outerTri.getPoints().get(2));
    			p.append(outerTri.getPoints().get(0));
        		p.append(innerTri.getPoints().get(0));
        		p.append(innerTri.getPoints().get(2));
    		}
    		else {
    			p.append(outerTri.getPoints().get(0));
        		p.append(innerTri.getPoints().get(0));
    		}
    		results.add(p);
    		System.out.println("Polygon num: " + Integer.toString(results.size()));
    		return results;
    }

    // Districting in all rings
    public List<Polygon2D> getAllDistrict(List<Voter> voters, List<Point2D> vertices, int repPerDistrict, int party) {
    		List<Polygon2D> results = new ArrayList<Polygon2D>();
    		results.add(getTriByVertex(vertices.get(0)));
    		for(int i = 0; i < vertices.size() - 1; i++) {
    			List<Polygon2D> result = getDistrictsInRing(voters, vertices.get(i), vertices.get(i+1), repPerDistrict, party);
    			results.addAll(result);
    			System.out.println("Total polygon num: " + Integer.toString(results.size()));
    		}
    		return results;
    }

	private Double compactness(Polygon2D district) {
			double area = district.area();
			double perimeter = 0;
			List<Point2D> points = district.getPoints();
			for (int i = 0; i < points.size(); ++i) {
					perimeter += district.ptDist(points.get(i), points.get((i+1)%points.size()));
			}
			// System.out.println(perimeter/area);
			return perimeter/area;
	}

	private Double compactGerry(List<Voter> voters, int repPerDistrict, int party) {
			List<Point2D> vertices = getConcentricTriangleTips(voters, repPerDistrict);
			List<Polygon2D> districts = getAllDistrict(voters, vertices, repPerDistrict, party);
			List<Double> comp = new ArrayList<Double>();
			double total = 0;
			for (int i=0; i < districts.size(); ++i) {
					comp.set(i, compactness(districts.get(i)));
					total += comp.get(i);
			}
			double mean = total/comp.size();
			double deviation = 0;
			for (Double c : comp) {
					deviation += Math.pow(c-mean, 2);
			}
			return Math.sqrt(deviation);
	}
	
	private Double efficiencyGap(List<Voter> voters, Polygon2D district, int repPerDistrict, int party) {
		List<Double> result = new ArrayList<Double>();
		for (Voter voter : voters) {
				if (district.contains(voter.getLocation())) {
						List<Double> preference = voter.getPreference();
						for (int i = 0; i < preference.size(); ++i) {
								if (result.size() < preference.size()) {
										result.add(preference.get(i));
								} else {
										result.set(i, result.get(i)+preference.get(i));
								}
						}
				}
		}
		double max = -1.0;
//		int maxIdx = -1;
		for (int i = 0; i < result.size(); ++i) {
				while (result.get(i) >= 1.0/repPerDistrict) {
						result.set(i, result.get(i)-1.0/repPerDistrict);
				}
				if (max < result.get(i)) {
						max = result.get(i);
//						maxIdx = i;
				}
		}
		result.set(party, 0.0);
		double sum = 0.0;
		for (int i = 0; i < result.size(); ++i) {
				sum += result.get(i);
		}
		return sum;
}

	@Override
	public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
			numVoters = voters.size();
      numParties = voters.get(0).getPreference().size();
      List<Polygon2D> result = new ArrayList<Polygon2D>();
      numDistricts = 243 / repPerDistrict;
      random = new Random(seed);
      avgVotersPerDistrict = 333333 / numDistricts;
      int gerryManderParty = 0;
			// System.out.println(compactGerry(voters));

      // 81 Districts
		List<Point2D> vertices = getConcentricTriangleTips(voters, repPerDistrict);
		System.out.println("Start districting");
		List<Polygon2D> districts = getAllDistrict(voters, vertices, repPerDistrict, gerryManderParty);
		 for (Polygon2D district : districts) {
		 		System.out.println(efficiencyGap(voters, district, repPerDistrict, 0));
		 }
		return districts;
	}

}
