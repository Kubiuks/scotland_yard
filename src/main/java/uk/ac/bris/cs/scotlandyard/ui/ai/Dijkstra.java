package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.model.Transport;

import java.util.*;

public class Dijkstra {
    private final List<Node> xNodes;
    private final List<Edge> xEdges;
    private Set<Node> xVisitedNodes;
    private Set<Node> xUnVisitedNodes;
    private Map<Node, Node> xParentNodes;
    private Map<Node, Integer> xDistance;


    public Dijkstra (Graph<Integer, Transport> graph) {
        this.xNodes = new ArrayList<>(graph.getNodes());
        this.xEdges = new ArrayList<>(graph.getEdges());
    }


    private int getxDistance (Node node, Node target) {
        for (Edge edge : xEdges) {
            if (edge.source().equals(node) && edge.destination().equals(target)) {
                return 1;
            }
        }
        throw new RuntimeException("Impossible!");
    }

    private int getShortestxDistance (Node destination) {
        Integer distance = this.xDistance.get(destination);
        if (distance == null) return Integer.MAX_VALUE;
        else return distance;
    }

    private boolean isXVisited (Node node) {
        return xVisitedNodes.contains(node);
    }

    private List<Node> getAdjacentNodes (Node node) {

        List<Node> xAdjacentNodes = new ArrayList<>();
        for (Edge edge : xEdges) {
            if (edge.source().equals(node) && !isXVisited(edge.destination())) {
                xAdjacentNodes.add(edge.destination());
            }
        }
        return xAdjacentNodes;
    }

    private void findMinimalxDistances (Node node) {
        List<Node> adjacentNodes = getAdjacentNodes(node);
        for (Node target : adjacentNodes) {
            if (getShortestxDistance(target) > getShortestxDistance(node) + getxDistance(node, target)) {
                xDistance.put(target, getShortestxDistance(node) + getxDistance(node, target));
                xParentNodes.put(target, node);
                xUnVisitedNodes.add(target);
            }
        }
    }

    private Node getXMinimum (Set<Node> nodes) {
        Node min = null;

        for (Node node : nodes) {
            if (min == null) {
                min = node;
            } else {
                if (getShortestxDistance(node) < getShortestxDistance(min)) {
                    min = node;
                }
            }
        }
        return min;
    }

    public LinkedList<Node> getPath (Node target) {

        LinkedList<Node> path = new LinkedList<>();
        Node step = target;

        if (xParentNodes.get(step) == null) {
            return null;
        }
        path.add(step);

        while (xParentNodes.get(step) != null) {
            step = xParentNodes.get(step);
            path.add(step);
        }

        Collections.reverse(path);
        return path;
    }

//    public int getPathxDistance (Node target) {
//        int distance = 0;
//        List<Node> path = getPath(target);
//        if (!(path == null)){distance = path.size();} return distance;
//    }
    
    public void execute (Node source) {

        this.xVisitedNodes = new HashSet<>();
        this.xUnVisitedNodes = new HashSet<>();
        this.xUnVisitedNodes.add(source);
        this.xDistance = new HashMap<>();
        this.xDistance.put(source, 0);
        this.xParentNodes= new HashMap<>();

        while (xUnVisitedNodes.size() > 0) {
            Node node = getXMinimum(xUnVisitedNodes);
            xVisitedNodes.add(node);
            xUnVisitedNodes.remove(node);
            findMinimalxDistances(node);
        }
    }

}
