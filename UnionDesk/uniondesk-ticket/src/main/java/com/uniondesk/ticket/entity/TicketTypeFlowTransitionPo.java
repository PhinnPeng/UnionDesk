package com.uniondesk.ticket.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("ticket_type_flow_transition")
public class TicketTypeFlowTransitionPo {

    public static final String PERMISSION_MODE_NONE = "none";
    public static final String PERMISSION_MODE_MEMBERS = "members";
    public static final String PERMISSION_MODE_ROLES = "roles";
    public static final String ANY_STATE_CODE = "*";

    @Id(keyType = KeyType.Auto)
    private long id;
    private long domainId;
    private long ticketTypeId;
    private String fromStateCode;
    private String toStateCode;
    private String stepName;
    private String permissionMode;
    @Column("member_ids")
    private String memberIdsJson;
    @Column("role_ids")
    private String roleIdsJson;
    @Column("required_slot_ids")
    private String requiredSlotIdsJson;
    @Column("attribute_updates")
    private String attributeUpdatesJson;
    @Column("additional_attributes")
    private String additionalAttributesJson;
    private int sortOrder;
    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime createdAt;
    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)", onUpdateValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public String getFromStateCode() {
        return fromStateCode;
    }

    public void setFromStateCode(String fromStateCode) {
        this.fromStateCode = fromStateCode;
    }

    public String getToStateCode() {
        return toStateCode;
    }

    public void setToStateCode(String toStateCode) {
        this.toStateCode = toStateCode;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getPermissionMode() {
        return permissionMode;
    }

    public void setPermissionMode(String permissionMode) {
        this.permissionMode = permissionMode;
    }

    public String getMemberIdsJson() {
        return memberIdsJson;
    }

    public void setMemberIdsJson(String memberIdsJson) {
        this.memberIdsJson = memberIdsJson;
    }

    public String getRoleIdsJson() {
        return roleIdsJson;
    }

    public void setRoleIdsJson(String roleIdsJson) {
        this.roleIdsJson = roleIdsJson;
    }

    public String getRequiredSlotIdsJson() {
        return requiredSlotIdsJson;
    }

    public void setRequiredSlotIdsJson(String requiredSlotIdsJson) {
        this.requiredSlotIdsJson = requiredSlotIdsJson;
    }

    public String getAttributeUpdatesJson() {
        return attributeUpdatesJson;
    }

    public void setAttributeUpdatesJson(String attributeUpdatesJson) {
        this.attributeUpdatesJson = attributeUpdatesJson;
    }

    public String getAdditionalAttributesJson() {
        return additionalAttributesJson;
    }

    public void setAdditionalAttributesJson(String additionalAttributesJson) {
        this.additionalAttributesJson = additionalAttributesJson;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
