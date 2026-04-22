package com.smartagri;

import com.smartagri.service.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * SmartAgri Routing Engine - Spring Boot Application
 * ============================================================
 * Agricultural Supply Chain Cost + Carbon Optimizer
 *
 * CORE DSA: Dijkstra's Algorithm with Multi-Objective Scoring
 *   score = 0.4*cost + 0.3*time + 0.3*carbon
 *
 * On startup: initializes the Maharashtra supply chain graph
 * with 8 nodes and 14 edges.
 * ============================================================
 */
@SpringBootApplication
public class SmartAgriApplication implements CommandLineRunner {

    @Autowired
    private GraphService graphService;

    public static void main(String[] args) {
        SpringApplication.run(SmartAgriApplication.class, args);
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║     SmartAgri Routing Engine — STARTED               ║");
        System.out.println("║     Algorithm: Dijkstra's O((V+E) log V)             ║");
        System.out.println("║     Optimize: Cost + Time + Carbon                   ║");
        System.out.println("║     API: http://localhost:8080                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize the default Maharashtra supply chain graph on startup
        graphService.initializeDefaultGraph();
        System.out.println("✅ Graph initialized: " + graphService.getGraphStats());
    }
}