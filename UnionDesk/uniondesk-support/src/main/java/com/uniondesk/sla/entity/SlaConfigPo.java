package com.uniondesk.sla.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("sla_config")
public class SlaConfigPo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long businessDomainId;
    private Integer firstResponseMinutes;
    private Integer resolutionMinutes;
    private String breachActionJson;
    private String calendarJson;
    private Integer urgentFirstResponseMinutes;
    private Integer urgentResolutionMinutes;
    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime createdAt;
    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)", onUpdateValue = "CURRENT_TIMESTAMP(3)")
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

    public String getBreachActionJson() {
        return breachActionJson;
    }

    public void setBreachActionJson(String breachActionJson) {
        this.breachActionJson = breachActionJson;
    }

    public String getCalendarJson() {
        return calendarJson;
    }

    public void setCalendarJson(String calendarJson) {
        this.calendarJson = calendarJson;
    }

    public Integer getUrgentFirstResponseMinutes() {
        return urgentFirstResponseMinutes;
    }

    public void setUrgentFirstResponseMinutes(Integer urgentFirstResponseMinutes) {
        this.urgentFirstResponseMinutes = urgentFirstResponseMinutes;
    }

    public Integer getUrgentResolutionMinutes() {
        return urgentResolutionMinutes;
    }

    public void setUrgentResolutionMinutes(Integer urgentResolutionMinutes) {
        this.urgentResolutionMinutes = urgentResolutionMinutes;
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
