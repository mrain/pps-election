package election.sim;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.awt.geom.*;

public class PlayerWrapper {
    private Timer thread;
    private Player player;
    private String name;
    private int n;
    private long timeout, originalTimeout, seed;
    private boolean timedOut;
    private boolean rte;

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public void setRte(boolean rte) {
        this.rte = rte;
    }

    public PlayerWrapper(Player player, String name, long timeout) {
        this.player = player;
        this.name = name;
        this.timeout = timeout;
        originalTimeout = timeout;
        this.timedOut = false;
        this.rte = false;
        thread = new Timer();
    }


    public void init(int numVoters, int numParties, int numDivision, long seed) {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> {
            player.init(numVoters, numParties, numDivision, seed);
            return null;
        });
        try {
            thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + " timed out.");
            return;
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return;
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
    }

    public List<Polygon2D> getDistricts(List<Voter> voters) {
        if (!thread.isAlive()) thread.start();
        thread.call_start(() -> player.getDistricts(voters));
        List<Polygon2D> ret;
        try {
            ret = thread.call_wait(timeout);
        } catch (TimeoutException e) {
            this.timedOut = true;
            System.err.println("Player " + name + " timed out.");
            return null;
        } catch (Exception e) {
            this.rte = true;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
            return null;
        }
        long elapsedTime = thread.getElapsedTime();
        timeout -= elapsedTime;
        return ret;
    }

    public long getTotalElapsedTime() {
        return originalTimeout - timeout;
    }

    public boolean isActive() {
        return !timedOut && !rte;
    }
}
