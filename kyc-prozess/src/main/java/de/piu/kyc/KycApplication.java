package de.piu.kyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** KYC-Prüfprozess auf CIB seven. BPMN und DMN aus src/main/resources werden beim Start deployt. */
@SpringBootApplication
public class KycApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(KycApplication.class, args);
    }

    /** Der CIB seven Webclient liefert unter /webapp/ selbst keine Startseite aus. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/webapp/").setViewName("forward:/webapp/index.html");
    }
}
