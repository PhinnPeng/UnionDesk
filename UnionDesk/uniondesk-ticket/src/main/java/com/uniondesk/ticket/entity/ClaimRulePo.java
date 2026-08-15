package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class ClaimRulePo {

    private Long id;
    private Long businessDomainId;
    private String name;
    private Boolean enabled;
    private Long matchTicketTypeId;
    private Long matchPriorityLevelId;
    private String strategy;
    private Long assigneeStaffAccountId;
    private Integer graceMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(Long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getMatchTicketTypeId() {
        return matchTicketTypeId;
    }

    public void setMatchTicketTypeId(Long matchTicketTypeId) {
        this.matchTicketTypeId = matchTicketTypeId;
    }

    public Long getMatchPriorityLevelId() {
        return matchPriorityLevelId;
    }

    public void setMatchPriorityLevelId(Long matchPriorityLevelId) {
        this.matchPriorityLevelId = matchPriorityLevelId;
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

    public Integer getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(Integer graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
