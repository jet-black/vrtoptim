public class Worker {

    private int step;
    public int nodeId;
    public int nodeFrom = 0;
    public int nodeTo = 0;
    public int fromIdx = 0;
    public int toIdx = 0;
    private Worker prev;
    private int id;

    public Worker() {

    }

    public Worker(int nodeId, int nodeFrom, int nodeTo, int fromIdx, int toIdx) {
        this.nodeId = nodeId;
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
        this.fromIdx = fromIdx;
        this.toIdx = toIdx;
        this.prev = new Worker();
        prev.nodeId = nodeId;
        prev.nodeFrom = nodeFrom;
        prev.nodeTo = nodeTo;
        prev.fromIdx = fromIdx;
        prev.toIdx = toIdx;
    }

    public void rollback() {
        nodeFrom = prev.nodeFrom;
        nodeTo = prev.nodeTo;
        fromIdx = prev.fromIdx;
        toIdx = prev.toIdx;
    }

    public void setFromBase() {
        backup();
        nodeFrom = 0;
        nodeTo = 0;
        toIdx = 0;
        fromIdx = 0;
    }

    public void setNodeFrom(int nodeFrom) {
        backup();
        this.nodeFrom = nodeFrom;
    }

    public void setNodeTo(int nodeTo) {
        backup();
        this.nodeTo = nodeTo;
    }

    public void setFromIdx(int fromIdx) {
        backup();
        this.fromIdx = fromIdx;
    }

    public void setToIdx(int toIdx) {
        backup();
        this.toIdx = toIdx;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void backup() {
        if (prev.step == TaskUtils.step) {
            return;
        }
        Node node = TaskUtils.world.nodes[nodeId];
        node.backup();
        TaskUtils.changeWorker(this);
        prev.step = TaskUtils.step;
        prev.nodeFrom = nodeFrom;
        prev.nodeTo = nodeTo;
        prev.fromIdx = fromIdx;
        prev.toIdx = toIdx;
    }


}
