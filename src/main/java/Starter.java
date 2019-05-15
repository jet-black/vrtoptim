import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Starter {

    static int[][] locations;
    static SolutionPrinter solutionPrinter = new SolutionPrinter();

    public static void main(String[] args) {
        solve(getReader());
    }

    public static void solve(InputStream is) {
        MyInputReader in = new MyInputReader(is);
        int n = in.nextInt();
        locations = new int[n][6];
        int[][] map = new int[101][101];
        for (int i = 0; i < n; i++) {
            locations[i][0] = in.nextInt();
            locations[i][1] = in.nextInt();
            locations[i][2] = in.nextInt();
            locations[i][3] = in.nextInt();
            locations[i][4] = in.nextInt();
            locations[i][5] = in.nextInt();
        }
        int baseX = locations[0][0];
        int baseY = locations[0][1];
        for (int i = 1; i < n; i++) {
            int[] loc = locations[i];
            map[loc[0]][loc[1]] = i;
        }
        World world = new World();
        TaskUtils.world = world;
        world.nodes = new Node[n];
        for (int i = 1; i < n; i++) {
            Node node = new Node();
            int[] loc = locations[i];
            int x = loc[0];
            int y = loc[1];
            int d = loc[2];
            int p = loc[3];
            int l = loc[4];
            int h = loc[5];
            node.workers = new Worker[p];
            for (int j = 0; j < p; j++) {
                node.workers[j] = new Worker(i, 0, 0, 0, 0);
                node.workers[j].setId(j);
            }
            node.minBeginTime = l;
            node.maxEndTime = h;
            node.duration = d;
            node.x = x;
            node.y = y;
            node.distToBase = Math.abs(baseX - x) + Math.abs(baseY - y);
            node.id = i;
            setNeighbours(node, map, locations, 12);
            /*setNeighboursSmart(node, map, locations, 10 + 5);*/
            world.nodes[i] = node;
            int rnd = TaskUtils.random.nextInt(h - l - d);
            node.setStartTimeUnsafe(l + rnd);
            for (int j = 0; j < p; j++) {
                node.workers[j].setFromBase();
            }
            node.backup();
        }
        /*truncateNeighbours();*/
        TaskUtils.init();
        GreedyFiller greedyFiller = new GreedyFiller();
        greedyFiller.fill(world);

        world.init();

        while (!TaskUtils.finished()) {
            for (int i = 0; i < 10000; i++) {
                world.step();
            }
        }
        int sc = 0;
        for (int i = 1; i < world.nodes.length; i++) {
            Node node = world.nodes[i];
            sc += node.getScore();
        }
        System.out.println(sc);
        System.out.println(world.prevScore);
        /*solutionPrinter.print();*/
    }

    public static void setNeighbours(Node node, int[][] map, int[][] locations, int diffThreshold) {
        int radius = 40;
        int cur = 1;
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int nx = Math.max(0, node.x - radius); nx < Math.min(map.length, node.x + radius); nx++) {
            for (int ny = Math.max(0, node.y - radius); ny < Math.min(map.length, node.y + radius); ny++) {
                int diff = Math.abs(nx - node.x) + Math.abs(ny - node.y);
                if (diff > diffThreshold) {
                    continue;
                }
                if (nx == node.x && ny == node.y) {
                    continue;
                }
                int nid = map[nx][ny];
                if (nid == 0) {
                    continue;
                }
                int[] loc = locations[nid];
                int minEndTime = node.minBeginTime + node.duration;
                int dist = Math.abs(loc[0] - node.x) + Math.abs(loc[1] - node.y);
                int reachTime = minEndTime + dist;
                int neighbourStartMaxTime = loc[5] - loc[2];
                if (reachTime < neighbourStartMaxTime) {
                    result.add(nid);
                }
            }
        }
        if (result.size() < 30 && diffThreshold < 50) {
            setNeighbours(node, map, locations, diffThreshold + 5);
        } else {
            node.neighbours = new int[result.size()];
            for (int i = 0; i < result.size(); i++) {
                node.neighbours[i] = result.get(i);
            }
        }
    }

    public static void setNeighboursSmart(Node node, int[][] map, int[][] locations, int diffThreshold) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int rad = 1; rad < 50; rad++) {
            if (result.size() > 80) {
                break;
            }
            for (int i = 0; i < rad; i++) {
                int leftX = node.x - rad + i;
                int leftY = node.y - i;
                int topX = node.x + i;
                int topY = node.y - rad + i;
                int rightX = node.x + rad - i;
                int rightY = node.y + i;
                int bottomX = node.x - i;
                int bottomY = node.y + rad - i;
                addNeighbour(result, node, map, locations, leftX, leftY);
                addNeighbour(result, node, map, locations, topX, topY);
                addNeighbour(result, node, map, locations, rightX, rightY);
                addNeighbour(result, node, map, locations, bottomX, bottomY);
            }
        }
        node.neighbours = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            node.neighbours[i] = result.get(i);
        }
    }

    public static void addNeighbour(List<Integer> result, Node node, int[][] map, int[][] locations, int x, int y) {
        if (x < 0 || y < 0 || x >= map.length || y >= map.length) {
            return;
        }
        int nid = map[x][y];
        if (nid == 0) {
            return;
        }
        int[] loc = locations[nid];
        int minEndTime = node.minBeginTime + node.duration;
        int dist = Math.abs(loc[0] - node.x) + Math.abs(loc[1] - node.y);
        int reachTime = minEndTime + dist;
        int neighbourStartMaxTime = loc[5] - loc[2];
        if (reachTime < neighbourStartMaxTime) {
            result.add(nid);
        }
    }


    private static void sortNeighbours(final World world, final Node node) {
        Integer[] arr = new Integer[node.neighbours.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = node.neighbours[i];
        }
        Arrays.sort(arr, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int d1 = TaskUtils.dist(node, world.nodes[o1]);
                int d2 = TaskUtils.dist(node, world.nodes[o2]);
                return Integer.compare(d1, d2);
            }
        });
        for (int i = 0; i < arr.length; i++) {
            node.neighbours[i] = arr[i];
        }
    }

    private static InputStream getReader() {
        return Starter.class.getClassLoader().getResourceAsStream("001.txt");
        /*return System.in;*/
    }

    static class MyInputReader {
        public BufferedReader reader;
        public StringTokenizer tokenizer;

        public MyInputReader(InputStream stream) {
            reader = new BufferedReader(new InputStreamReader(stream), 32768);
            tokenizer = null;
        }

        public String next() {
            while (tokenizer == null || !tokenizer.hasMoreTokens()) {
                try {
                    tokenizer = new StringTokenizer(reader.readLine());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return tokenizer.nextToken();
        }

        public int nextInt() {
            return Integer.parseInt(next());
        }

        public long nextLong() {
            return Long.parseLong(next());
        }

    }

    static class SolutionPrinter {

        void print() {
            for (int i = 1; i < TaskUtils.world.nodes.length; i++) {
                Node n = TaskUtils.world.nodes[i];
                if (n.isWiped()) {
                    continue;
                }
                for (int j = 0; j < n.workers.length; j++) {
                    Worker w = n.workers[j];
                    if (w.nodeFrom == 0) {
                        printWorker(w);
                    }
                }
            }
        }

        void printWorker(Worker w) {
            Worker cur = w;
            Node n = TaskUtils.world.nodes[w.nodeId];
            int start = n.getStartTime() - n.distToBase;
            System.out.println("start " + start + " 1");
            do {
                n = TaskUtils.world.nodes[cur.nodeId];
                start = n.getStartTime();
                int end = n.getStartTime() + n.duration;
                System.out.println("arrive " + start + " " + (n.id + 1) + " ");
                System.out.println("work " + start + " " + end + " " + (n.id + 1));
                if (cur.nodeTo == 0) {
                    break;
                }
                Node nodeTo = TaskUtils.world.nodes[cur.nodeTo];
                cur = nodeTo.workers[cur.toIdx];
            } while (true);
            int end = n.getStartTime() + n.duration + n.distToBase;
            System.out.println("arrive " + end + " 1");
            System.out.println("end");
        }

    }


}

