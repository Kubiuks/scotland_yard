package uk.ac.bris.cs.scotlandyard.ui.ai;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.bris.cs.gamekit.graph.*;
import uk.ac.bris.cs.scotlandyard.model.Transport;

/**
 * The implementation of Dijkstra Algorithm.
 *
 * "Dijkstra’s shortest path algorithm in Java - Tutorial" (2016).
 * Original code available online from: http://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 */

public class Dijkstra {

    private final List<Node> nodes;
    private final List<Edge> edges;
    private Set<Node> settledNodes;
    private Set<Node> unSettledNodes;
    private Map<Node, Node> predecessors;
    private Map<Node, Integer> distance;

    // Creates a copy of the array so that we can operate on this array
    public Dijkstra(Graph<Integer, Transport> graph) {

        this.nodes = new ArrayList<>(graph.getNodes());
        this.edges = new ArrayList<>(graph.getEdges());
    }


    public void execute (Node source) {

        this.settledNodes = new HashSet<>();
        this.unSettledNodes = new HashSet<>();
        this.distance = new HashMap<>();
        this.predecessors = new HashMap<>();
        this.distance.put(source, 0);
        this.unSettledNodes.add(source);

        while (unSettledNodes.size() > 0) {

            Node node = getMinimum(unSettledNodes);

            settledNodes.add(node);
            unSettledNodes.remove(node);

            findMinimalDistances(node);
        }
    }


    private void findMinimalDistances (Node node) {

        List<Node> adjacentNodes = getNeighbors(node);

        for (Node target : adjacentNodes) {

            if (getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {

                distance.put(target, getShortestDistance(node) + getDistance(node, target));
                predecessors.put(target, node);
                unSettledNodes.add(target);
            }
        }
    }


    private int getDistance (Node node, Node target) {

        for (Edge edge : edges) {

            if (edge.source().equals(node) && edge.destination().equals(target)) {
                return 1;
            }
        }
        throw new RuntimeException("Should not happen");
    }


    private List<Node> getNeighbors (Node node) {

        List<Node> neighbors = new ArrayList<>();

        for (Edge edge : edges) {

            if (edge.source().equals(node) && !isSettled(edge.destination())) {
                neighbors.add(edge.destination());
            }
        }
        return neighbors;
    }


    private Node getMinimum (Set<Node> nodes) {

        Node minimum = null;

        for (Node node : nodes) {

            if (minimum == null) {
                minimum = node;
            } else {
                if (getShortestDistance(node) < getShortestDistance(minimum)) {
                    minimum = node;
                }
            }
        }
        return minimum;
    }


    private boolean isSettled (Node node) {

        return settledNodes.contains(node);
    }

    private int getShortestDistance (Node destination) {

        Integer distance = this.distance.get(destination);

        if (distance == null) return Integer.MAX_VALUE;
        else return distance;
    }


    //This method returns the path from the source to the selected target and NULL if no path exists
    public LinkedList<Node> getPath (Node target) {

        LinkedList<Node> path = new LinkedList<>();
        Node step = target;

        // check if a path exists
        if (predecessors.get(step) == null) {
            return null;
        }
        path.add(step);

        while (predecessors.get(step) != null) {

            step = predecessors.get(step);
            path.add(step);
        }

        // Put it into the correct order
        Collections.reverse(path);
        return path;
    }

    public int getPathDistance (Node target) {

        int distance = 0;
        List<Node> path = getPath(target);

        if (!(path == null)) {
            distance = path.size();
        }
        return distance;
    }

}
