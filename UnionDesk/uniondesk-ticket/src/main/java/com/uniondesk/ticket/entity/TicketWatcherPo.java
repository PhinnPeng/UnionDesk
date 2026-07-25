package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketWatcherPo {

    private long id;
    private long ticketId;
    private long staffAccountId;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public long getStaffAccountId() {
        return staffAccountId;
    }

    public void setStaffAccountId(long staffAccountId) {
        this.staffAccountId = staffAccountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
