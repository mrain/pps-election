package election.sim;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.geom.*;
import java.io.*;
import java.lang.*;
import java.lang.IllegalArgumentException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Simulator {
    private static long seed = 20190918;
    private static long playerTimeout = 10000;

    private static final String root = "election";
    private static Random random;
    private static boolean silent = false;
    private static double fps = 5;

    private static int numVoters = 333333;
    private static int numParties = 2;
    private static int numDistricts = 243;
    private static Polygon2D board;

    private static String mapPath;
    private static String playerName = "g0";
    private static Player player;
    private static String resultPath = "result.dat";

    public static void main(String[] args) throws Exception {
//		args = new String[] {"-p", "g1", "g2", "g3", "g4", "g5", "g6", "-n", "10", "-t", "100", "-s", "1411390388"};
        parseArgs(args);
        random = new Random(seed);
        board = new Polygon2D();
        board.append(0., 0.);
        board.append(1000., 0.);
        board.append(500., 500. * Math.sqrt(3));
        // Simulation starts!
        List<Voter> voters = loadMap(mapPath);
        PlayerWrapper player = new PlayerWrapper(loadPlayer(playerName), playerName, playerTimeout);
        player.init(numVoters, numParties, numDistricts, seed);
        List<Polygon2D> districts = player.getDistricts(voters);

        Log.record("Total running time for player " + playerName + " is " + player.getTotalElapsedTime() + "ms");

        // Validate result
        double totalArea = board.area();
        double sumArea = 0;
        if (districts.size() != numDistricts) {
            Log.record("Incorrect number of districts.");
            System.exit(-1);
        }
        for (int i = 0; i < districts.size(); ++ i) {
            Polygon2D district = districts.get(i);
            if (district.size() > 9) {
                Log.record("District has too many sides.");
                System.exit(-1);
            }
            sumArea += district.area();
            for (int j = 0; j < i; ++ j)
                if (district.overlap(districts.get(j))) {
                    Log.record("Overlapping districts " + i + " and " + j);
                    Log.record(district.toString());
                    Log.record(districts.get(j).toString());
                    System.exit(-1);
                }
        }
        if (Math.abs(sumArea - totalArea) > 1e-7) {
            Log.record("There are some empty areas");
            System.exit(-1);
        }
        Log.record("Saving result to " + resultPath);
        saveRawData(resultPath, voters, districts);
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
        numParties = sc.nextInt();
        for (int i = 0; i < n; ++ i) {
            double x, y;
            List<Double> pref = new ArrayList<Double>();
            x = sc.nextDouble();
            y = sc.nextDouble();
            Point2D location = new Point2D.Double(x, y);
            if (!board.contains(location))
                throw new IllegalArgumentException("Voter's location is out of scope.");
            for (int j = 0; j < numParties; ++ j) {
                double a = sc.nextDouble();
                if (a > 1.0 || a < 0)
                    throw new IllegalArgumentException("Voter's preference is out of scope");
                pref.add(a);
            }
            voters.add(new Voter(location, pref));
        }
        return voters;
    }

    private static void parseArgs(String[] args) {
        int i = 0;
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].startsWith("-p") || args[i].equals("--players")) {
                        ++i;
                        playerName = args[i];
                    } else if (args[i].equals("-n")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of voters");
                        }
                        numVoters = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-s") || args[i].equals("--seed")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing seed");
                        }
                        seed = Long.parseLong(args[i]);
                    } else if (args[i].equals("-m") || args[i].equals("--map")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing map file");
                        }
                        mapPath = args[i];
                    } else if (args[i].equals("-r") || args[i].equals("--result")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing result file");
                        }
                        resultPath = args[i];
                    } else if (args[i].equals("-tl") || args[i].equals("--timelimit")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit");
                        }
                        playerTimeout = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-l") || args[i].equals("--logfile")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing logfile name");
                        }
                        Log.setLogFile(args[i]);
                    } else if (args[i].equals("--silent")) {
                        silent = true;
                    } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                        Log.activate();
                    } else {
                        throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
            }
        }
        Log.record("Player: " + playerName);
        Log.record("Map file: " + mapPath);
        Log.record("Time limit for each player: " + playerTimeout + "ms");
    }

    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<File>();
        Set<File> prev_dirs = new HashSet<File>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<File>();
            for (File dir : prev_dirs)
                for (File file : dir.listFiles())
                    if (!file.canRead()) ;
                    else if (file.isDirectory())
                        next_dirs.add(file);
                    else if (file.getPath().endsWith(extension))
                        files.add(file);
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    public static Player loadPlayer(String name) throws IOException, ClassNotFoundException, InstantiationException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
                class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
            Log.record("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "Player.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class<?> raw_class = loader.loadClass(root + "." + name + ".Player");
        return (Player) raw_class.getConstructor().newInstance();
    }

    private static long last_modified(Iterable<File> files) {
        long last_date = 0;
        for (File file : files) {
            long date = file.lastModified();
            if (last_date < date)
                last_date = date;
        }
        return last_date;
    }
}
