package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeAttributeSlotMapper {

    List<TicketTypeAttributeSlotPo> findByTicketTypeId(@Param("ticketTypeId") long ticketTypeId);

    TicketTypeAttributeSlotPo findById(@Param("id") long id);

    TicketTypeAttributeSlotPo findByTypeAndAttribute(
            @Param("ticketTypeId") long ticketTypeId,
            @Param("attributeId") long attributeId);

    Integer findMaxSortOrder(@Param("ticketTypeId") long ticketTypeId);

    void insert(TicketTypeAttributeSlotPo po);

    int updateSlotConfig(
            @Param("id") long id,
            @Param("slotConfig") String slotConfig,
            @Param("updatedBy") Long updatedBy);

    int updateSortOrder(
            @Param("id") long id,
            @Param("sortOrder") int sortOrder,
            @Param("updatedBy") Long updatedBy);

    int deleteById(@Param("id") long id);

    int deleteByTicketTypeId(@Param("ticketTypeId") long ticketTypeId);

    int countByAttributeId(@Param("attributeId") long attributeId);
}
