package com.uniondesk.ticket.core;

import com.uniondesk.domain.core.DomainTeamTemplateApplier;
import com.uniondesk.domain.repository.DomainRepository;
import com.uniondesk.ticket.entity.TicketTeamTemplateItemPo;
import com.uniondesk.ticket.entity.TicketTeamTemplatePo;
import com.uniondesk.ticket.repository.TicketTeamTemplateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamTemplateApplyService implements DomainTeamTemplateApplier {

    private final TicketTeamTemplateRepository templateRepository;
    private final PlatformTicketTypeCopyService platformTicketTypeCopyService;
    private final DomainRepository domainRepository;

    public TeamTemplateApplyService(
            TicketTeamTemplateRepository templateRepository,
            PlatformTicketTypeCopyService platformTicketTypeCopyService,
            DomainRepository domainRepository) {
        this.templateRepository = templateRepository;
        this.platformTicketTypeCopyService = platformTicketTypeCopyService;
        this.domainRepository = domainRepository;
    }

    @Override
    @Transactional
    public void applyOnDomainCreate(long domainId, long teamTemplateId, Long operatorId) {
        TicketTeamTemplatePo template = templateRepository.findRequiredById(teamTemplateId);
        if (!TicketTeamTemplatePo.STATUS_ACTIVE.equals(template.getStatus())) {
            throw new IllegalArgumentException("团队模板已停用，无法套用");
        }
        List<TicketTeamTemplateItemPo> items = templateRepository.findItemsByTemplateId(teamTemplateId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("团队模板未包含事项类型");
        }

        for (TicketTeamTemplateItemPo item : items) {
            platformTicketTypeCopyService.copyToDomain(
                    domainId,
                    item.getTicketTypeId(),
                    new PlatformTicketTypeCopyService.CopyOptions(
                            item.isIncludeFormSchema(),
                            item.isIncludeWorkflow(),
                            item.isIncludeDescriptionTemplate(),
                            item.getSortOrder()),
                    operatorId);
        }

        domainRepository.updateAppliedTeamTemplate(
                domainId,
                template.getId(),
                template.getVersion(),
                operatorId);
    }
}
