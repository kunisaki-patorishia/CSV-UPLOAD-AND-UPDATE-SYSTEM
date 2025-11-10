package main.java.com.csvuploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CsvUploadApplication {
    public static void main(String[] args) {
        SpringApplication.run(CsvUploadApplication.class, args);
    }
}
