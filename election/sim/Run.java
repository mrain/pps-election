package election.sim;

import java.awt.geom.*;
import java.io.*;
import java.lang.*;
import java.lang.IllegalArgumentException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.InvocationTargetException;

public class Run {
    private static long seed = 20190918;
    private static long timeLimit = 10000;
    private static long elapsedTime;

    private static Random random;

    private static int numVoters;
    private static int numParties;
    private static int numDistricts = 81;
    private static int repPerDistrict = 3;
    private static Polygon2D board;

    private static String moduleName;
    private static String mapPath;
    private static String resultPath = "result.dat";
    private static Timer thread;

    private static List<Voter> voters;
    private static List<Polygon2D> districts;

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        random = new Random(seed);
        board = new Polygon2D();
        board.append(0., 0.);
        board.append(1000., 0.);
        board.append(500., 500. * Math.sqrt(3));


        if (args[0].equals("run")) {
            voters = loadMap(mapPath);
            districts = new ArrayList<Polygon2D>();
            thread = new Timer();
            thread.start();
            thread.call_start(() -> {
                return loadDistrictGenerator(moduleName).getDistricts(voters, repPerDistrict, seed);
            });
            try {
                districts = thread.call_wait(timeLimit);
            } catch (TimeoutException e) {
                Log.record("Timed out!");
                System.err.println("Generator timed out.");
                System.exit(-1);
            }
            elapsedTime = thread.getElapsedTime();
            Log.record("Generator finished in " + elapsedTime + "ms.");
        } else if (args[0].equals("verify")) {
            loadResultFile(resultPath);
        }

        // Checking
        if (districts.size() != numDistricts)
            throw new IllegalArgumentException("Incorrect number of districts, expected: " + numDistricts + ", received: " + districts.size() + ".");
        double totalArea = board.area(), sumArea = 0;
        int sumVoters = 0;
        int avgL = numVoters / numDistricts;
        int avgU = avgL + (numVoters % numDistricts == 0 ? 0 : 1);
        int UB = avgU + (avgU / 10 + (avgU % 10 == 0 ? 0 : 1));// Math.ceil(1.1 * (double) numVoters / numDistricts);
        int LB = avgL - avgL / 10;
//        System.err.println(LB + " " + UB);
        for (int i = 0; i < districts.size(); ++ i) {
            Polygon2D district = districts.get(i);
            if (district.area() < 1e-7)
                throw new IllegalArgumentException("District cannot have empty area: " + district.toString());
            if (!board.contains(district)) {
                throw new IllegalArgumentException("District out of scope: " + district.toString());
            }
            int t = countInclusion(voters, district);
            sumVoters += t;
            if (t < LB || t > UB) {
                throw new IllegalArgumentException("District contains too much/few voters (" + t + "): " + district.toString());
            }
            for (int j = 0; j < i; ++ j)
                if (district.overlap(districts.get(j)))
                    throw new IllegalArgumentException("Overlapping districts: (" + district.toString() + ") and (" + districts.get(j).toString() + ")");
            sumArea += district.area();
        }
        if (Math.abs(sumArea - totalArea) < 1e-8) {
            throw new IllegalArgumentException("Empty area not covered by any district.");
        }
        if (sumVoters != numVoters) {
            throw new IllegalArgumentException("Some voters doesn't belong to any district.");
        }

        if (args[0].equals("run")) {
            saveRawData(resultPath, voters, districts);
        }
        Log.record("Passed verification");
        System.exit(0);
    }

    public static int countInclusion(List<Voter> voters, Polygon2D polygon) {
        int ret = 0;
        for (Voter voter : voters)
            if (polygon.strictlyContains(voter.getLocation()))
                ++ ret;
        return ret;
    }

    public static void saveRawData(String path, List<Voter> voters, List<Polygon2D> districts)  throws IOException {
        Log.record("Saving data to " + path);
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write(voters.size() + " " + voters.get(0).getPreference().size() + "\n");
        for (Voter voter : voters)
            writer.write(voter.toString() + "\n");
        writer.write(districts.size() + "\n");
        for (Polygon2D district : districts) {
            writer.write(district.toString() + "\n");
        }
        writer.close();
    }

    private static void loadResultFile(String path) throws IllegalArgumentException, FileNotFoundException {
        voters = new ArrayList<Voter>();
        districts = new ArrayList<Polygon2D>();
        File file = new File(path);
        Scanner sc = new Scanner(file);
        numVoters = sc.nextInt();
        numParties = sc.nextInt();
        for (int i = 0; i < numVoters; ++ i) {
            double x, y;
            List<Double> pref = new ArrayList<Double>();
            x = sc.nextDouble();
            y = sc.nextDouble();
            Point2D location = new Point2D.Double(x, y);
            for (int j = 0; j < numParties; ++ j) {
                double a = sc.nextDouble();
                pref.add(a);
            }
            voters.add(new Voter(location, pref));
        }
        Log.record("Loaded map with " + numVoters + " voters and " + numParties + " parties.");

        int n = sc.nextInt(), m;
        for (int i = 0; i < n; ++ i) {
            m = sc.nextInt();
            Polygon2D polygon = new Polygon2D();
            for (int j = 0; j < m; ++ j) {
                double x = sc.nextDouble();
                double y = sc.nextDouble();
                polygon.append(x, y);
            }
            districts.add(polygon);
        }
        Log.record("Loaded " + districts.size() + " districts.");
    }

    private static List<Voter> loadMap(String mapPath) throws IllegalArgumentException, FileNotFoundException {
        List<Voter> voters = new ArrayList<Voter>();
        File file = new File(mapPath);
        Scanner sc = new Scanner(file);
        numVoters = sc.nextInt();
        numParties = sc.nextInt();
        for (int i = 0; i < numVoters; ++ i) {
            double x, y;
            List<Double> pref = new ArrayList<Double>();
            x = sc.nextDouble();
            y = sc.nextDouble();
            Point2D location = new Point2D.Double(x, y);
            for (int j = 0; j < numParties; ++ j) {
                double a = sc.nextDouble();
                pref.add(a);
            }
            voters.add(new Voter(location, pref));
        }
        Log.record("Loaded map with " + numVoters + " voters and " + numParties + " parties.");
        int n = sc.nextInt();
        for (int i = 0; i < numVoters; ++ i) {

        }
        return voters;
    }

    private static void parseArgs(String[] args) {
        List<String> cmds = new ArrayList<String>();
        int i = 0;
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].equals("-rep")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of representatives per district");
                        }
                        repPerDistrict = Integer.parseInt(args[i]);
                        numDistricts = 243 / repPerDistrict;
                    } else if (args[i].equals("-s") || args[i].equals("--seed")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing seed");
                        }
                        seed = Long.parseLong(args[i]);
                    } else if (args[i].equals("-m") || args[i].equals("--map")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing map file path");
                        }
                        mapPath = args[i];
                    } else if (args[i].equals("-r") || args[i].equals("--result")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing result file path");
                        }
                        resultPath = args[i];
                    } else if (args[i].equals("-tl") || args[i].equals("--timelimit")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit");
                        }
                        timeLimit = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                        Log.activate();
                    } else {
                        throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
                    }
                    break;
                default:
                    cmds.add(args[i]);
            }
        }
        args = cmds.toArray(new String[0]);
        if (args.length == 0) {
            throw new IllegalArgumentException("No command specified.");
        }
        if (args[0].equals("run")) {
            if (mapPath == null) throw new IllegalArgumentException("No map file specified.");
            if (args.length < 2) throw new IllegalArgumentException("No module specified.");
            moduleName = args[1];
        } else if (args[0].equals("verify")) {
            // Nothing to do?
            if (resultPath == null)
                throw new IllegalArgumentException("No result file specified.");
        } else {
            throw new IllegalArgumentException("Unknown command " + args[0]);
        }
    }

    public static DistrictGenerator loadDistrictGenerator(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return (DistrictGenerator) Class.forName(className).getConstructor().newInstance();
    }
}
