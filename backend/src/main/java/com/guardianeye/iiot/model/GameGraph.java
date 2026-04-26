package com.guardianeye.iiot.model;

import lombok.Getter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Getter
public class GameGraph {
    
    private Map<String, GraphNode> nodes;
    private List<GraphEdge> edges;
    private Map<String, List<String>> adjacencyList;
    
    private static GameGraph instance;
    
    @PostConstruct
    public void init() {
        instance = this;
        initializeGraph();
    }
    
    public static GameGraph getInstance() {
        if (instance == null) {
            instance = new GameGraph();
            instance.initializeGraph();
        }
        return instance;
    }
    
    private void initializeGraph() {
        nodes = new HashMap<>();
        edges = new ArrayList<>();
        adjacencyList = new HashMap<>();
        
        createNodes();
        createEdges();
        buildAdjacencyList();
    }
    
    private void createNodes() {
        // 上层（顶部）3个节点
        nodes.put("A", new GraphNode("A", NodeType.BASE, "lawful", "守序", 100, 60));
        nodes.put("B", new GraphNode("B", NodeType.CENTER, "neutral", "中立", 400, 60));
        nodes.put("C", new GraphNode("C", NodeType.BASE, "aggressive", "激进", 700, 60));
        
        // 中层（中间）2个节点
        nodes.put("D", new GraphNode("D", NodeType.WILDERNESS, null, "广场", 200, 250));
        nodes.put("E", new GraphNode("E", NodeType.WILDERNESS, null, "广场", 600, 250));
        
        // 下层（底部）3个节点
        nodes.put("F", new GraphNode("F", NodeType.WILDERNESS, "lawful", "森林", 100, 440));
        nodes.put("G", new GraphNode("G", NodeType.CENTER, null, "河流", 400, 440));
        nodes.put("H", new GraphNode("H", NodeType.WILDERNESS, "aggressive", "山地", 700, 440));
    }
    
    private void createEdges() {
        // 节点A（守序）连接：D、F（直接连接森林）
        edges.add(new GraphEdge("A", "D"));
        edges.add(new GraphEdge("A", "F"));
        
        // 节点B（中立）连接：D、E、G（直接连接河流）
        edges.add(new GraphEdge("B", "D"));
        edges.add(new GraphEdge("B", "E"));
        edges.add(new GraphEdge("B", "G"));
        
        // 节点C（激进）连接：E、H（直接连接山地）
        edges.add(new GraphEdge("C", "E"));
        edges.add(new GraphEdge("C", "H"));
        
        // 节点D连接
        edges.add(new GraphEdge("D", "E")); // D和E（两个广场）直接连接
        edges.add(new GraphEdge("D", "F"));
        edges.add(new GraphEdge("D", "G"));
        
        // 节点E连接
        edges.add(new GraphEdge("E", "G"));
        edges.add(new GraphEdge("E", "H"));
        
        // 节点G连接H
        edges.add(new GraphEdge("G", "H"));
    }
    
    private void buildAdjacencyList() {
        for (GraphEdge edge : edges) {
            adjacencyList.computeIfAbsent(edge.getSourceId(), k -> new ArrayList<>()).add(edge.getTargetId());
            adjacencyList.computeIfAbsent(edge.getTargetId(), k -> new ArrayList<>()).add(edge.getSourceId());
        }
    }
    
    public GraphNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    public List<GraphNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    public List<String> getAdjacentNodes(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }
    
    public boolean areAdjacent(String node1, String node2) {
        List<String> adjacent = getAdjacentNodes(node1);
        return adjacent.contains(node2);
    }
    
    public List<GraphEdge> getEdges() {
        return new ArrayList<>(edges);
    }
    
    public List<GraphEdge> getEdgesForNode(String nodeId) {
        return edges.stream()
            .filter(edge -> edge.connects(nodeId))
            .collect(Collectors.toList());
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getEdgeCount() {
        return edges.size();
    }
    
    public Map<String, GraphNode> getNodesByFaction(String faction) {
        return nodes.values().stream()
            .filter(node -> faction.equals(node.getFaction()))
            .collect(Collectors.toMap(GraphNode::getId, node -> node));
    }
    
    public List<GraphNode> getBases() {
        return nodes.values().stream()
            .filter(node -> node.getType() == NodeType.BASE)
            .collect(Collectors.toList());
    }
    
    public List<GraphNode> getWildernesses() {
        return nodes.values().stream()
            .filter(node -> node.getType() == NodeType.WILDERNESS)
            .collect(Collectors.toList());
    }
    
    public GraphNode getCenter() {
        return nodes.get("G");
    }
    
    public String getNodeFaction(String nodeId) {
        GraphNode node = nodes.get(nodeId);
        return node != null ? node.getFaction() : null;
    }
    
    public NodeType getNodeType(String nodeId) {
        GraphNode node = nodes.get(nodeId);
        return node != null ? node.getType() : null;
    }
}
