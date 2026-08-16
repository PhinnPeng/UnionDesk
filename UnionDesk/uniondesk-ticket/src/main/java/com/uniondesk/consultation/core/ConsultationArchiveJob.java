package com.uniondesk.consultation.core;

import com.uniondesk.consultation.entity.ConsultationSessionPo;
import com.uniondesk.consultation.mapper.ConsultationMapper;
import com.uniondesk.consultation.repository.ConsultationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 咨询会话自动归档扫描：每小时按域配置（consultation_archive_auto_enabled/auto_days）归档
 * 已关闭满 N 天的会话。单实例部署无分布式锁（MVP）；单域失败隔离，不中断整批。
 */
@Component
public class ConsultationArchiveJob {

    private static final Logger log = LoggerFactory.getLogger(ConsultationArchiveJob.class);

    private static final int BATCH_LIMIT = 200;

    private final ConsultationRepository consultationRepository;
    private final Clock clock;

    public ConsultationArchiveJob(ConsultationRepository consultationRepository, Clock clock) {
        this.consultationRepository = consultationRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scanAutoArchive() {
        List<ConsultationMapper.AutoArchiveConfigRow> configs = consultationRepository.findAutoArchiveConfigs();
        if (configs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (ConsultationMapper.AutoArchiveConfigRow config : configs) {
            try {
                archiveDomain(config.domainId(), config.autoDays(), now);
            } catch (Exception ex) {
                log.warn("咨询自动归档失败（跳过该域）：domainId={}", config.domainId(), ex);
            }
        }
    }

    private void archiveDomain(long domainId, int autoDays, LocalDateTime now) {
        int effectiveDays = Math.max(autoDays, 1);
        LocalDateTime closedBefore = now.minusDays(effectiveDays);
        List<ConsultationSessionPo> candidates = consultationRepository.findAutoArchiveCandidates(domainId, closedBefore, BATCH_LIMIT);
        if (candidates.isEmpty()) {
            return;
        }
        log.info("咨询自动归档候选数：domainId={}, days={}, count={}", domainId, effectiveDays, candidates.size());
        for (ConsultationSessionPo candidate : candidates) {
            try {
                consultationRepository.updateArchived(candidate.getId(), now);
            } catch (Exception ex) {
                log.warn("咨询自动归档失败（跳过单条）：sessionId={}", candidate.getId(), ex);
            }
        }
    }
}
