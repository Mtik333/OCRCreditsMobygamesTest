package com.nfssoundtrack.creditstest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class CreditstestApplication implements CommandLineRunner {
	@Resource
	FilesStorageService storageService;

	public static void main(String[] args) {
		SpringApplication.run(CreditstestApplication.class, args);
	}

	@Override
	public void run(String... arg) throws Exception {
		Path root = Paths.get("uploads");
		if (!root.toFile().exists()){
			Files.createDirectory(root);
		}
//		storageService.deleteAll();
//		storageService.init();
	}
}
