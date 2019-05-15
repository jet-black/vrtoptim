public class Node {

    Worker[] workers;
    public int minBeginTime;
    public int maxEndTime;
    int duration;
    public int[] neighbours;
    int x;
    int y;
    int distToBase;
    int id;

    NodeState cur = new NodeState();
    NodeState prev = new NodeState();

    public void rollback() {
        cur.time = prev.time;
        cur.wiped = prev.wiped;
    }

    public boolean isWiped() {
        return cur.wiped;
    }

    public void setWiped(boolean wiped) {
        backup();
        cur.wiped = wiped;
    }

    public int getStartTime() {
        return cur.time;
    }

    public void setStartTime(int startTime) {
        backup();
        cur.time = startTime;
    }

    public void setStartTimeUnsafe(int startTime) {
        cur.time = startTime;
        prev.time = startTime;
    }

    public int getScore() {
        int result = 0;
        if (cur.wiped) {
            return 0;
        }
        for (int i = 0; i < workers.length; i++) {
            Worker w = workers[i];
            if (w.nodeFrom == 0) {
                result -= 240;
                result -= distToBase;
            } else {
                Node n = TaskUtils.world.nodes[w.nodeFrom];
                int diff = getStartTime() - n.getStartTime() - n.duration;
                if (diff < 0) {
                    System.out.println("wrong!!");
                }
                result -= diff;
            }
            result -= duration;
            if (w.nodeTo == 0) {
                result -= distToBase;
            }
        }
        result += duration * workers.length * (workers.length + 5);
        return result;
    }

    public void backup() {
        if (prev.step == TaskUtils.step) {
            return;
        }
        TaskUtils.changeNode(this);
        prev.wiped = cur.wiped;
        prev.time = cur.time;
        prev.step = TaskUtils.step;
    }

    public boolean isFromBase() {
        for (int i = 0; i < workers.length; i++) {
            if (workers[i].nodeFrom != 0) {
                return false;
            }
            if (workers[i].nodeTo != 0) {
                return false;
            }
        }
        return true;
    }

    static class NodeState {
        int step;
        boolean wiped;
        int time;
    }

}
