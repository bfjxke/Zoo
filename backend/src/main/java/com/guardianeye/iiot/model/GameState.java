package com.guardianeye.iiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@Table(name = "game_state")
public class GameState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer currentTick = 0;

    @Column(nullable = false)
    private Boolean running = false;

    @Column(nullable = false)
    private LocalDateTime lastTickTime;

    @Column
    private String orderSwordLocation;

    @Column
    private Long orderSwordHolderId;

    @Column(nullable = false)
    private Boolean orderSwordSpawned = false;

    @Column(nullable = false)
    private Boolean orderDeclarationActive = false;

    @Column(nullable = false)
    private Integer lastDeclarationTick = 0;

    @Column(nullable = false)
    private Integer declarationCooldown = 10;

    @Column(columnDefinition = "TEXT")
    private String foodInventoryJson = "{}";

    @Column(columnDefinition = "TEXT")
    private String foodDropLocationsJson = "{}";

    @Transient
    private Map<String, Integer> foodInventoryCache;

    @Transient
    private Map<String, Integer> foodDropLocationsCache;

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getFoodInventory() {
        if (foodInventoryCache == null) {
            foodInventoryCache = new HashMap<>();
            if (foodInventoryJson != null && !foodInventoryJson.isEmpty()) {
                try {
                    String json = foodInventoryJson.replace("\"", "").replace("{", "").replace("}", "");
                    if (!json.isEmpty()) {
                        for (String entry : json.split(",")) {
                            String[] parts = entry.trim().split("=");
                            if (parts.length == 2) {
                                foodInventoryCache.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                            }
                        }
                    }
                } catch (Exception e) {
                    foodInventoryCache = new HashMap<>();
                }
            }
            if (foodInventoryCache.isEmpty()) {
                foodInventoryCache.put("lawful", 20);
                foodInventoryCache.put("aggressive", 20);
                foodInventoryCache.put("neutral", 20);
            }
        }
        return foodInventoryCache;
    }

    public void setFoodInventory(Map<String, Integer> inventory) {
        this.foodInventoryCache = inventory;
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sb.append("}");
        this.foodInventoryJson = sb.toString();
    }

    public int getFactionFood(String faction) {
        Integer amount = getFoodInventory().get(faction);
        return amount != null ? amount : 0;
    }

    public boolean consumeFactionFood(String faction, int amount) {
        Map<String, Integer> inventory = getFoodInventory();
        Integer current = inventory.get(faction);
        if (current == null || current < amount) {
            return false;
        }
        inventory.put(faction, current - amount);
        setFoodInventory(inventory);
        return true;
    }

    public void addFactionFood(String faction, int amount) {
        Map<String, Integer> inventory = getFoodInventory();
        Integer current = inventory.get(faction);
        inventory.put(faction, (current != null ? current : 0) + amount);
        setFoodInventory(inventory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getFoodDropLocations() {
        if (foodDropLocationsCache == null) {
            foodDropLocationsCache = new HashMap<>();
            if (foodDropLocationsJson != null && !foodDropLocationsJson.isEmpty()) {
                try {
                    String json = foodDropLocationsJson.replace("\"", "").replace("{", "").replace("}", "");
                    if (!json.isEmpty()) {
                        for (String entry : json.split(",")) {
                            String[] parts = entry.trim().split("=");
                            if (parts.length == 2) {
                                foodDropLocationsCache.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                            }
                        }
                    }
                } catch (Exception e) {
                    foodDropLocationsCache = new HashMap<>();
                }
            }
        }
        return foodDropLocationsCache;
    }

    public void setFoodDropLocations(Map<String, Integer> locations) {
        this.foodDropLocationsCache = locations;
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Integer> entry : locations.entrySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sb.append("}");
        this.foodDropLocationsJson = sb.toString();
    }

    public int getFoodAtNode(String nodeId) {
        Integer amount = getFoodDropLocations().get(nodeId);
        return amount != null ? amount : 0;
    }

    public void dropFood(String nodeId, int amount) {
        Map<String, Integer> locations = getFoodDropLocations();
        Integer current = locations.get(nodeId);
        locations.put(nodeId, (current != null ? current : 0) + amount);
        setFoodDropLocations(locations);
    }

    public boolean pickupFood(String nodeId, int amount) {
        Map<String, Integer> locations = getFoodDropLocations();
        Integer current = locations.get(nodeId);
        if (current == null || current < amount) {
            return false;
        }
        locations.put(nodeId, current - amount);
        setFoodDropLocations(locations);
        return true;
    }
}
