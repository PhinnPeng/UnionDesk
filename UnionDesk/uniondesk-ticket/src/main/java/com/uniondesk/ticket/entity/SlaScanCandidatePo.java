package com.uniondesk.ticket.entity;

/**
 * SLA 定时扫描候选行（ticket 表最小投影）。
 */
public class SlaScanCandidatePo {

    private long id;
    private long businessDomainId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }
}
