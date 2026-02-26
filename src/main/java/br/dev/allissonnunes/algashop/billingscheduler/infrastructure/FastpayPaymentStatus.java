package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

public enum FastpayPaymentStatus {
    PENDING,
    PROCESSING,
    FAILED,
    REFUNDED,
    PAID,
    CANCELED
}
