package ru.iac.inscheck.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Конфигурация SOAP-эндпоинта.
 *
 * Сервлет публикуется по адресу /ws/*, WSDL доступен по /ws/inscheck.wsdl.
 * Контракт (namespace http://tempuri.org/, операция GetInsPrkState) повторяет
 * старый asmx-сервис, поэтому существующие клиенты работают без изменений.
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean
    public XsdSchema inscheckSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/inscheck.xsd"));
    }

    @Bean(name = "inscheck")
    public DefaultWsdl11Definition wsdl11Definition(XsdSchema inscheckSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("InsCheckPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://tempuri.org/");
        wsdl.setSchema(inscheckSchema);
        return wsdl;
    }

    /**
     * Маршаллер JAXB по пакету модели — сам находит классы запроса/ответа
     * с аннотациями @XmlRootElement.
     */
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("ru.iac.inscheck.ws.model");
        return marshaller;
    }
}
