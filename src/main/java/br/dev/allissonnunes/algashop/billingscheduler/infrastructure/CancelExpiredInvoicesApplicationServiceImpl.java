package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import br.dev.allissonnunes.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
class CancelExpiredInvoicesApplicationServiceImpl implements CancelExpiredInvoicesApplicationService {

    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);

    private static final String UNPAID_STATUS = "UNPAID";

    private static final String CANCELED_STATUS = "CANCELED";

    private static final String CANCELLATION_REASON = "Invoice expired";

    private static final String SELECT_EXPIRED_INVOICES_SQL = """
            SELECT
            	i.id
            FROM
            	public.invoice i
            WHERE
            	i.expires_at <= NOW() - INTERVAL '3 days'
            	AND i.status = 'UNPAID'
            LIMIT 5
            FOR UPDATE SKIP LOCKED;
            """;

    private static final String CANCEL_EXPIRED_INVOICE_SQL = """
            UPDATE
            	public.invoice
            SET
            	status = 'CANCELED',
            	canceled_at = NOW(),
            	cancellation_reason = 'Invoice expired'
            WHERE
            	id = ?;
            """;

    private final JdbcClient jdbcClient;

    private final JdbcOperations jdbcOperations;

    private final TransactionTemplate transactionTemplate;

    @Override
    public void cancelExpiredInvoices() {
        transactionTemplate.executeWithoutResult(status -> {
            final List<UUID> expiredInvoicesIds = fetchExpiredInvoices();
            log.info("Task - Total invoices to be cancelled: {}", expiredInvoicesIds.size());
            if (expiredInvoicesIds.isEmpty()) {
                log.info("Task - No invoices to be cancelled");
                return;
            }
            final int totalCancelledInvoices = cancelInvoices(expiredInvoicesIds);
            log.info("Task - Total invoices cancelled: {}", totalCancelledInvoices);
        });
    }

    private List<UUID> fetchExpiredInvoices() {
        return jdbcClient.sql(SELECT_EXPIRED_INVOICES_SQL)
                .query(UUID.class)
                .list();
    }

    private int cancelInvoices(final List<UUID> invoiceIds) {
        int updatedInvoices = 0;

        try {
            final int[][] affectedRows = jdbcOperations.batchUpdate(
                    CANCEL_EXPIRED_INVOICE_SQL,
                    invoiceIds,
                    invoiceIds.size(),
                    (ps, argument) -> ps.setObject(1, argument));

            updatedInvoices = Stream.of(affectedRows)
                    .flatMapToInt(IntStream::of)
                    .reduce(updatedInvoices, Integer::sum);

            log.info("Task - Invoices {} canceled", invoiceIds);
        } catch (final DataAccessException e) {
            log.error("Task - Invoices {} could not be canceled", invoiceIds, e);
        }

//        for (UUID invoiceId : invoiceIds) {
//            try {
//                updatedInvoices += jdbc.sql(CANCEL_EXPIRED_INVOICE_SQL)
//                        .param(invoiceId)
//                        .update();
//                log.info("Task - Invoice {} cancelled", invoiceId);
//            } catch (final DataAccessException e) {
//                log.error("Task - Invoice {} could not be canceled", invoiceId, e);
//            }
//        }
        return updatedInvoices;
    }

}
