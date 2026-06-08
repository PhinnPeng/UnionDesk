package com.uniondesk.notification.mapper;

import com.uniondesk.notification.entity.NotificationTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationTemplateMapper {

    List<NotificationTemplatePo> selectByDomainId(
            @Param("scopeId") long scopeId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomainId(@Param("scopeId") long scopeId);

    NotificationTemplatePo selectByIdAndDomainId(
            @Param("id") long id,
            @Param("scopeId") long scopeId);

    void updateByIdAndDomainId(
            @Param("id") long id,
            @Param("scopeId") long scopeId,
            @Param("eventCategory") String eventCategory,
            @Param("channel") String channel,
            @Param("code") String code,
            @Param("titleTemplate") String titleTemplate,
            @Param("contentTemplate") String contentTemplate,
            @Param("isSecurity") boolean isSecurity,
            @Param("isUnsubscribable") boolean isUnsubscribable,
            @Param("status") String status);
}
