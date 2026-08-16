package com.uniondesk.consultation.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsultationMapper extends BaseMapper<ConsultationSessionPo> {

    ConsultationSessionPo selectBySessionNoAndDomain(@Param("sessionNo") String sessionNo, @Param("domainId") long domainId);

    List<ConsultationSessionPo> selectPageByDomain(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("assignedToMe") Long assignedToMe,
            @Param("archived") Boolean archived,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomain(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("assignedToMe") Long assignedToMe,
            @Param("archived") Boolean archived);

    /**
     * 自动归档候选：已关闭、未归档、且关闭时间早于阈值（按域配置天数）的会话。
     */
    List<ConsultationSessionPo> selectAutoArchiveCandidates(
            @Param("domainId") long domainId,
            @Param("closedBefore") LocalDateTime closedBefore,
            @Param("limit") int limit);

    /**
     * 启用自动归档的域与天数（domain_config KV；未配置天数时默认 30）。
     */
    List<AutoArchiveConfigRow> selectAutoArchiveConfigs();

    record AutoArchiveConfigRow(long domainId, int autoDays) {
    }

    List<ConsultationSessionPo> selectByCustomerId(@Param("domainId") long domainId, @Param("customerId") long customerId);

    long nextSessionSequence(@Param("domainId") long domainId, @Param("prefix") String prefix);

    default void insertSession(ConsultationSessionPo po) {
        insert(po);
    }

    List<ConsultationMessagePo> selectMessagesBySession(@Param("sessionId") long sessionId);

    ConsultationMessagePo selectMessageByIdAndSession(@Param("messageId") long messageId, @Param("sessionId") long sessionId);

    int nextSeqNo(@Param("sessionId") long sessionId);

    int updateLastMessageAt(@Param("sessionId") long sessionId, @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 乐观接入：未分配会话（含排队态）写入 assigned_to 并置为 open；0 行表示已被他人接入或已结束。
     */
    int assignSessionIfUnassigned(@Param("sessionId") long sessionId, @Param("assignedTo") long assignedTo);

    /**
     * 按会话编号乐观接入（自动取队分配路径）：未分配且未结束才会成功，0 行表示已结束/已被接入，跳过。
     */
    int assignSessionByNoIfUnassigned(
            @Param("sessionNo") String sessionNo,
            @Param("domainId") long domainId,
            @Param("assignedTo") long assignedTo);

    /**
     * 候选客服（在线 auto 列表）中未完结咨询会话最少者；并列取最近会话久者（MAX(updated_at) 最小），再并列取 id 小。
     */
    Long selectLeastLoadedOnlineAssignee(@Param("domainId") long domainId, @Param("staffIds") List<Long> staffIds);

    int updateMessageRetracted(
            @Param("messageId") long messageId,
            @Param("retractedBy") long retractedBy,
            @Param("retractedAt") LocalDateTime retractedAt);

    int closeSession(@Param("sessionId") long sessionId, @Param("closedAt") LocalDateTime closedAt);

    /**
     * 归档/取消归档：仅影响 archived_at，状态不变；0 行表示会话不存在。
     */
    int updateArchived(@Param("sessionId") long sessionId, @Param("archivedAt") LocalDateTime archivedAt);

    String selectLinkedTicketNo(@Param("sessionId") long sessionId);

    void insertTicketLink(
            @Param("sessionId") long sessionId,
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId,
            @Param("convertedBy") long convertedBy);
}
