package com.uniondesk.ticket.entity;

public class ClaimRulePolicyPo {

    private Long id;
    private String strategy;
    private Long assigneeStaffAccountId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Long getAssigneeStaffAccountId() {
        return assigneeStaffAccountId;
    }

    public void setAssigneeStaffAccountId(Long assigneeStaffAccountId) {
        this.assigneeStaffAccountId = assigneeStaffAccountId;
    }
}
