package co.g3a.functionalrop.ejemplo;

import co.g3a.functionalrop.ErrorMessageProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfiguration {

    @Bean
    public ErrorMessageProvider errorMessageProvider(){
        return new ErrorMessageProvider("es");
    }
}
