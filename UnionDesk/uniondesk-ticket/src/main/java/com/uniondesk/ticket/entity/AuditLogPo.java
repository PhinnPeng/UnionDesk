package com.uniondesk.ticket.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("audit_log")
public class AuditLogPo {

    @Id(keyType = KeyType.Auto)
    private long id;
    private long businessDomainId;
    private Long operatorSubjectId;
    private String operatorActorType;
    private String target;
    private String action;
    private String detail;
    private String result;
    private String requestId;
    /** 审计表时间列为 occurred_at（无 created_at 列），由数据库默认值处理 */
    @Column(ignore = true)
    private LocalDateTime createdAt;

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

    public Long getOperatorSubjectId() {
        return operatorSubjectId;
    }

    public void setOperatorSubjectId(Long operatorSubjectId) {
        this.operatorSubjectId = operatorSubjectId;
    }

    public String getOperatorActorType() {
        return operatorActorType;
    }

    public void setOperatorActorType(String operatorActorType) {
        this.operatorActorType = operatorActorType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
