package com.solarmicrogrid.mobile.models;

import java.io.Serializable;

/**
 * Domain entity model representing an Energy Slot Trading Reservation.
 * Author: Member 4 (Energy Reservation & QR Dispatch)
 */
public class Reservation implements Serializable {
    private String id;
    private String prosumerId;
    private String prosumerNic;
    private String nodeId;
    private String nodeName;
    private double reservedEnergyKwh;
    private double totalPrice;
    private String reservationDate;
    private String startTime;
    private String endTime;
    private String status;
    private String qrCodePayload;
    private String createdAt;

    public Reservation() {}

    public Reservation(String id, String prosumerId, String prosumerNic, String nodeId,
                       double reservedEnergyKwh, String startTime, String endTime, String status) {
        this.id = id;
        this.prosumerId = prosumerId;
        this.prosumerNic = prosumerNic;
        this.nodeId = nodeId;
        this.reservedEnergyKwh = reservedEnergyKwh;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProsumerId() { return prosumerId; }
    public void setProsumerId(String prosumerId) { this.prosumerId = prosumerId; }

    public String getProsumerNic() { return prosumerNic != null ? prosumerNic : prosumerId; }
    public void setProsumerNic(String prosumerNic) { this.prosumerNic = prosumerNic; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public double getReservedEnergyKwh() { return reservedEnergyKwh; }
    public void setReservedEnergyKwh(double reservedEnergyKwh) { this.reservedEnergyKwh = reservedEnergyKwh; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getReservationDate() { return reservationDate != null ? reservationDate : startTime; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status != null ? status : "Pending"; }
    public void setStatus(String status) { this.status = status; }

    public String getQrPayload() { return qrCodePayload; }
    public void setQrPayload(String qrPayload) { this.qrCodePayload = qrPayload; }

    public String getQrCodePayload() { return qrCodePayload; }
    public void setQrCodePayload(String qrCodePayload) { this.qrCodePayload = qrCodePayload; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
