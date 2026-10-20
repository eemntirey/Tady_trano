package mg.emit.tadytrano;

import org.springframework.boot.SpringApplication;
import mg.emit.tadytrano.infrastructure.config.ConfigurationSecurite;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TadyTranoApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TadyTranoApplication.class);
        // Valider les variables critiques avant de creer les beans DataSource/Flyway.
        application.addInitializers(new ConfigurationSecurite());
        application.run(args);
    }
}
