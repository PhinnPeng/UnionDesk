package com.uniondesk.consultation.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import java.time.LocalDateTime;

@Table("consultation_message")
public class ConsultationMessagePo {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    private Long consultationSessionId;
    private Long businessDomainId;

    @Column(ignore = true)
    private String sessionNo;
    private Integer seqNo;
    private Long senderUserId;
    private String senderRole;
    private String messageType;
    private String content;

    @Column("payload")
    private String payloadJson;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime createdAt;

    private LocalDateTime retractedAt;
    private Long retractedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConsultationSessionId() {
        return consultationSessionId;
    }

    public void setConsultationSessionId(Long consultationSessionId) {
        this.consultationSessionId = consultationSessionId;
    }

    public Long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(Long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRetractedAt() {
        return retractedAt;
    }

    public void setRetractedAt(LocalDateTime retractedAt) {
        this.retractedAt = retractedAt;
    }

    public Long getRetractedBy() {
        return retractedBy;
    }

    public void setRetractedBy(Long retractedBy) {
        this.retractedBy = retractedBy;
    }
}
