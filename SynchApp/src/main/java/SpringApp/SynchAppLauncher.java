package SpringApp;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import SpringApp.Controllers.AppLogger;

@SpringBootApplication
@ComponentScan(basePackages = {"com.bigid.appinfra", "SpringApp"})
public class SynchAppLauncher {

	public static void main(String[] args) throws SecurityException, IOException {		
		SpringApplication.run(SynchAppLauncher.class, args);
		AppLogger logger = new AppLogger();
	}

}