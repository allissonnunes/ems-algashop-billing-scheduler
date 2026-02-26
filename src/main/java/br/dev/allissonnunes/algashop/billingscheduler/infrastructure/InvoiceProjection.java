package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import java.util.UUID;

public record InvoiceProjection(UUID id, String gatewayCode) {

}
