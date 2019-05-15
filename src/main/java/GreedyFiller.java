import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GreedyFiller {

    public void fill(World world) {
        for (int i = 1; i < world.nodes.length; i++) {
            Node n = world.nodes[i];
            fillNeigbours(world, n);
        }
        /*if (!world.checkConsistent()) {
            System.out.println("inconsistent");
        }*/
    }

    public void fillNeigbours(final World world, final Node node) {
        sortNeighbours(world, node);
        for (int i = 0; i < node.neighbours.length; i++) {
            Node neighbour = world.nodes[node.neighbours[i]];
            if (!isValid(node, neighbour)) {
                continue;
            }
            List<Worker> free = getFreeWorkers(node);
            if (free.isEmpty()) {
                break;
            }
            List<Worker> base = getBaseWorkers(neighbour);
            if (base.isEmpty()) {
                continue;
            }
            for (int wid = 0; wid < Math.min(base.size(), free.size()); wid++) {
                Worker b = base.get(wid);
                Worker f = free.get(wid);
                f.setNodeTo(neighbour.id);
                f.setToIdx(b.getId());
                b.setNodeFrom(node.id);
                b.setFromIdx(f.getId());
            }
            fillNeigbours(world, neighbour);
        }
    }

    List<Worker> getFreeWorkers(Node n) {
        List<Worker> result = new ArrayList<>(7);
        for (Worker w : n.workers) {
            if (w.nodeTo == 0) {
                result.add(w);
            }
        }
        return result;
    }

    List<Worker> getBaseWorkers(Node n) {
        List<Worker> result = new ArrayList<>(7);
        for (Worker w : n.workers) {
            if (w.nodeFrom == 0) {
                result.add(w);
            }
        }
        return result;
    }

    int getBaseWorkersSize(Node n) {
        int result = 0;
        for (Worker w : n.workers) {
            if (w.nodeFrom == 0) {
                result += 1;
            }
        }
        return result;
    }

    public void sortNeighbours(final World world, final Node node) {
        Integer[] neighbours = new Integer[node.neighbours.length];
        for (int i = 0; i < neighbours.length; i++) {
            neighbours[i] = node.neighbours[i];
        }
        Arrays.sort(neighbours, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                if (i1.intValue() == i2.intValue()) {
                    return 0;
                }
                Node n1 = world.nodes[i1];
                Node n2 = world.nodes[i2];
                boolean v1 = isValid(node, n1);
                boolean v2 = isValid(node, n2);
                if (v1 && !v2) {
                    return -1;
                }
                if (v2 && !v1) {
                    return 1;
                }
                if (!v2) {
                    return 0;
                }
                int diff1 = getBaseWorkersSize(n1);
                int diff2 = getBaseWorkersSize(n2);
                return Integer.compare(diff1, diff2);
            }
        });
    }

    static boolean isValid(Node n, Node neighbour) {
        int start = n.getStartTime() + n.duration;
        int dist = TaskUtils.dist(n, neighbour);
        int reachTime = start + dist;
        return reachTime < neighbour.getStartTime();
    }

}
