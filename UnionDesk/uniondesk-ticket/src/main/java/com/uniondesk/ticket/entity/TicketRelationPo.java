package com.uniondesk.ticket.entity;

public class TicketRelationPo {

    private long id;
    private long sourceTicketId;
    private long targetTicketId;
    private String relationType;
    private long createdByStaffAccountId;
    private String note;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSourceTicketId() {
        return sourceTicketId;
    }

    public void setSourceTicketId(long sourceTicketId) {
        this.sourceTicketId = sourceTicketId;
    }

    public long getTargetTicketId() {
        return targetTicketId;
    }

    public void setTargetTicketId(long targetTicketId) {
        this.targetTicketId = targetTicketId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public long getCreatedByStaffAccountId() {
        return createdByStaffAccountId;
    }

    public void setCreatedByStaffAccountId(long createdByStaffAccountId) {
        this.createdByStaffAccountId = createdByStaffAccountId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
