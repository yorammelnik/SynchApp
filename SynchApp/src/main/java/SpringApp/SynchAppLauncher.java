package SpringApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.bigid.appinfra", "SpringApp"})
public class SynchAppLauncher {

	public static void main(String[] args) {		
		SpringApplication.run(SynchAppLauncher.class, args);
	}

}