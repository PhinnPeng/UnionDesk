package com.uniondesk.sla.entity;

public class TicketSlaPolicyPo {

    private Integer firstResponseMinutes;
    private Integer resolutionMinutes;
    private String breachActionJson;

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
}
