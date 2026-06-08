package com.uniondesk.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class UnionDeskEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public UnionDeskEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(UnionDeskDomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
