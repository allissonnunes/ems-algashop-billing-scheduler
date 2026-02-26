package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import java.math.BigDecimal;

public record FastpayPaymentModel(
        String id,
        String referenceCode,
        String status,
        String method,
        BigDecimal totalAmount
) {

}
