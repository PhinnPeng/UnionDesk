package com.uniondesk.sla.entity;

import java.time.LocalDateTime;

public class SlaTicketPo {

    private String priority;
    private String slaStatus;
    private LocalDateTime slaFirstResponseDeadline;
    private LocalDateTime slaResolutionDeadline;
    private LocalDateTime slaFirstRespondedAt;
    private LocalDateTime slaResolvedAt;
    private String breachActionJson;

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public LocalDateTime getSlaFirstResponseDeadline() {
        return slaFirstResponseDeadline;
    }

    public void setSlaFirstResponseDeadline(LocalDateTime slaFirstResponseDeadline) {
        this.slaFirstResponseDeadline = slaFirstResponseDeadline;
    }

    public LocalDateTime getSlaResolutionDeadline() {
        return slaResolutionDeadline;
    }

    public void setSlaResolutionDeadline(LocalDateTime slaResolutionDeadline) {
        this.slaResolutionDeadline = slaResolutionDeadline;
    }

    public LocalDateTime getSlaFirstRespondedAt() {
        return slaFirstRespondedAt;
    }

    public void setSlaFirstRespondedAt(LocalDateTime slaFirstRespondedAt) {
        this.slaFirstRespondedAt = slaFirstRespondedAt;
    }

    public LocalDateTime getSlaResolvedAt() {
        return slaResolvedAt;
    }

    public void setSlaResolvedAt(LocalDateTime slaResolvedAt) {
        this.slaResolvedAt = slaResolvedAt;
    }

    public String getBreachActionJson() {
        return breachActionJson;
    }

    public void setBreachActionJson(String breachActionJson) {
        this.breachActionJson = breachActionJson;
    }
}
