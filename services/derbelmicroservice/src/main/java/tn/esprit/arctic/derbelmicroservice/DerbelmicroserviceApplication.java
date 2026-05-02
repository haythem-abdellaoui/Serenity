package tn.esprit.arctic.derbelmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DerbelmicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DerbelmicroserviceApplication.class, args);
    }

}
