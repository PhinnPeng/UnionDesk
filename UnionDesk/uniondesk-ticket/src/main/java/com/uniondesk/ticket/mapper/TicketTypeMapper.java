package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeMapper {

    List<TicketTypePo> findByDomainId(@Param("domainId") long domainId);

    TicketTypePo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    TicketTypePo findByDomainIdAndCode(@Param("domainId") long domainId, @Param("code") String code);

    TicketTypePo findByDomainIdAndName(@Param("domainId") long domainId, @Param("name") String name);

    void insert(TicketTypePo po);

    void updateMetadata(@Param("id") long id,
                        @Param("domainId") long domainId,
                        @Param("name") String name,
                        @Param("description") String description,
                        @Param("icon") String icon,
                        @Param("statusFlowConfig") String statusFlowConfig,
                        @Param("status") String status);

    void updateFormSchemaDraft(@Param("id") long id,
                               @Param("domainId") long domainId,
                               @Param("formSchemaDraft") String formSchemaDraft);

    void publishFormSchema(@Param("id") long id,
                           @Param("domainId") long domainId,
                           @Param("formSchema") String formSchema,
                           @Param("formSchemaDraft") String formSchemaDraft);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    Long findFirstIdByDomainId(@Param("domainId") long domainId);

    int countTicketsByTypeId(@Param("domainId") long domainId, @Param("typeId") long typeId);
}
