package com.org.pp.finAgent;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = "classpath:secrets.properties", ignoreResourceNotFound = true)
public class FinAgentApplication {

	public static void main(String[] args) {
		Application.launch(JavaFxApplication.class, args);
	}

}