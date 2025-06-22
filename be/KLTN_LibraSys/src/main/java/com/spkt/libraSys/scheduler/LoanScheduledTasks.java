package com.spkt.libraSys.scheduler;

import com.spkt.libraSys.service.loan.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanScheduledTasks {

    private final LoanService loanService;

    @Scheduled(cron = "0 0 0 * * *")
    public void autoCancelExpiredReservations() {
        log.info("Running scheduled task: autoCancelExpiredReservations()");
        loanService.autoCancelExpiredReservations();
    }
    @Scheduled(cron = "0 30 0 * * *") // chạy lúc 00:00 mỗi ngày
    public void notifyUsersNearDueDate() {
        log.info("Scheduled task: notifyUsersNearDueDate()");
        loanService.notifyUsersNearDueDate();
    }

    @Scheduled(cron = "0 45 0 * * *") // 00:45 mỗi ngày - kiểm tra block user quá hạn trả trên 30 ngày
    public void blockUsersOverdueReturn() {
        log.info("Scheduled task: blockUsersOverdueReturn() started");
        loanService.blockUsersWithReturnOverdue30Days();
        log.info("Scheduled task: blockUsersOverdueReturn() completed");
    }
}
