package ru.iac.inscheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * [А14] ON-LINE проверка полисов.
 * Перенос SOAP-сервиса GetInsPrkState с Oracle/ASP.NET на PostgreSQL/Spring Boot.
 *
 * Наследует {@link SpringBootServletInitializer}, чтобы приложение можно было собрать
 * в war и развернуть во внешний Tomcat (как обычный бэк). При этом обычный запуск
 * через main() (fat-jar/встроенный Tomcat) тоже продолжает работать.
 */
@SpringBootApplication
public class InsCheckApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(InsCheckApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(InsCheckApplication.class, args);
    }
}
