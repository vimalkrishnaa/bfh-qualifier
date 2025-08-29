package com.example.bfhqualifier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class BfhQualifierApplication implements CommandLineRunner {

    // Endpoints as per task PDF
    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    public static void main(String[] args) {
        SpringApplication.run(BfhQualifierApplication.class, args);
    }

    @Override
    public void run(String... args) {
        RestTemplate rest = new RestTemplate();

        // 1) Prepare the request body 
        Map<String, Object> genReq = new HashMap<>();
        genReq.put("name", "Vimal Krishna S");
        genReq.put("regNo", "22BCE3009");
        genReq.put("email", "vimalkrishna.s2022@vitstudent.ac.in");

        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> genEntity = new HttpEntity<>(genReq, json);

        try {
            // 2) Call generateWebhook
            ResponseEntity<Map> genResp = rest.postForEntity(GENERATE_WEBHOOK_URL, genEntity, Map.class);

            if (genResp.getStatusCode().is2xxSuccessful() && genResp.getBody() != null) {
                Object webhookObj = genResp.getBody().get("webhook");
                Object tokenObj = genResp.getBody().get("accessToken");

                if (webhookObj == null || tokenObj == null) {
                    System.err.println("Error: Missing 'webhook' or 'accessToken' in response.");
                    System.err.println("Response body: " + genResp.getBody());
                    return;
                }

                String webhookUrl = webhookObj.toString();
                String accessToken = tokenObj.toString();

                System.out.println("Webhook URL: " + webhookUrl);
                System.out.println("Access Token (JWT): " + accessToken);

                // 3) Build the final SQL query for Question 1 (Odd regNo)
                // MySQL-style; selects highest salary not on 1st day, with name, age, department
                String finalQuery =
                        "SELECT p.AMOUNT AS SALARY, " +
                        "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                        "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
                        "d.DEPARTMENT_NAME " +
                        "FROM PAYMENTS p " +
                        "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                        "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                        "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                        "AND p.AMOUNT = (SELECT MAX(AMOUNT) FROM PAYMENTS WHERE DAY(PAYMENT_TIME) <> 1) " +
                        "LIMIT 1;";

                // 4) Submit the final query to the returned webhook with JWT Bearer auth
                Map<String, Object> answer = new HashMap<>();
                answer.put("finalQuery", finalQuery);

                HttpHeaders authHeaders = new HttpHeaders();
                authHeaders.setContentType(MediaType.APPLICATION_JSON);
                authHeaders.setBearerAuth(accessToken); // Authorization: Bearer <token>

                HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(answer, authHeaders);
                ResponseEntity<String> submitResp = rest.postForEntity(webhookUrl, submitEntity, String.class);

                System.out.println("Submission Status: " + submitResp.getStatusCode());
                System.out.println("Submission Body: " + submitResp.getBody());
            } else {
                System.err.println("Failed to generate webhook. HTTP: " + genResp.getStatusCode());
                System.err.println("Body: " + (genResp.getBody() == null ? "<null>" : genResp.getBody()));
            }
        } catch (RestClientException ex) {
            System.err.println("HTTP error calling APIs: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
