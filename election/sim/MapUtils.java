package election.sim;

import java.awt.geom.*;
import java.io.*;
import java.lang.*;
import java.lang.IllegalArgumentException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.InvocationTargetException;

public class MapUtils {
    private static long seed = 20190918;
    private static long timeLimit = 10000;
    private static long elapsedTime;

    private static final String root = "election";
    private static Random random;

    private static int numVoters = 333333;
    private static int numParties = 2;
    private static Polygon2D board;

    private static String moduleName;
    private static String mapPath;
    private static Timer thread;

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        random = new Random(seed);
        board = new Polygon2D();
        board.append(0., 0.);
        board.append(1000., 0.);
        board.append(500., 500. * Math.sqrt(3));

        List<Voter> voters = new ArrayList<Voter>();
        if (args[0].equals("run")) {
            Log.record("Generating map with " + numVoters + " voters and " + numParties + " parties.");
            thread = new Timer();
            thread.start();
            thread.call_start(() -> {
                return loadMapGenerator(moduleName).getVoters(numVoters, numParties, seed);
            });
            try {
                voters = thread.call_wait(timeLimit);
            } catch (TimeoutException e) {
                Log.record("Timed out!");
                System.err.println("Generator timed out.");
                System.exit(-1);
            }
            elapsedTime = thread.getElapsedTime();
            Log.record("Generator finished in " + elapsedTime + "ms.");
        } else if (args[0].equals("verify")) {
            voters = loadMap(mapPath);
        }

        if (numVoters != voters.size()) {
            throw new IllegalArgumentException("Incorrect number of voters, expected: " + numVoters + ", received: " + voters.size() + ".");
        }

        for (Voter voter : voters) {
            if (!board.strictlyContains(voter.getLocation())) {
                throw new IllegalArgumentException("Voter's location is out of scope': (" + voter.toString() + ").");
            }
            if (voter.getPreference().size() != numParties) {
                throw new IllegalArgumentException("Invalid preference for voter: (" + voter.toString() + ").");
            }
            for (Double d : voter.getPreference())
                if (d < 0 || d > 1) {
                    throw new IllegalArgumentException("Invalid preference for voter: (" + voter.toString() + ").");
                }
        }

        Log.record("Passed verification");

        if (args[0].equals("run") && mapPath != null) {
            Log.record("Saving map file to " + mapPath + ".");
            saveRawData(mapPath, voters, new ArrayList<Polygon2D>());
        } else if (args[0].equals("verify")) {
            System.out.println("Passed!");
        }
        System.exit(0);
    }

    public static void saveRawData(String path, List<Voter> voters, List<Polygon2D> districts)  throws IOException {
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
    private static List<Voter> loadMap(String mapPath) throws IllegalArgumentException, FileNotFoundException {
        List<Voter> voters = new ArrayList<Voter>();
        File file = new File(mapPath);
        Scanner sc = new Scanner(file);
        int n = sc.nextInt();
        int p = sc.nextInt();
        for (int i = 0; i < n; ++ i) {
            double x, y;
            List<Double> pref = new ArrayList<Double>();
            x = sc.nextDouble();
            y = sc.nextDouble();
            Point2D location = new Point2D.Double(x, y);
            for (int j = 0; j < p; ++ j) {
                double a = sc.nextDouble();
                pref.add(a);
            }
            voters.add(new Voter(location, pref));
        }
        return voters;
    }

    private static void parseArgs(String[] args) {
        List<String> cmds = new ArrayList<String>();
        int i = 0;
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].equals("-n")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of voters");
                        }
                        numVoters = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-p")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of parties");
                        }
                        numParties = Integer.parseInt(args[i]);
                    }else if (args[i].equals("-s") || args[i].equals("--seed")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing seed");
                        }
                        seed = Long.parseLong(args[i]);
                    } else if (args[i].equals("-m") || args[i].equals("--map")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing map file");
                        }
                        mapPath = args[i];
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
            if (args.length < 2) throw new IllegalArgumentException("No module specified.");
            moduleName = args[1];
        } else if (args[0].equals("verify")) {
            // Nothing to do?
            if (mapPath == null)
                throw new IllegalArgumentException("No map file specified.");
        } else {
            throw new IllegalArgumentException("Unknown command " + args[0]);
        }
    }

    public static MapGenerator loadMapGenerator(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return (MapGenerator) Class.forName(className).getConstructor().newInstance();
    }
}
