package serenity.api_gatewya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "serenity.api_gatewya", "configs" })
public class ApiGatewyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewyaApplication.class, args);
    }

}