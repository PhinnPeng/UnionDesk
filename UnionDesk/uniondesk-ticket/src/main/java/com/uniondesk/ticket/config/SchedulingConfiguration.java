package com.uniondesk.ticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务（SlaScanJob 每分钟扫描超时工单）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
