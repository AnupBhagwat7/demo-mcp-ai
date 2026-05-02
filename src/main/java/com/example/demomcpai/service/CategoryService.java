package com.example.demomcpai.service;

import com.example.demomcpai.entity.Category;
import com.example.demomcpai.repository.CategoryRepository;
import com.example.demomcpai.tools.CategoryTools;
import com.example.demomcpai.util.FinancialStatementPdfGenerator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class CategoryService {


    private final AnthropicChatModel anthropicChatModel;
    private final CategoryRepository categoryRepository;

    public CategoryService(AnthropicChatModel anthropicChatModel, CategoryRepository categoryRepository) {
        this.anthropicChatModel = anthropicChatModel;
        this.categoryRepository = categoryRepository;
    }

    public List<Category> guessCategoryPdf(MultipartFile file) throws IOException {

        List<String> descriptions = FinancialStatementPdfGenerator.extractDescriptions(file);
        if (descriptions != null && !descriptions.isEmpty()) {
            List<Category> categories = guessCategory(descriptions);
            System.out.println("Guessed categories: " + categories);
            return categories;
        } else {
            System.out.println("Failed to extract descriptions from the PDF.");
            return Collections.emptyList();
        }
    }

    public record AiCategoryResponse(
            @JsonProperty(required = true) Long categoryId,
            @JsonProperty(required = true) String sourceDescription,
            @JsonProperty(required = true) String observation
    ) {
    }

    public record AiCategoryListResponse(
            @JsonProperty(required = true) List<AiCategoryResponse> categories
    ) {
    }

    public List<Category> guessCategory(List<String> descriptions) {

        BeanOutputConverter<AiCategoryListResponse> converter = new BeanOutputConverter<>(AiCategoryListResponse.class);

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model("claude-sonnet-4-5") // using the most capable model to get the best results
                .maxTokens(500) // helps to manage cost by limiting the quantity of tokens
                .temperature(0.0) // makes the answer closer to deterministic
                .build();


        String promptContent;
        try {
            String bulletDescriptions = descriptions.stream()
                    .map(d -> "- " + d)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            promptContent =
                    "You are a financial transaction classifier. You will analyze the parsed transactions "
                            + "and define what should be the category for each one.\n\n"
                            + "Your job is to assign the most appropriate system category to each description "
                            + "that appears in Input Transactions.\n\n"

                            + "# Input Transactions\n"
                            + bulletDescriptions + "\n\n"

                            + "# Instructions\n"
                            + "1. You MUST call listCategories first and prefer one of those IDs even when "
                            + "the match is only approximate.\n"
                            + "2. Only call createCategory when none of the existing categories are even roughly "
                            + "relevant. Call it once per batch of transactions at most.\n"
                            + "3. For every input description you must output an object containing:\n"
                            + "   - categoryId: a Long that exists in the system (or was just returned by createCategory)\n"
                            + "   - sourceDescription: the original text, unchanged\n"
                            + "   - observation: optional free-text notes on your reasoning\n\n"

                            + "# Few-Shot Examples\n\n"
                            + "## Example 1 — reuse existing categories\n"
                            + "System categories: 1: Salary, 2: Office supplies, 3: Travel, 4: Rent, 5: Health insurance\n\n"
                            + "Input transactions:\n"
                            + "- Foxtons Real Estate London\n"
                            + "- TFL TRAVEL CHARGE TFL.GOV.UK/CP\n\n"
                            + "Reasoning:\n"
                            + "- Foxtons is a real-estate letting agent → choose category Rent (id 4).\n"
                            + "- TFL is London public transport → choose category Travel (id 3).\n\n"

                            + "## Example 2 — create a new category\n"
                            + "System categories: same as above\n\n"
                            + "Input transactions:\n"
                            + "- McDonald's\n\n"
                            + "Reasoning:\n"
                            + "- No existing category fits a restaurant/fast-food spend.\n"
                            + "- Call createCategory(\"Eating Out\") → returns id 6, name: Eating Out.\n\n"

                            + "## Example 3 — output format reference\n"
                            + "Input: Netflix subscription payment\n"
                            + "Expected output:\n"
                            + "{\n"
                            + "  \"categories\": [\n"
                            + "    {\n"
                            + "      \"categoryId\": 6,\n"
                            + "      \"sourceDescription\": \"Netflix subscription payment\",\n"
                            + "      \"observation\": \"Netflix is a streaming service, matches Entertainment category.\"\n"
                            + "    }\n"
                            + "  ]\n"
                            + "}\n\n"

                            + "Input: Indigo flight to Mumbai\n"
                            + "Expected output:\n"
                            + "{\n"
                            + "  \"categories\": [\n"
                            + "    {\n"
                            + "      \"categoryId\": 3,\n"
                            + "      \"sourceDescription\": \"Indigo flight to Mumbai\",\n"
                            + "      \"observation\": \"Indigo is a commercial airline, matches Travel category.\"\n"
                            + "    }\n"
                            + "  ]\n"
                            + "}\n\n"

                            + "# Output Rules\n"
                            + "CRITICAL: Respond with ONLY a raw JSON object. "
                            + "No markdown, no code fences, no explanation, no bullet points. "
                            + "Your response must start with '{' and end with '}' and be directly parseable by JSON.parse().\n\n"

                            + converter.getFormat();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Prompt prompt = new Prompt(promptContent, options);

        var categoryTools = new CategoryTools(categoryRepository);

        var response = ChatClient.create(anthropicChatModel)
                .prompt(prompt)
                .options(options)
                .tools(categoryTools)
                .call();

        var responseObject = converter.convert(response.content());

        return descriptions.stream()
                .map(description -> {
                    assert responseObject != null;
                    AiCategoryResponse current = responseObject.categories().stream()
                            .filter(c -> description.equals(c.sourceDescription()))
                            .findFirst()
                            .orElseThrow();
                    return categoryRepository.listCategories().stream()
                            .filter(category -> Objects.equals(category.id(), current.categoryId()))
                            .findFirst()
                            .orElseThrow();
                })
                .toList();
    }

    public List<Category> listCategories() {
        return categoryRepository.listCategories();
    }
}
