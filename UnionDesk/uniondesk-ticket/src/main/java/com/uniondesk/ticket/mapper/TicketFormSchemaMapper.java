package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketFormSchemaMapper {

    TicketFormSchemaPo findDraftByTicketTypeId(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId);

    TicketFormSchemaPo findLatestPublishedByTicketTypeId(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId);

    TicketFormSchemaPo findPublishedByVersionNo(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId,
            @Param("versionNo") int versionNo);

    List<TicketFormSchemaPo> listPublishedByTicketTypeId(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId,
            @Param("limit") int limit);

    Integer findMaxPublishedVersionNo(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId);

    void insert(TicketFormSchemaPo po);

    void updateDraftSchema(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId,
            @Param("formSchema") String formSchema);

    void updateDraftMaterialized(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId,
            @Param("formSchema") String formSchema,
            @Param("pluginRevision") String pluginRevision);

    int deleteByTicketTypeId(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId);

    int deletePublishedByVersionNo(@Param("ticketTypeId") long ticketTypeId,
            @Param("domainId") long domainId,
            @Param("versionNo") int versionNo);
}
