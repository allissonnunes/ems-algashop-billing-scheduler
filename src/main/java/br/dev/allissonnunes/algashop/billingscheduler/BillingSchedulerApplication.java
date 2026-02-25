package br.dev.allissonnunes.algashop.billingscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableScheduling
public class BillingSchedulerApplication {

    static void main(String[] args) {
        SpringApplication.run(BillingSchedulerApplication.class, args);
    }

}
