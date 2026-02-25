package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import br.dev.allissonnunes.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class CancelExpiredInvoicesScheduler implements ApplicationRunner {

    private final CancelExpiredInvoicesApplicationService cancelExpiredInvoicesApplicationService;

    //    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    void runTask() {
        log.info("Task started - Canceling expired invoices");
        cancelExpiredInvoicesApplicationService.cancelExpiredInvoices();
        log.info("Task completed - Canceling expired invoices");
    }

    @Override
    public void run(final @NonNull ApplicationArguments args) throws Exception {
        runTask();
    }

}
