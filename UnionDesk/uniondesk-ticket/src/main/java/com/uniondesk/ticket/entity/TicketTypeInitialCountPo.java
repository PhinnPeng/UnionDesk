package com.uniondesk.ticket.entity;

/**
 * 事项类型「未处理」计数行：ticket_type_id 对应的初始状态工单数（工作台类型筛选徽标）。
 */
public class TicketTypeInitialCountPo {

    private long ticketTypeId;
    private long count;

    public long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
