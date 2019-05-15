import java.util.Random;

public class TaskUtils {

    public static Random random = new Random(1);
    public static World world;
    public static int step;
    public static Worker[] changedWorkers = new Worker[300];
    public static Node[] changedNodes = new Node[300];
    public static int changedWorkersIdx = 0;
    public static int changeNodesIdx = 0;
    public static int MAX_DURATION = 15000;
    public static int MAX_STEPS = 7000000 * 3;
    public static boolean STEPS_BASED = true;
    public static long start = System.currentTimeMillis();
    public static double fraction;
    public static int[][] dists;

    public static int dist(Node a, Node b) {
        /*return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);*/
        return dists[a.id][b.id];
    }

    public static void fillFraction() {
        if (STEPS_BASED) {
            fraction = step / (double) MAX_STEPS;
        } else {
            fraction = (System.currentTimeMillis() - TaskUtils.start) / (double) TaskUtils.MAX_DURATION;
        }
    }

    public static boolean finished() {
        if (STEPS_BASED) {
            return step >= MAX_STEPS;
        } else {
            long diff = (System.currentTimeMillis() - TaskUtils.start);
            return diff > MAX_DURATION - 400;
        }
    }

    public static void clear() {
        changedWorkersIdx = 0;
        changeNodesIdx = 0;
    }

    public static void changeWorker(Worker w) {
        changedWorkers[changedWorkersIdx] = w;
        changedWorkersIdx += 1;
    }

    public static void changeNode(Node n) {
        changedNodes[changeNodesIdx] = n;
        changeNodesIdx += 1;
    }

    public static void init() {
        dists = new int[world.nodes.length][world.nodes.length];
        for (int i = 1; i < world.nodes.length; i++) {
            Node n = world.nodes[i];
            for (int j = 1; j < world.nodes.length; j++) {
                Node ne = world.nodes[j];
                int dst = Math.abs(ne.x - n.x) + Math.abs(ne.y - n.y);
                dists[ne.id][n.id] = dst;
                dists[n.id][ne.id] = dst;
            }
        }
    }

}
