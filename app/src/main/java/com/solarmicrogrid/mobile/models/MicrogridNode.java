package com.solarmicrogrid.mobile.models;

import java.io.Serializable;

public class MicrogridNode implements Serializable {

    private String id;
    private String nodeCode;
    private String name;
    private String region;
    private double totalCapacityKw;
    private double latitude;
    private double longitude;
    private int batterySlots;
    private String schedule;
    private String status;

    public MicrogridNode() {
    }

    public MicrogridNode(
            String id,
            String nodeCode,
            String name,
            String region,
            double totalCapacityKw
    ) {
        this.id = id;
        this.nodeCode = nodeCode;
        this.name = name;
        this.region = region;
        this.totalCapacityKw = totalCapacityKw;
    }

    public MicrogridNode(
            String id,
            String name,
            double latitude,
            double longitude,
            double totalCapacityKw,
            int batterySlots,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalCapacityKw = totalCapacityKw;
        this.batterySlots = batterySlots;
        this.status = status;
    }

    public MicrogridNode(
            String id,
            String name,
            double latitude,
            double longitude,
            double totalCapacityKw,
            int batterySlots,
            String schedule,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalCapacityKw = totalCapacityKw;
        this.batterySlots = batterySlots;
        this.schedule = schedule;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getNodeCode() {
        return nodeCode;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public double getTotalCapacityKw() {
        return totalCapacityKw;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getBatterySlots() {
        return batterySlots;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getStatus() {
        return status;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setTotalCapacityKw(double totalCapacityKw) {
        this.totalCapacityKw = totalCapacityKw;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setBatterySlots(int batterySlots) {
        this.batterySlots = batterySlots;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        if (nodeCode == null || nodeCode.trim().isEmpty()) {
            return name != null ? name : "Unnamed Node";
        }

        return (name != null ? name : "Unnamed Node")
                + " ("
                + nodeCode
                + ")";
    }
}