package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@ImportHttpServices(
        group = "fastpay",
        types = { FastpayPaymentClient.class }
)
@Configuration
class FastpayClientConfiguration {

}
