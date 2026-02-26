package br.dev.allissonnunes.algashop.billingscheduler.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "algashop.integrations.payment")
@Data
class AlgaShopPaymentProperties {

    @Valid
    @NotNull
    private FastpayProperties fastpay;

    public record FastpayProperties(
            @NotBlank
            @URL
            String host,
            @NotBlank
            String privateToken
    ) {

    }

}
