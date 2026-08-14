package com.uniondesk.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.attachment.entity.FileAttachmentPo;
import com.uniondesk.attachment.mapper.FileAttachmentMapper;
import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import com.uniondesk.consultation.mapper.ConsultationMapper;
import com.uniondesk.consultation.mapper.ConsultationMessageMapper;
import com.uniondesk.notification.entity.InboxMessagePo;
import com.uniondesk.notification.entity.NotificationLogPo;
import com.uniondesk.notification.mapper.InboxMessageMapper;
import com.uniondesk.notification.mapper.NotificationLogMapper;
import com.uniondesk.ticket.entity.TicketPo;
import com.uniondesk.ticket.entity.TicketReplyPo;
import com.uniondesk.ticket.mapper.TicketMapper;
import com.uniondesk.ticket.mapper.TicketReplyMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AC4 集成冒烟：验证 snowFlakeId 主键切换后，7 个模型的插入 id 由 Flex 生成器回填
 * （19 位大数，非自增小整数），且外键引用链路正常；finally 逆序清理。
 */
@SpringBootTest
class SnowflakePrimaryKeyIntegrationTest {

    private static final long SNOWFLAKE_MIN = 1_000_000_000_000_000L;

    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private TicketReplyMapper ticketReplyMapper;
    @Autowired
    private InboxMessageMapper inboxMessageMapper;
    @Autowired
    private NotificationLogMapper notificationLogMapper;
    @Autowired
    private ConsultationMapper consultationMapper;
    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;
    @Autowired
    private FileAttachmentMapper fileAttachmentMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void snowflakeGeneratedIdsForSelectedModels() {
        Long domainId = jdbcTemplate.queryForObject("SELECT id FROM business_domain ORDER BY id LIMIT 1", Long.class);
        Long customerId = jdbcTemplate.queryForObject("SELECT id FROM customer_account ORDER BY id LIMIT 1", Long.class);
        Long ticketTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM ticket_type WHERE business_domain_id = ? ORDER BY id LIMIT 1", Long.class, domainId);
        Long subjectId = jdbcTemplate.queryForObject("SELECT id FROM identity_subject ORDER BY id LIMIT 1", Long.class);

        TicketPo ticket = new TicketPo();
        ticket.setTicketNo("snowflake-" + UUID.randomUUID().toString().substring(0, 8));
        ticket.setBusinessDomainId(domainId);
        ticket.setCustomerId(customerId);
        ticket.setTicketTypeId(ticketTypeId);
        ticket.setTitle("snowflake 冒烟");
        ticket.setPriority("normal");
        ticket.setSource("web");
        ticketMapper.insert(ticket);
        assertThat(ticket.getId()).isGreaterThan(SNOWFLAKE_MIN);

        TicketReplyPo reply = new TicketReplyPo();
        reply.setTicketId(ticket.getId());
        reply.setBusinessDomainId(domainId);
        reply.setSenderUserId(customerId);
        reply.setSenderRole("customer");
        reply.setSenderType("customer");
        reply.setReplyType("text");
        reply.setContent("snowflake 冒烟回复");
        reply.setCreatedAt(LocalDateTime.now());
        ticketReplyMapper.insert(reply);
        assertThat(reply.getId()).isGreaterThan(SNOWFLAKE_MIN);

        InboxMessagePo inbox = new InboxMessagePo();
        inbox.setRecipientSubjectId(subjectId);
        inbox.setPortalType("customer-web");
        inbox.setBusinessDomainId(domainId);
        inbox.setTitle("snowflake 冒烟");
        inbox.setContent("snowflake 冒烟");
        inboxMessageMapper.insert(inbox);
        assertThat(inbox.getId()).isGreaterThan(SNOWFLAKE_MIN);

        NotificationLogPo logPo = new NotificationLogPo();
        logPo.setBusinessDomainId(domainId);
        logPo.setSourceType("ticket");
        logPo.setSourceId(ticket.getId());
        logPo.setChannel("inbox");
        logPo.setRecipientSubjectId(subjectId);
        logPo.setPortalType("customer-web");
        logPo.setTemplateCode("ticket.created");
        logPo.setStatus("sent");
        notificationLogMapper.insert(logPo);
        assertThat(logPo.getId()).isGreaterThan(SNOWFLAKE_MIN);

        ConsultationSessionPo session = new ConsultationSessionPo();
        session.setSessionNo("ss-snowflake-" + UUID.randomUUID().toString().substring(0, 8));
        session.setBusinessDomainId(domainId);
        session.setCustomerId(customerId);
        session.setSessionStatus("open");
        consultationMapper.insertSession(session);
        assertThat(session.getId()).isGreaterThan(SNOWFLAKE_MIN);

        ConsultationMessagePo msg = new ConsultationMessagePo();
        msg.setConsultationSessionId(session.getId());
        msg.setBusinessDomainId(domainId);
        msg.setSeqNo(1);
        msg.setSenderUserId(customerId);
        msg.setSenderRole("customer");
        msg.setMessageType("text");
        msg.setContent("snowflake 冒烟");
        consultationMessageMapper.insert(msg);
        assertThat(msg.getId()).isGreaterThan(SNOWFLAKE_MIN);

        FileAttachmentPo att = new FileAttachmentPo();
        att.setBusinessDomainId(domainId);
        att.setUploaderSubjectId(subjectId);
        att.setPortalType("customer-web");
        att.setFileName("snowflake.txt");
        att.setMimeType("text/plain");
        att.setFileSize(1L);
        att.setStorageType("s3");
        att.setStorageKey("snowflake/" + UUID.randomUUID());
        att.setStatus("uploaded");
        att.setCreatedAt(LocalDateTime.now());
        fileAttachmentMapper.insert(att);
        assertThat(att.getId()).isGreaterThan(SNOWFLAKE_MIN);

        try {
            assertThat(jdbcTemplate.queryForObject("SELECT ticket_no FROM ticket WHERE id = ?", String.class, ticket.getId()))
                    .isEqualTo(ticket.getTicketNo());
            assertThat(jdbcTemplate.queryForObject("SELECT ticket_id FROM ticket_reply WHERE id = ?", Long.class, reply.getId()))
                    .isEqualTo(ticket.getId());
        } finally {
            consultationMessageMapper.deleteById(msg.getId());
            consultationMapper.deleteById(session.getId());
            inboxMessageMapper.deleteById(inbox.getId());
            notificationLogMapper.deleteById(logPo.getId());
            fileAttachmentMapper.deleteById(att.getId());
            ticketReplyMapper.deleteById(reply.getId());
            ticketMapper.deleteById(ticket.getId());
        }
    }
}
