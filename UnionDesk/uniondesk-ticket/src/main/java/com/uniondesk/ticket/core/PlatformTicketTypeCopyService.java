package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketAttributeRepository;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 将平台事项类型深拷贝到业务域（快照，写 source_global_type_id）。
 */
@Service
public class PlatformTicketTypeCopyService {

    public record CopyOptions(
            boolean includeFormSchema,
            boolean includeWorkflow,
            boolean includeDescriptionTemplate,
            Integer sortOrder) {

        public static CopyOptions allIncluded() {
            return new CopyOptions(true, true, true, null);
        }
    }

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTypeAttributeSlotRepository slotRepository;
    private final TicketAttributeRepository ticketAttributeRepository;
    private final TicketFormSchemaService ticketFormSchemaService;
    private final TicketFormSchemaRepository ticketFormSchemaRepository;
    private final TicketTypeFlowService ticketTypeFlowService;
    private final ObjectMapper objectMapper;

    public PlatformTicketTypeCopyService(
            TicketTypeRepository ticketTypeRepository,
            TicketTypeAttributeSlotRepository slotRepository,
            TicketAttributeRepository ticketAttributeRepository,
            TicketFormSchemaService ticketFormSchemaService,
            TicketFormSchemaRepository ticketFormSchemaRepository,
            TicketTypeFlowService ticketTypeFlowService,
            ObjectMapper objectMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.slotRepository = slotRepository;
        this.ticketAttributeRepository = ticketAttributeRepository;
        this.ticketFormSchemaService = ticketFormSchemaService;
        this.ticketFormSchemaRepository = ticketFormSchemaRepository;
        this.ticketTypeFlowService = ticketTypeFlowService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TicketTypePo copyToDomain(long domainId, long platformTypeId, CopyOptions options, Long operatorId) {
        CopyOptions resolved = options == null ? CopyOptions.allIncluded() : options;
        if (ticketTypeRepository.findByDomainIdAndSourceGlobalTypeId(domainId, platformTypeId) != null) {
            throw new IllegalArgumentException("该平台事项类型已添加到当前域");
        }
        TicketTypePo platformType = ticketTypeRepository.findRequiredPlatformById(platformTypeId);
        TicketTypePo domainType = new TicketTypePo();
        domainType.setScope(TicketTypePo.SCOPE_DOMAIN);
        domainType.setBusinessDomainId(domainId);
        domainType.setCode(resolveUniqueCode(domainId, platformType.getCode()));
        domainType.setShortCode(resolveUniqueShortCode(domainId, platformType));
        domainType.setName(resolveUniqueName(domainId, platformType.getName()));
        domainType.setDescription(platformType.getDescription());
        domainType.setDescriptionTemplateMd(
                resolved.includeDescriptionTemplate() ? platformType.getDescriptionTemplateMd() : null);
        domainType.setIcon(platformType.getIcon());
        domainType.setCategory(platformType.getCategory());
        domainType.setStatus(TicketTypePo.STATUS_ACTIVE);
        domainType.setSortOrder(
                resolved.sortOrder() != null ? resolved.sortOrder() : ticketTypeRepository.nextSortOrderDomain(domainId));
        domainType.setSystem(false);
        domainType.setSourceGlobalTypeId(platformType.getId());
        try {
            ticketTypeRepository.save(domainType);
        }
        catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("事项类型编码或名称冲突: " + platformType.getCode());
        }

        Map<String, String> slotIdRemap = copySlots(domainId, platformType.getId(), domainType.getId(), operatorId);
        String formSchemaJson = resolveFormSchemaJson(platformType.getId(), resolved.includeFormSchema());
        ticketFormSchemaService.initializeForNewType(domainId, domainType.getId(), formSchemaJson);

        if (resolved.includeWorkflow()) {
            TicketConfigDtos.WorkflowConfigView workflow = ticketTypeFlowService.loadAssembled(0L, platformType.getId());
            List<TicketConfigDtos.SaveTransitionRuleRequest> rules = remapRules(workflow.transition_rules(), slotIdRemap);
            ticketTypeFlowService.replaceAll(domainId, domainType.getId(), workflow.status_flow(), rules);
        }
        else {
            ticketTypeFlowService.replaceAll(domainId, domainType.getId(), DefaultStatusFlowProvider.emptyFlow(), null);
        }
        return domainType;
    }

    private Map<String, String> copySlots(long domainId, long platformTypeId, long domainTypeId, Long operatorId) {
        Map<String, String> remap = new HashMap<>();
        for (TicketTypeAttributeSlotPo source : slotRepository.findByTicketTypeId(platformTypeId)) {
            TicketAttributePo platformAttr = ticketAttributeRepository.findRequiredById(source.getAttributeId());
            TicketAttributePo domainAttr = ensureDomainAttribute(domainId, platformAttr, operatorId);
            TicketTypeAttributeSlotPo target = new TicketTypeAttributeSlotPo();
            target.setTicketTypeId(domainTypeId);
            target.setAttributeId(domainAttr.getId());
            target.setSortOrder(source.getSortOrder());
            target.setSlotConfig(source.getSlotConfig());
            target.setStatus(source.getStatus());
            target.setCreatedBy(operatorId);
            target.setUpdatedBy(operatorId);
            slotRepository.insert(target);
            remap.put(String.valueOf(source.getId()), String.valueOf(target.getId()));
        }
        return remap;
    }

    /**
     * 确保平台属性在域内有对应字典项（按 source_attribute_id / system_key / name 复用，否则深拷贝）。
     */
    public TicketAttributePo ensureDomainAttribute(long domainId, TicketAttributePo platformAttr, Long operatorId) {
        TicketAttributePo bySource = ticketAttributeRepository.findDomainBySourceAttributeId(domainId, platformAttr.getId());
        if (bySource != null) {
            return bySource;
        }
        if (StringUtils.hasText(platformAttr.getSystemKey())) {
            TicketAttributePo byKey = ticketAttributeRepository.findDomainBySystemKey(domainId, platformAttr.getSystemKey());
            if (byKey != null) {
                return byKey;
            }
        }
        TicketAttributePo byName = ticketAttributeRepository.findDomainByName(domainId, platformAttr.getName());
        if (byName != null) {
            return byName;
        }
        TicketAttributePo target = new TicketAttributePo();
        target.setScope(TicketAttributePo.SCOPE_DOMAIN);
        target.setBusinessDomainId(domainId);
        target.setName(platformAttr.getName());
        target.setDescription(platformAttr.getDescription());
        target.setFieldType(platformAttr.getFieldType());
        target.setTypeConfig(platformAttr.getTypeConfig());
        target.setStatus(platformAttr.getStatus());
        target.setSortOrder(ticketAttributeRepository.nextSortOrderDomain(domainId));
        target.setSystem(platformAttr.isSystem());
        target.setSystemKey(platformAttr.getSystemKey());
        target.setSourceAttributeId(platformAttr.getId());
        target.setCreatedBy(operatorId);
        target.setUpdatedBy(operatorId);
        try {
            ticketAttributeRepository.insert(target);
        }
        catch (DuplicateKeyException ex) {
            TicketAttributePo bySourceAgain = ticketAttributeRepository.findDomainBySourceAttributeId(
                    domainId, platformAttr.getId());
            if (bySourceAgain != null) {
                return bySourceAgain;
            }
            if (StringUtils.hasText(platformAttr.getSystemKey())) {
                TicketAttributePo byKeyAgain = ticketAttributeRepository.findDomainBySystemKey(
                        domainId, platformAttr.getSystemKey());
                if (byKeyAgain != null) {
                    return byKeyAgain;
                }
            }
            TicketAttributePo byNameAgain = ticketAttributeRepository.findDomainByName(domainId, platformAttr.getName());
            if (byNameAgain != null) {
                return byNameAgain;
            }
            String detail = ex.getMostSpecificCause() == null ? null : ex.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("uk_ticket_attribute_system_key")) {
                throw new IllegalArgumentException(
                        "系统属性键冲突（" + platformAttr.getSystemKey() + "），无法落入域内，请检查 system_key 唯一约束是否已按域作用域修复",
                        ex);
            }
            if (detail != null && detail.contains("uk_ticket_attribute_scope_domain_name")) {
                throw new IllegalArgumentException("域内属性名称冲突: " + platformAttr.getName(), ex);
            }
            throw new IllegalArgumentException(
                    "域内属性写入冲突: " + platformAttr.getName()
                            + (detail == null ? "" : (" (" + detail + ")")),
                    ex);
        }
        return target;
    }

    private String resolveFormSchemaJson(long platformTypeId, boolean includeFormSchema) {
        if (!includeFormSchema) {
            return toJson(FormSchemaValidator.mergeAndValidate(null, objectMapper));
        }
        TicketFormSchemaPo published = ticketFormSchemaRepository.findLatestPublished(
                platformTypeId, TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY);
        if (published != null && StringUtils.hasText(published.getFormSchema())) {
            return published.getFormSchema();
        }
        return toJson(FormSchemaValidator.mergeAndValidate(null, objectMapper));
    }

    private List<TicketConfigDtos.SaveTransitionRuleRequest> remapRules(
            List<TicketConfigDtos.TransitionRuleView> rules,
            Map<String, String> slotIdRemap) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        List<TicketConfigDtos.SaveTransitionRuleRequest> result = new ArrayList<>();
        for (TicketConfigDtos.TransitionRuleView rule : rules) {
            result.add(new TicketConfigDtos.SaveTransitionRuleRequest(
                    rule.from_state_code(),
                    rule.to_state_code(),
                    rule.step_name(),
                    rule.permission_mode(),
                    List.of(),
                    List.of(),
                    remapSlotIdList(rule.required_slot_ids(), slotIdRemap),
                    remapAttributeUpdates(rule.attribute_updates(), slotIdRemap),
                    remapAdditionalAttributes(rule.additional_attributes(), slotIdRemap)));
        }
        return result;
    }

    private List<String> remapSlotIdList(List<String> slotIds, Map<String, String> slotIdRemap) {
        if (slotIds == null || slotIds.isEmpty()) {
            return List.of();
        }
        List<String> remapped = new ArrayList<>();
        for (String slotId : slotIds) {
            if (!StringUtils.hasText(slotId)) {
                continue;
            }
            remapped.add(slotIdRemap.getOrDefault(slotId, slotId));
        }
        return remapped;
    }

    private List<TicketConfigDtos.AttributeUpdateItemRequest> remapAttributeUpdates(
            List<TicketConfigDtos.AttributeUpdateItemView> updates,
            Map<String, String> slotIdRemap) {
        if (updates == null || updates.isEmpty()) {
            return List.of();
        }
        List<TicketConfigDtos.AttributeUpdateItemRequest> result = new ArrayList<>();
        for (TicketConfigDtos.AttributeUpdateItemView update : updates) {
            String slotId = update.slot_id();
            if (StringUtils.hasText(slotId)) {
                slotId = slotIdRemap.getOrDefault(slotId, slotId);
            }
            result.add(new TicketConfigDtos.AttributeUpdateItemRequest(
                    slotId,
                    update.value(),
                    update.value_type()));
        }
        return result;
    }

    private List<TicketConfigDtos.AdditionalAttributeItemRequest> remapAdditionalAttributes(
            List<TicketConfigDtos.AdditionalAttributeItemView> items,
            Map<String, String> slotIdRemap) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<TicketConfigDtos.AdditionalAttributeItemRequest> result = new ArrayList<>();
        for (TicketConfigDtos.AdditionalAttributeItemView item : items) {
            String slotId = item.slot_id();
            if (StringUtils.hasText(slotId)) {
                slotId = slotIdRemap.getOrDefault(slotId, slotId);
            }
            result.add(new TicketConfigDtos.AdditionalAttributeItemRequest(
                    slotId,
                    item.required(),
                    item.default_mode(),
                    item.default_value()));
        }
        return result;
    }

    private String resolveUniqueCode(long domainId, String baseCode) {
        String code = StringUtils.hasText(baseCode) ? baseCode.trim() : "type";
        if (ticketTypeRepository.findByDomainIdAndCode(domainId, code) == null) {
            return code;
        }
        for (int i = 1; i <= 50; i++) {
            String candidate = code + "_" + i;
            if (ticketTypeRepository.findByDomainIdAndCode(domainId, candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("无法生成唯一事项类型编码: " + code);
    }

    /**
     * 解析并确保短码在目标域内唯一：优先平台短码，为空兜底 code 前 2 位大写；冲突时追加数字后缀。
     */
    private String resolveUniqueShortCode(long domainId, TicketTypePo platformType) {
        String code = StringUtils.hasText(platformType.getCode()) ? platformType.getCode().trim() : "";
        String base = StringUtils.hasText(platformType.getShortCode())
                ? platformType.getShortCode().trim().toUpperCase(Locale.ROOT)
                : (code.length() >= 2 ? code.substring(0, 2).toUpperCase(Locale.ROOT) : code.toUpperCase(Locale.ROOT));
        if (!StringUtils.hasText(base)) {
            base = "TK";
        }
        if (ticketTypeRepository.findByDomainIdAndShortCode(domainId, base) == null) {
            return base;
        }
        for (int i = 1; i <= 50; i++) {
            String candidate = base + i;
            if (ticketTypeRepository.findByDomainIdAndShortCode(domainId, candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("无法生成唯一事项类型短码: " + base);
    }

    private String resolveUniqueName(long domainId, String baseName) {
        String name = StringUtils.hasText(baseName) ? baseName.trim() : "事项类型";
        if (ticketTypeRepository.findByDomainIdAndName(domainId, name) == null) {
            return name;
        }
        for (int i = 1; i <= 50; i++) {
            String candidate = name + " (" + i + ")";
            if (ticketTypeRepository.findByDomainIdAndName(domainId, candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("无法生成唯一事项类型名称: " + name);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize form schema failed", ex);
        }
    }
}
