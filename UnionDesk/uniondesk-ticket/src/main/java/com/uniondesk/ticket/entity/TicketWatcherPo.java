package com.uniondesk.ticket.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("ticket_watcher")
public class TicketWatcherPo {

    @Id(keyType = KeyType.Auto)
    private long id;
    private long ticketId;
    private long staffAccountId;
    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
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
