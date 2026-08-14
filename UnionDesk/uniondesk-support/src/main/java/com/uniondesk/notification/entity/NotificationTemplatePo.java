package com.uniondesk.notification.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("notification_template")
public class NotificationTemplatePo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String scopeType;
    private Long scopeId;
    private String eventCategory;
    private String channel;
    private String code;
    private String titleTemplate;
    private String contentTemplate;
    private Boolean isSecurity;
    private Boolean isUnsubscribable;
    private String status;
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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public void setContentTemplate(String contentTemplate) {
        this.contentTemplate = contentTemplate;
    }

    public Boolean getIsSecurity() {
        return isSecurity;
    }

    public void setIsSecurity(Boolean isSecurity) {
        this.isSecurity = isSecurity;
    }

    public Boolean getIsUnsubscribable() {
        return isUnsubscribable;
    }

    public void setIsUnsubscribable(Boolean isUnsubscribable) {
        this.isUnsubscribable = isUnsubscribable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
