package com.solarmicrogrid.mobile.models;

import java.io.Serializable;

/**
 * Microgrid Hub Node Model.
 */
public class MicrogridNode implements Serializable {
    private String id;
    private String nodeCode;
    private String name;
    private String region;
    private double totalCapacityKw;

    public MicrogridNode(String id, String nodeCode, String name, String region, double totalCapacityKw) {
        this.id = id;
        this.nodeCode = nodeCode;
        this.name = name;
        this.region = region;
        this.totalCapacityKw = totalCapacityKw;
    }

    public String getId() { return id; }
    public String getNodeCode() { return nodeCode; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public double getTotalCapacityKw() { return totalCapacityKw; }

    @Override
    public String toString() {
        return name + " (" + nodeCode + ")";
    }
}
