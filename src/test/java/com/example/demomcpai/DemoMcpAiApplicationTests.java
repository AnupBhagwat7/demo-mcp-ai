package com.example.demomcpai;

import com.example.demomcpai.util.FinancialStatementPdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public
class DemoMcpAiApplicationTests {


    @Autowired
    public MockMvc mockMvc;


    public static void main(String[] args) throws IOException {
        FinancialStatementPdfGenerator.generatePDF("financial_statement.pdf");

        File file = new File("financial_statement.pdf");
        MultipartFile multipartFile = new MockMultipartFile(
                "file",                          // form field name
                file.getName(),                  // original filename
                "application/pdf",               // content type
                Files.readAllBytes(file.toPath()) // file content as bytes
        );
        List<String> descriptions =
                FinancialStatementPdfGenerator.extractDescriptions(multipartFile);

        System.out.println("=== Descriptions Extracted ===");
        descriptions.forEach(System.out::println);
    }
}
