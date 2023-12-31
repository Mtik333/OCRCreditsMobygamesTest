package com.nfssoundtrack.creditstest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class CreditstestApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(CreditstestApplication.class, args);
    }

    @Override
    public void run(String... arg) throws Exception {
        Path root = Paths.get("uploads");
        FileSystemUtils.deleteRecursively(root.toFile());
        Files.createDirectory(root);
    }
}
