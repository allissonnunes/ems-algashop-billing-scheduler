package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import br.dev.allissonnunes.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
class CancelExpiredInvoicesScheduler {

    private final CancelExpiredInvoicesApplicationService cancelExpiredInvoicesApplicationService;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    void runTask() {
        log.info("Task started - Canceling expired invoices");
        cancelExpiredInvoicesApplicationService.cancelExpiredInvoices();
        log.info("Task completed - Canceling expired invoices");
    }

}
