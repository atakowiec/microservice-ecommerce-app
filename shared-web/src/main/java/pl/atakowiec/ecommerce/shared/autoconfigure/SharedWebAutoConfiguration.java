package pl.atakowiec.ecommerce.shared.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import pl.atakowiec.ecommerce.shared.exception.HttpExceptionHandler;
import pl.atakowiec.ecommerce.shared.exception.ServerExceptionHandler;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class SharedWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    HttpExceptionHandler httpExceptionHandler() {
        return new HttpExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    ServerExceptionHandler serverExceptionHandler() {
        return new ServerExceptionHandler();
    }
}