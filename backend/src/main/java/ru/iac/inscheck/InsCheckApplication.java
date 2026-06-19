package ru.iac.inscheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * [А14] ON-LINE проверка полисов.
 * Перенос SOAP-сервиса GetInsPrkState с Oracle/ASP.NET на PostgreSQL/Spring Boot.
 */
@SpringBootApplication
public class InsCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsCheckApplication.class, args);
    }
}
