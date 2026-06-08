package com.uniondesk.sla.entity;

import java.time.LocalDateTime;

public class SlaRulePo {

    private Long id;
    private Long businessDomainId;
    private String name;
    private Long ticketTypeId;
    private Long priorityLevelId;
    private Long calendarId;
    private Integer firstResponseMinutes;
    private Integer resolutionMinutes;
    private Boolean isUrgentConfig;
    private String breachActionJson;
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

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public Long getPriorityLevelId() {
        return priorityLevelId;
    }

    public void setPriorityLevelId(Long priorityLevelId) {
        this.priorityLevelId = priorityLevelId;
    }

    public Long getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(Long calendarId) {
        this.calendarId = calendarId;
    }

    public Integer getFirstResponseMinutes() {
        return firstResponseMinutes;
    }

    public void setFirstResponseMinutes(Integer firstResponseMinutes) {
        this.firstResponseMinutes = firstResponseMinutes;
    }

    public Integer getResolutionMinutes() {
        return resolutionMinutes;
    }

    public void setResolutionMinutes(Integer resolutionMinutes) {
        this.resolutionMinutes = resolutionMinutes;
    }

    public Boolean getIsUrgentConfig() {
        return isUrgentConfig;
    }

    public void setIsUrgentConfig(Boolean isUrgentConfig) {
        this.isUrgentConfig = isUrgentConfig;
    }

    public String getBreachActionJson() {
        return breachActionJson;
    }

    public void setBreachActionJson(String breachActionJson) {
        this.breachActionJson = breachActionJson;
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
