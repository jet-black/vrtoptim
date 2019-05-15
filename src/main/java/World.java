import java.util.Random;

    public class World {

    Node[] nodes;
    int[] borrowFromWorkers = new int[7];
    int[] borrowToWorkers = new int[7];
    int prevScore;
    double mean = -1100;
    double std = 700;
    int[] scores = new int[2000];
    Worker[] wipedFrom = new Worker[7];
    Worker[] wipedTo = new Worker[7];
    int wipedFromLen = 0;
    int wipedToLen = 0;


    public void init() {
        prevScore = 0;
        for (int i = 1; i < nodes.length; i++) {
            Node node = nodes[i];
            int ns = node.getScore();
            prevScore += ns;
            scores[node.id] = ns;
        }
    }

    public void step() {
        wipedFromLen = 0;
        wipedToLen = 0;
        TaskUtils.clear();
        TaskUtils.fillFraction();
        TaskUtils.step += 1;
        Random random = TaskUtils.random;
        double prob = random.nextDouble();
        Node node = nodes[random.nextInt(nodes.length - 1) + 1];
        node.backup();
        boolean time = false;
        if (prob < 0.00) {
            wipe(node);
        } else if (prob < 0.2) {
            int curTime = getCurTime(node) - node.getStartTime();
            changeTime(node, curTime);
            time = true;
        } else {
            borrow(node, 0);
        }
        int result = prevScore;
        for (int i = 0; i < TaskUtils.changeNodesIdx; i++) {
            Node n = TaskUtils.changedNodes[i];
            result -= scores[n.id];
            result += n.getScore();
        }
        if (shouldPick(prevScore, result, TaskUtils.step, time)) {
            this.prevScore = result;
            commit();
        } else {
            rollback();
        }
        /*checkConsistent();*/
    }

    private void commit() {
        for (int i = 0; i < TaskUtils.changeNodesIdx; i++) {
            Node n = TaskUtils.changedNodes[i];
            scores[n.id] = n.getScore();
        }
    }

    private boolean shouldPick(int prevScore, int curScore, int step, boolean time) {
        if (curScore >= prevScore) {
            return true;
        }
        double diff = curScore - prevScore;
        double stepFraction = TaskUtils.fraction;
        if (stepFraction > 0.7) {
            return false;
        }
        double mul = time ? 50.0 : 200.0;
        double prob = Math.exp((diff / (-mean) * Math.max(stepFraction, 0.1) * mul));
        boolean result = prob > TaskUtils.random.nextDouble();
        return result;
    }

    private void wipe(Node node) {
        if (node.isWiped()) {
            return;
        }
        wipeInner(node);
        node.setWiped(true);
    }

    private void changeTime(Node node, int by) {
        if (node.isWiped()) {
            node.setWiped(false);
            wipeInner(node);
        }
        if (by == 0) {
            return;
        }
        int curTime = node.getStartTime() + by;
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeTo == 0) {
                continue;
            }
            Node nodeTo = nodes[w.nodeTo];
            nodeTo.backup();
            int t = curTime + node.duration + TaskUtils.dist(node, nodeTo);
            if (t > nodeTo.getStartTime()) {
                Worker workerTo = nodeTo.workers[w.toIdx];
                workerTo.setFromIdx(0);
                workerTo.setNodeFrom(0);
                w.setToIdx(0);
                w.setNodeTo(0);
            }
        }
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeFrom == 0) {
                continue;
            }
            Node from = nodes[w.nodeFrom];
            from.backup();
            int t = from.getStartTime() + from.duration + TaskUtils.dist(node, from);
            if (t > curTime) {
                Worker fromWorker = from.workers[w.fromIdx];
                fromWorker.setToIdx(0);
                fromWorker.setNodeTo(0);
                w.setFromIdx(0);
                w.setNodeFrom(0);
            }
        }
        node.setStartTime(curTime);
    }

    private int getCurTime(Node node) {
        int curTime = node.getStartTime() + TaskUtils.random.nextInt(27) - 13;
        /*int curTime = node.getStartTime() + TaskUtils.random.nextInt(31) - 15;*/
        curTime = Math.min(curTime, node.maxEndTime - node.duration);
        return Math.max(curTime, node.minBeginTime);
    }

    private void wipeInner(Node node) {
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeTo == 0) {
                continue;
            }
            Node nodeTo = nodes[w.nodeTo];
            Worker workerTo = nodeTo.workers[w.toIdx];
            workerTo.setFromIdx(0);
            workerTo.setNodeFrom(0);
        }
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeFrom == 0) {
                continue;
            }
            Node from = nodes[w.nodeFrom];
            Worker fromWorker = from.workers[w.fromIdx];
            fromWorker.setToIdx(0);
            fromWorker.setNodeTo(0);
        }
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            w.setFromBase();
        }
    }

    private void borrow(Node node, int cnt) {
        Node neighbour = getNeighbour(node);
        if (neighbour == null) {
            return;
        }
        if (node.isWiped()) {
            node.setWiped(false);
        }
        if (neighbour.isWiped()) {
            neighbour.setWiped(false);
        }
        int numBorrow = Math.min(node.workers.length, neighbour.workers.length);
        int n1 = pickBorrowFrom(node, numBorrow);
        int n2 = pickBorrowTo(neighbour, numBorrow);
        numBorrow = Math.min(n1, n2);
        for (int i = 0; i < numBorrow; i++) {
            Worker borrowFromWorker = node.workers[borrowFromWorkers[i]];
            if (borrowFromWorker.nodeTo != 0) {
                Node n = nodes[borrowFromWorker.nodeTo];
                Worker w = n.workers[borrowFromWorker.toIdx];
                w.setNodeFrom(0);
                w.setFromIdx(0);
                wipedTo[wipedToLen] = w;
                wipedToLen += 1;
            }
        }
        for (int i = 0; i < numBorrow; i++) {
            Worker borrowToWorker = neighbour.workers[borrowToWorkers[i]];
            if (borrowToWorker.nodeFrom != 0) {
                Node n = nodes[borrowToWorker.nodeFrom];
                Worker w = n.workers[borrowToWorker.fromIdx];
                w.setNodeTo(0);
                w.setToIdx(0);
                wipedFrom[wipedFromLen] = w;
                wipedFromLen += 1;
            }
        }
        for (int i = 0; i < numBorrow; i++) {
            Worker borrowFromWorker = node.workers[borrowFromWorkers[i]];
            Worker borrowToWorker = neighbour.workers[borrowToWorkers[i]];
            borrowFromWorker.setNodeTo(neighbour.id);
            borrowFromWorker.setToIdx(borrowToWorkers[i]);
            borrowToWorker.setNodeFrom(node.id);
            borrowToWorker.setFromIdx(borrowFromWorkers[i]);
        }
        connectWiped();
    }

    private void connectWiped() {
        for (int i = 0; i < wipedFromLen; i++) {
            Worker workerFrom = wipedFrom[i];
            if (workerFrom.nodeTo != 0) {
                continue;
            }
            Node nodeFrom = nodes[workerFrom.nodeId];
            for (int j = 0; j < wipedToLen; j++) {
                if (workerFrom.nodeTo != 0) {
                    break;
                }
                Worker workerTo = wipedTo[j];
                if (workerTo == null) {
                    continue;
                }
                if (workerTo.nodeFrom != 0) {
                    continue;
                }
                Node nodeTo = nodes[workerTo.nodeId];
                if (nodeTo.id == nodeFrom.id) {
                    continue;
                }
                int t = nodeFrom.getStartTime() + nodeFrom.duration + TaskUtils.dist(nodeFrom, nodeTo);
                if (t < nodeTo.getStartTime()) {
                    workerFrom.setNodeTo(nodeTo.id);
                    workerFrom.setToIdx(workerTo.getId());
                    workerTo.setNodeFrom(nodeFrom.id);
                    workerTo.setFromIdx(workerFrom.getId());
                    wipedTo[j] = null;
                    break;
                }
            }
        }
    }

    private int pickBorrowFrom(Node node, int num) {
        int free = countFree(node);
        if (free > 0) {
            int idx = 0;
            for (int i = 0; i < node.workers.length; i++) {
                Worker w = node.workers[i];
                if (w.nodeTo == 0) {
                    borrowFromWorkers[idx] = i;
                    idx += 1;
                }
            }
            return free;
        }
        for (int i = 0; i < 7; i++) {
            borrowFromWorkers[i] = i;
        }
        shuffleArray(borrowFromWorkers, node.workers.length);
        return num;
    }

    private int pickBorrowTo(Node node, int num) {
        int base = countBase(node);
        if (base > 0) {
            int idx = 0;
            for (int i = 0; i < node.workers.length; i++) {
                Worker w = node.workers[i];
                if (w.nodeFrom == 0) {
                    borrowToWorkers[idx] = i;
                    idx += 1;
                }
            }
            return base;
        }
        for (int i = 0; i < 7; i++) {
            borrowToWorkers[i] = i;
        }
        shuffleArray(borrowToWorkers, node.workers.length);
        return num;
    }

    private int countFree(Node node) {
        int cnt = 0;
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeTo == 0) {
                cnt += 1;
            }
        }
        return cnt;
    }

    private int countBase(Node node) {
        int cnt = 0;
        for (int i = 0; i < node.workers.length; i++) {
            Worker w = node.workers[i];
            if (w.nodeFrom == 0) {
                cnt += 1;
            }
        }
        return cnt;
    }

    private static void shuffleArray(int[] array, int len) {
        int index;
        Random random = TaskUtils.random;
        for (int i = len - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }

    private Node getNeighbour(Node node) {
        if (node.neighbours.length == 0) {
            return null;
        }
        for (int i = 0; i < 5; i++) {
            int n = TaskUtils.random.nextInt(node.neighbours.length);
            Node neighbour = nodes[node.neighbours[n]];
            int endTime = node.getStartTime() + node.duration;
            int dist = TaskUtils.dist(node, neighbour);
            if (endTime + dist <= neighbour.getStartTime()) {
                return neighbour;
            }
        }
        return null;
    }

    private boolean checkConsistent() {
        for (int ni = 1; ni < nodes.length; ni++) {
            Node node = nodes[ni];
            for (int i = 0; i < node.workers.length; i++) {
                Worker w = node.workers[i];
                if (w.nodeFrom != 0) {
                    Worker from = nodes[w.nodeFrom].workers[w.fromIdx];
                    if (from.nodeTo != node.id || from.toIdx != i) {
                        return false;
                    }
                }
                if (w.nodeTo != 0) {
                    Worker to = nodes[w.nodeTo].workers[w.toIdx];
                    if (to.nodeFrom != node.id || to.fromIdx != i) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void rollback() {
        for (int i = 0; i < TaskUtils.changedWorkersIdx; i++) {
            TaskUtils.changedWorkers[i].rollback();
        }
        for (int i = 0; i < TaskUtils.changeNodesIdx; i++) {
            TaskUtils.changedNodes[i].rollback();
        }
    }


}
