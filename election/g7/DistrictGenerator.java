package election.g7;

import java.util.*;
import election.sim.*;
import java.awt.geom.*;

public class DistrictGenerator implements election.sim.DistrictGenerator {
    private double scale = 1000.0;
    private Random random;
    private int numVoters, numParties, numDistricts;
    private double eps = 1E-7;
    private Map<Polygon2D, List<Voter>> polyganList = new HashMap<>();
    private Map<Integer, Polygon2D> polygonMap = new HashMap<>();
    private Map<Integer, List<Voter>> voterMap = new HashMap<>();
    private Map<Integer, Boolean> checkMap = new HashMap<>();
    private int partyToWin = 1;

    public List<Voter> sortByXCoordinate(List<Voter>voters){
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return Double.compare(v1.getLocation().getX(), v2.getLocation().getX());
            }
        });
        return voters;
    }

    @Override
    public List<Polygon2D> getDistricts(List<Voter> voters, int repPerDistrict, long seed) {
        numVoters = voters.size();
        numParties = voters.get(0).getPreference().size();
        List<Polygon2D> result = new ArrayList<Polygon2D>();
        //numDistricts = 243 / repPerDistrict;
        numDistricts = 243;
        double height = scale / 2.0 * Math.sqrt(3);
        int numStripes = 81;
        //Can contribute deviation
        int peopleInBlock = numVoters / numDistricts;

        int blockEachStripe =  numDistricts / numStripes; //9;
        Collections.sort(voters, new Comparator<Voter>() {
            @Override
            public int compare(Voter v1, Voter v2) {
                return Double.compare(v2.getLocation().getY(), v1.getLocation().getY());
            }
        });
        // From top to bottom
        List<List<Voter>> votersInStripe = new ArrayList<>();

        int from = 0;
        double btm = 500*Math.sqrt(3);
        for (int i = 0; i < numStripes; i++) {
            int to = blockEachStripe*peopleInBlock*(i + 1) - 1;
            if (i == numStripes - 1) {
                blockEachStripe = numDistricts - blockEachStripe * (numStripes - 1);
                to = numVoters - 1;
            }
            while (to + 1 < numVoters && voters.get(to) == voters.get(to + 1))
                to++;

            List<Voter> voter_by_y = voters.subList(from, to + 1);
            from = to + 1;
            double top = btm;
            btm = (i == numStripes - 1) ? 0 : voter_by_y.get(voter_by_y.size() - 1).getLocation().getY() - eps;
            List<Voter> voter_by_x=sortByXCoordinate(voter_by_y);
            List<Double> x_coordinates=new ArrayList<>();
            //draw vertical lines in each stripe area
            for (int j=1;j<blockEachStripe;j++){
                Double curr_x=voter_by_x.get((int)(voter_by_x.size()/blockEachStripe*j)).getLocation().getX();
                x_coordinates.add(curr_x+eps);//8 doubles -->9
            }
            //x_coordinates contains x_val for 8 vertical lines

            double preX = btm / Math.sqrt(3);
            double btmWidth = 1000 - 2*preX;
            if (i == 0) {
                double left_btm=preX;
                double right_btm=left_btm;
                double height_temp=btm;
                for(int vl=0;vl<x_coordinates.size();vl++){
                    Polygon2D polygon=new Polygon2D();
                    right_btm=x_coordinates.get(vl);
                    if (right_btm<preX+(btmWidth/2) || left_btm>preX+(btmWidth/2) ){
                        if (vl==0){
                            polygon.append(left_btm,btm);
                            polygon.append(right_btm,btm);
                            double diff=right_btm-left_btm;
                            height_temp+=diff*Math.sqrt(3);
                            polygon.append(right_btm,height_temp);
                            result.add(polygon);
                        }
                        else{
                            double diff=right_btm-left_btm;
                            if (right_btm<preX+(btmWidth/2)){
                                polygon.append(left_btm,height_temp);
                                polygon.append(left_btm,btm);
                                polygon.append(right_btm,btm);
                                height_temp+=diff*Math.sqrt(3);
                                polygon.append(right_btm,height_temp);
                            }
                            else{
                                polygon.append(left_btm,height_temp);
                                polygon.append(left_btm,btm);
                                polygon.append(right_btm,btm);
                                height_temp-=diff*Math.sqrt(3);
                                polygon.append(right_btm,height_temp);
                            }
                            result.add(polygon);
                        }
                    }
                    else{
                        polygon.append(500., 500*Math.sqrt(3));
                        polygon.append(left_btm,height_temp);
                        polygon.append(left_btm,btm);
                        polygon.append(right_btm,btm);
                        height_temp=btm+(preX+btmWidth-right_btm)*Math.sqrt(3);
                        polygon.append(right_btm,height_temp);
                        result.add(polygon);
                    }
                    left_btm=right_btm;//update left_btm
//                    polyganList.put(polygon, voter_by_x.subList((int)(voter_by_x.size()/blockEachStripe*vl), (int)(voter_by_x.size()/blockEachStripe*(vl+1)));
                }
                Polygon2D polygon=new Polygon2D();
                polygon.append(left_btm,btm);
                polygon.append(preX+btmWidth,btm);
                polygon.append(left_btm,btm+(preX+btmWidth-left_btm)*Math.sqrt(3));
                result.add(polygon);
//                polyganList.put(polygon, voter_by_x.subList((int)(voter_by_x.size()/blockEachStripe*vl), (int)(voter_by_x.size()));
            }

            else {
                double preX1 = top / Math.sqrt(3);
                double topWidth = 1000 - 2*preX1;
                double left_btm=btm/(Math.sqrt(3));
                double right_btm=left_btm;
                double height_temp=btm;
                for(int vl=0;vl<x_coordinates.size();vl++){
                    Polygon2D polygon=new Polygon2D();
                    right_btm=x_coordinates.get(vl);
                    if (left_btm>preX1 && right_btm<(topWidth+preX1)){
                        polygon.append(left_btm,btm);
                        polygon.append(right_btm,btm);
                        polygon.append(right_btm,top);
                        polygon.append(left_btm,top);
                        result.add(polygon);
                    }else if (vl==0 && right_btm<=preX1){
                        polygon.append(preX,btm);
                        polygon.append(right_btm,btm);
                        height_temp+=(right_btm-left_btm)*Math.sqrt(3);
                        polygon.append(right_btm,btm+(right_btm-left_btm)*Math.sqrt(3));
                        result.add(polygon);
                    }else if (right_btm<=preX1){
                        polygon.append(left_btm,height_temp);
                        polygon.append(left_btm,btm);
                        polygon.append(right_btm,btm);
                        height_temp+=(right_btm-left_btm)*Math.sqrt(3);
                        polygon.append(right_btm,height_temp);
                        result.add(polygon);
                    }
                    else if (vl==0 && right_btm>preX1){
                        polygon.append(left_btm,btm);
                        polygon.append(right_btm,btm);
                        polygon.append(right_btm,top);
                        polygon.append(preX1,top);
                        result.add(polygon);
                    }
                    else if(left_btm>preX1+topWidth){
                        polygon.append(left_btm,height_temp);
                        polygon.append(left_btm,btm);
                        polygon.append(right_btm,btm);
                        height_temp-=(right_btm-left_btm)*Math.sqrt(3);
                        polygon.append(right_btm,height_temp);
                        result.add(polygon);
                    }
                    else{
                        if(left_btm<preX1 && right_btm>preX1){
                            polygon.append(left_btm,btm);
                            polygon.append(right_btm,btm);
                            polygon.append(right_btm,top);
                            polygon.append(preX1,top);
                            polygon.append(left_btm,height_temp);
                            result.add(polygon);
                        }
                        else{
                            polygon.append(preX1+topWidth,top);
                            polygon.append(left_btm,top);
                            polygon.append(left_btm,btm);
                            polygon.append(right_btm,btm);
                            height_temp=top-(right_btm-(preX1+topWidth))*Math.sqrt(3);//update temp height
                            polygon.append(right_btm,height_temp);
                            result.add(polygon);
                        }
                    }
                    left_btm=right_btm;
                }

                Polygon2D polygon=new Polygon2D();
                if(left_btm<(preX1+topWidth)){
                    polygon.append(left_btm,top);
                    polygon.append(left_btm,btm);
                    polygon.append(preX+btmWidth,btm);
                    polygon.append(preX1+topWidth,top);
                    result.add(polygon);
                }
                else{
                    polygon.append(preX+btmWidth,btm);
                    polygon.append(left_btm,btm);
                    polygon.append(left_btm,btm+(preX-left_btm)*Math.sqrt(3));
                    result.add(polygon);
                }

            }
        }
        int index = 0;
//TODO: TEST
//        for (Map.Entry<Polygon2D, List<Voter>> entry: polyganList.entrySet()){
//            polygonMap.put(index, entry.getKey());
//            voterMap.put(index++, entry.getValue());
//            checkMap.put(index, false);
//        }
//
//
//        for (Map.Entry<Integer, Polygon2D> entry : polygonMap.entrySet()) {
//            int id = entry.getKey();
//            if (!checkMap.get(id) && !isSwingState(id)) {
//                Map<Integer, double[]> adjacentDistricts = getAdjacentDistricts(id);
//                Polygon2D swing = entry.getValue();
//                for (Map.Entry<Integer, double[]> adjacentDistrict : adjacentDistricts.entrySet()) {
//                    int otherId = adjacentDistrict.getKey();
//                    double[] edge = adjacentDistrict.getValue();
//                    double x1 = edge[0], y1 = edge[1], x2 = edge[2], y2 = edge[3];
//                    double len = Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
//                    double width = getWidth(len);
//                    List<Point2D> swingPoints = swing.getPoints();
//                    List<Point2D> adjacentPoints = polygonMap.get(otherId).getPoints();
//                    //vertical line
//                    if (isEqual(x1, x2)) {
//                        double start = Math.max(y1, y2);
//                        double end = Math.min(y1, y2);
//                        for (double i = start; i - end >= width; i -= width) {
//                            Polygon2D concaveSwing = new Polygon2D();
//                            Polygon2D convexAdjacent = new Polygon2D();
//                            Polygon2D concaveAdjacent = new Polygon2D();
//                            Polygon2D convexSwing = new Polygon2D();
//                            buildPolygonByY(swingPoints, concaveSwing, convexSwing, x1, i, width);
//                            buildPolygonByY(adjacentPoints, concaveAdjacent, convexAdjacent, x1, i, width);
//                            if (setNewPolygon(id, otherId, convexSwing, concaveAdjacent, concaveSwing, convexAdjacent))
//                                break;
//                        }
//                    }
//                    //horizontal line
//                    else if (isEqual(y1, y2)) {
//                        double start = Math.min(x1, x2);
//                        double end = Math.max(x1, x2);
//                        for (double i = start; end - i >= width; i += width) {
//                            Polygon2D concaveSwing = new Polygon2D();
//                            Polygon2D convexAdjacent = new Polygon2D();
//                            Polygon2D concaveAdjacent = new Polygon2D();
//                            Polygon2D convexSwing = new Polygon2D();
//                            buildPolygonByX(swingPoints, concaveSwing, convexSwing, y1, i, width);
//                            buildPolygonByX(adjacentPoints, concaveAdjacent, convexAdjacent, y1, i, width);
//                            if (setNewPolygon(id, otherId, convexSwing, concaveAdjacent, concaveSwing, convexAdjacent))
//                                break;
//                        }
//                    }
//                    else
//                        System.out.println("Adjacent edge is not vertical or horizontal!");
//                }
//            }
//        }
//
//        result = new ArrayList<Polygon2D>(polygonMap.values());
        //System.out.println(getDistrictsByBorder(result));
        return result;
    }

    private double getWidth(double len) {
        return len / 10;
    }


    public Line2D constructLineKey(Point2D p1, Point2D p2) {
        //border line is always from left to right, from top to bottom, with horizontal order first
        Line2D key = null;

        if (p1.getX() <= p2.getX()){
            if (isEqual(p1.getX(), p2.getX())){
                if (p1.getY() > p2.getY()){
                    key = new Line2D.Double(p1,p2);
                }
                else{
                    key = new Line2D.Double(p2,p1);
                }
            }
            else{
                key = new Line2D.Double(p1,p2);
            }

        }
        else{
            key = new Line2D.Double(p2,p1);
        }



        return key;
    }

    //think this integer as the polygon id
    //borrow some of this from group 8
    public Map<Line2D, ArrayList<Integer>>getDistrictsByBorder(List<Polygon2D> districts) {
        Map<Line2D, ArrayList<Integer>>edgeMap = new HashMap<Line2D, ArrayList<Integer>>();

        for (HashMap.Entry<Integer, Polygon2D> entry : polygonMap.entrySet()){
            Polygon2D district = entry.getValue();
            for(int i = 0; i < district.getPoints().size(); i++) {
                Point2D startPoint = district.getPoints().get(i);
                int nextIdx = i + 1;
                if(i + 1 >= district.getPoints().size())  {
                    nextIdx = 0;
                }
                Point2D endPoint = district.getPoints().get(nextIdx);
                Line2D key = constructLineKey(startPoint, endPoint);
                if(edgeMap.containsKey(key)) {
                    edgeMap.get(key).add(entry.getKey());
                } else {
                    ArrayList<Integer>l = new ArrayList<>();
                    l.add(entry.getKey());
                    edgeMap.put(key, l);
                }
            }
        }

        return edgeMap;
    }

    //only occurs when y coordinates are the same
    //need to make sure this captures all cases
    //l1 is border, l2 is neighbor
    //code: 0 is l2 contained in l1, 1 is overlap, 2 is not overlap 3 is l1 contained in l2
    int strictOverlap(Line2D l1, Line2D l2){
        double l1_p1_x = l1.getP1().getX();
        double l1_p2_x = l1.getP2().getX();
        double l2_p1_x = l2.getP1().getX();
        double l2_p2_x = l2.getP2().getX();
        double l1_len = Math.abs(l1_p2_x - l1_p1_x);
        double l2_len = Math.abs(l2_p2_x - l2_p1_x);
        if (l1_p1_x < l2_p1_x ){
            if (l1_p2_x > l2_p1_x){
                //contain
                if (l2_p2_x <= l1_p2_x){
                    return 0;
                }
                //overlap
                else{
                    return 1;
                }

            }
            else{
                return 2;
            }

        }
        else{
            if (l2_p2_x <=l1_p1_x){
                return 2;
            }
            else{
                if (l1_p2_x <=l2_p2_x){
                    return 3;
                }
                else{
                    return 1;
                }

            }


        }

    }

    //Return the index of the matrix with the a double array representting the vertex for the edge.
    // [0] x1, [1] y1, [2] x2, [3] y2
    private double[] fromLine2DtoDouble(Line2D l){
        double[] edge = {l.getP1().getX(),l.getP1().getY(),l.getP2().getX(),l.getP2().getY()};
        return edge;
    }

    private Map<Integer, double[]> getAdjacentDistricts(int id) {
        Map<Integer, double[]> list = new HashMap<>();
        Polygon2D poly = polygonMap.get(id);
        //result is districts generated
        List<Point2D> points = poly.getPoints();
        List<Polygon2D> result = new ArrayList<Polygon2D>(polygonMap.values());
        Map<Line2D, ArrayList<Integer>> edgeMap = getDistrictsByBorder(result);
        for (int i = 0; i < points.size(); ++ i){
            Line2D border = constructLineKey(points.get(i), points.get(i + 1));


            //first find districts that are exactly match the border line
            //this should find districts that are to the left and to the right

            List<Integer> shareDistricts = edgeMap.get(border);
            for (int j = 0; j<shareDistricts.size();j++){
                if (shareDistricts.get(j)!= id){
                    //here should be a 2D thing
                    double[] edge = {points.get(i).getX(), points.get(i).getY(),
                            points.get(i + 1).getX(), points.get(i + 1).getY()};
                    list.put(shareDistricts.get(j), edge);
                }
            }

            //find districts that share some of the border line
            //this should find districts upstairs and downstairs

            //if neighbor is contained in border, return neighbor length
            //if overlap but not contained, return overlap
            //if border is contained in neighbor, return border length


            //neighbour contained in border
            for (HashMap.Entry<Line2D, ArrayList<Integer>> entry : edgeMap.entrySet()) {
                if (strictOverlap(border, entry.getKey()) == 0) {
                    //most of the time this loop should just be one time
                    for (int k = 0; k < entry.getValue().size();k++) {
                        if (entry.getValue().get(k)!=id){
                            list.put(entry.getValue().get(k), fromLine2DtoDouble(entry.getKey()));
                        }

                    }
                }
                //border contained in neighbour
                else if(strictOverlap(border, entry.getKey()) == 3){
                    for (int m = 0; m < entry.getValue().size();m++) {
                        if (entry.getValue().get(m)!=id){
                            list.put(entry.getValue().get(m), fromLine2DtoDouble(border));
                        }

                    }
                }
                else if(strictOverlap(border, entry.getKey()) == 1){
                    //turn Line2D to array double
                    double[] lineborder = fromLine2DtoDouble(border);
                    double[] lineneighbor = fromLine2DtoDouble(entry.getKey());

                    double[] edge_put = null;

                    //border has lower x
                    if (lineborder[0] < lineneighbor[0]) {
                        edge_put = new double[] {lineborder[2],lineborder[3],lineneighbor[0],lineneighbor[1]};

                    }
                    else{
                        edge_put = new double[] {lineneighbor[2],lineneighbor[3],lineborder[0],lineborder[1]};
                    }


                    for (int p = 0; p < entry.getValue().size(); p++) {
                        if (entry.getValue().get(p)!=id){
                            list.put(entry.getValue().get(p), edge_put);
                        }

                    }
                }
            }


        }

        return list;
    }

    //count number of people from the party to win
    private int countWin(List<Voter> voters) {

        int num = 0;
        for (int i = 0; i < voters.size(); i++) {
            List<Double> preference = voters.get(i).getPreference();
            //probability of red less than blue

            if ((partyToWin == 0) ? preference.get(0) > preference.get(1) : preference.get(0) < preference.get(1)) {
                num += 1;
            }
            //if same probability increase red by flipping a coin
            else if (isEqual(preference.get(0), preference.get(1))) {
                double x = random.nextDouble();
                if (x <= 0.5) {
                    num += 1;
                }
            }

        }
        return num;
    }

    // partyToWin is global variable now
    private boolean isSwingState(int id) {
        //should pass in polygon id here
        List<Voter> voters_curr = voterMap.get(id);
        int blue = countWin(voters_curr);
        //only a valid swing state between 0.42 and 0.5
        if (blue / voters_curr.size() > 0.42 && blue / voters_curr.size() < 0.5) {
            return true;
        }
        return false;
    }

    //Check population is valid for two polygon2 and if how beneficial it is for digging.
    private boolean isValidGerrymander(int swingId, int otherId, Polygon2D swing, Polygon2D other) {
        List<Voter> swing_voters = new ArrayList<>();


        //Recalculate people in proposed districts
        for (int i = 0; i < voterMap.get(swingId).size(); i++) {

            boolean contain_voter = swing.strictlyContains(voterMap.get(swingId).get(i).getLocation());
            if (contain_voter) {
                swing_voters.add(voterMap.get(swingId).get(i));

            }

        }

        for (int j = 0; j < voterMap.get(otherId).size(); j++) {

            boolean contain_voter = swing.strictlyContains(voterMap.get(otherId).get(j).getLocation());
            if (contain_voter) {
                swing_voters.add(voterMap.get(otherId).get(j));

            }

        }



        int num_win = countWin(swing_voters);
        if (num_win / swing_voters.size() > 0.5) {
            return true;
        }
        return false;
    }

    //Vertical adjacent edge.
    private void buildPolygonByY(List<Point2D> point2Ds, Polygon2D concave, Polygon2D convex, double x1, double y1,
                                 double width) {
        for (int j = 0; j < point2Ds.size(); j++) {
            Point2D point2D = point2Ds.get(j);
            concave.append(point2D);
            convex.append(point2D);
            // Build the triangle when bulding the overlapping edge.
            if (isEqual(point2D.getX(), x1) && j < point2Ds.size() - 1 &&
                    isEqual(point2Ds.get(j + 1).getX(), x1)) {
                double y3 = 0, y4 = 0;
                if (point2Ds.get(j + 1).getY() > point2D.getY()) {
                    y3 = y1 - width;
                    y4 = y1;
                }
                else {
                    y3 = y1;
                    y4 = y1 - width;
                }
                concave.append(x1, y3);
                concave.append(x1 - Math.sqrt(3)/2*width, (y4 + y3)/2);
                concave.append(x1, y4);
                convex.append(x1, y3);
                convex.append(x1 + Math.sqrt(3)/2*width, (y4 + y3)/2);
                convex.append(x1, y4);
            }
        }
    }

    //Horizontal adjacent edge.
    private void buildPolygonByX(List<Point2D> point2Ds, Polygon2D concave, Polygon2D convex, double y1, double x1,
                                 double width) {
        for (int j = 0; j < point2Ds.size(); j++) {
            Point2D point2D = point2Ds.get(j);
            concave.append(point2D);
            convex.append(point2D);
            // Build the triangle when bulding the overlapping edge.
            if (isEqual(point2D.getY(), y1) && j < point2Ds.size() - 1 &&
                    isEqual(point2Ds.get(j + 1).getY(), y1)) {
                double x3 = 0, x4 = 0;
                if (point2Ds.get(j + 1).getX() > point2D.getX()) {
                    x3 = x1;
                    x4 = x1 + width;
                }
                else {
                    x3 = x1 + width;
                    x4 = x1;
                }
                concave.append(x3, y1);
                concave.append((x3 + x4)/2, y1 + Math.sqrt(3)/2*width);
                concave.append(x4, y1);
                convex.append(x3, y1);
                convex.append((x3 + x4)/2, y1 - Math.sqrt(3)/2*width);
                convex.append(x4, y1);
            }
        }
    }

    private boolean setNewPolygon(int id, int otherId, Polygon2D convexSwing, Polygon2D concaveAdjacent,
                                  Polygon2D concaveSwing, Polygon2D convexAdjacent) {
        if (isValidGerrymander(id, otherId, convexSwing, concaveAdjacent)) {
            checkMap.put(id, true);
            checkMap.put(otherId, true);
            polygonMap.put(id, convexSwing);
            polygonMap.put(otherId, concaveAdjacent);
            return true;
        }
        if (isValidGerrymander(id, otherId, concaveSwing, convexAdjacent)) {
            checkMap.put(id, true);
            checkMap.put(otherId, true);
            polygonMap.put(id, concaveSwing);
            polygonMap.put(otherId, convexAdjacent);
            return true;
        }
        return false;
    }

    private boolean isEqual(double a, double b) {
        return Math.abs(a - b) <= eps;
    }

}