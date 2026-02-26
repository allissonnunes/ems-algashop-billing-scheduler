package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import br.dev.allissonnunes.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
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
class CancelExpiredInvoicesApplicationServiceJDBCImpl implements CancelExpiredInvoicesApplicationService {

    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);

    private static final String UNPAID_STATUS = "UNPAID";

    private static final String CANCELED_STATUS = "CANCELED";

    private static final String CANCELLATION_REASON = "Invoice expired";

    private static final String SELECT_EXPIRED_INVOICES_SQL = """
            SELECT
            	i.id,
            	ps.gateway_code
            FROM
            	public.invoice i
            INNER JOIN public.payment_settings ps ON
            	ps.id = i.payment_settings_id
            WHERE
            	i.expires_at <= NOW() - INTERVAL '3 days'
            	AND i.status = 'UNPAID'
            LIMIT 50
            FOR
            UPDATE
            	SKIP LOCKED;
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

    private final FastpayPaymentClient fastpayPaymentClient;

    @Override
    public void cancelExpiredInvoices() {
        transactionTemplate.executeWithoutResult(status -> {
            final List<InvoiceProjection> expiredInvoicesIds = fetchExpiredInvoices();
            log.info("Task - Total invoices to be canceled: {}", expiredInvoicesIds.size());
            if (expiredInvoicesIds.isEmpty()) {
                log.info("Task - No invoices to be canceled");
                return;
            }
            final int totalCanceledInvoices = cancelInvoices(expiredInvoicesIds);
            log.info("Task - Total invoices canceled: {}", totalCanceledInvoices);
        });
    }

    private List<InvoiceProjection> fetchExpiredInvoices() {
        final RowMapper<InvoiceProjection> rowMapper = (rs, rowNum) -> new InvoiceProjection(
                rs.getObject("id", UUID.class),
                rs.getString("gateway_code")
        );
        return jdbcClient.sql(SELECT_EXPIRED_INVOICES_SQL)
                .query(rowMapper)
                .list();
    }

    private int cancelInvoices(final List<InvoiceProjection> invoices) {
        final List<InvoiceProjection> canceledInvoices = invoices.stream()
                .filter(invoice -> {
                    try {
                        final FastpayPaymentModel paymentDetails = fastpayPaymentClient.findById(invoice.gatewayCode());
                        final FastpayPaymentStatus paymentStatus = FastpayPaymentStatus.valueOf(paymentDetails.status());
                        if (FastpayPaymentStatus.CANCELED.equals(paymentStatus)) {
                            log.info("Task - Invoice {} has the payment {} canceled on gateway", invoice.id(), invoice.gatewayCode());
                            return true;
                        }

                        fastpayPaymentClient.cancel(invoice.gatewayCode());
                        log.info("Task - Invoice {} has the payment {} canceled on gateway", invoice.id(), invoice.gatewayCode());

                        return true;
                    } catch (final Exception e) {
                        log.error("Task - Invoice {} payment {} could not be canceled on gateway", invoice.id(), invoice.gatewayCode(), e);
                    }
                    return false;
                })
                .toList();

        int updatedInvoices = 0;
        try {
            final int[][] affectedRows = jdbcOperations.batchUpdate(
                    CANCEL_EXPIRED_INVOICE_SQL,
                    canceledInvoices,
                    canceledInvoices.size(),
                    (ps, invoice) ->
                            ps.setObject(1, invoice.id()));

            updatedInvoices = Stream.of(affectedRows)
                    .flatMapToInt(IntStream::of)
                    .reduce(updatedInvoices, Integer::sum);

            log.info("Task - Invoices canceled");
        } catch (final DataAccessException e) {
            log.error("Task - Invoices could not be canceled", e);
        }

//        for (UUID invoiceId : invoices) {
//            try {
//                updatedInvoices += jdbc.sql(CANCEL_EXPIRED_INVOICE_SQL)
//                        .param(invoiceId)
//                        .update();
//                log.info("Task - Invoice {} canceled", invoiceId);
//            } catch (final DataAccessException e) {
//                log.error("Task - Invoice {} could not be canceled", invoiceId, e);
//            }
//        }
        return updatedInvoices;
    }

}
