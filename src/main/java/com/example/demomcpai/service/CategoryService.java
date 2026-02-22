package com.example.demomcpai.service;

import com.example.demomcpai.entity.Category;
import com.example.demomcpai.repository.CategoryRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {


    private final OpenAiChatModel openAiChatModel;
    private final CategoryRepository categoryRepository;


    public CategoryService(OpenAiChatModel openAiChatModel, CategoryRepository categoryRepository) {
        this.openAiChatModel = openAiChatModel;
        this.categoryRepository = categoryRepository;
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

    @Tool(description = "List all the system categories")
    public List<Category> listCategories() {
        return categoryRepository.listCategories();
    }

    @Tool(description = "Create a new category based on the category name")
    public Category createCategory(
            @ToolParam(description = "The name of the category, should be clean and preferable not more than 3 words")
            String categoryName
    ) {
        Long newId = listCategories().stream()
                .map(Category::id)
                .max(Long::compareTo)
                .orElse(1L);
        Category category = new Category(newId + 1, categoryName);
        return categoryRepository.addCategory(category);
    }


    public List<Category> guessCategory(List<String> descriptions){

        BeanOutputConverter<AiCategoryListResponse> converter = new BeanOutputConverter<>(AiCategoryListResponse.class);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_1)
                .maxTokens(1000) // helps to manage cost by limiting the quantity of tokens
                .temperature(0.0) // makes the answer closer to deterministic
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, converter.getJsonSchema()))
                .build();


        String promptContent;
        try {
            promptContent = """
              You are a financial transaction classifier. You will analyze the parsed transactions and define what should be the category for each one.
              
              Your job is to assign the most appropriate **system category** to each description that appears in **Input Transactions**.
              
              # Input Transaction
              %s
              
              # Instructions:
              1. You MUST call **listCategories** first and prefer one of those IDs even when the match is only approximate.
              2. Only call **createCategory** when none of the existing categories are even roughly relevant.
                 • Call it once per batch of transactions at most.
              3. For every input description you must output an object containing:
                 • **categoryId** – a Long that exists in the system (or was just returned by createCategory)
                 • **sourceDescription** – the original text, unchanged
                 • **observation** – optional free-text notes on your reasoning
              
              # FEW-SHOT EXAMPLES
              (These examples guide you on how to behave.)
              
              ## Example 1 – all transactions can reuse existing categories
              **System categories** (id → name):
              1: Salary; 2: Office supplies; 3: Travel; 4: Rent; 5: Health insurance
              
              **Input transactions**
              • Foxtons Real State London
              • TFL TRAVEL CHARGE TFL.GOV.UK/CP
              
              **Assistant reasoning (implicit)**
              - “Foxtons” is a real-estate letting agent → choose category *Rent* (id 4).
              - “TFL” is London public transport → choose category *Travel* (id 3).
              
              ## Example 2 – no suitable category, so create one
              System categories (same as above)
              
              **Input transactions**
              • McDonald's
              
              **Assistant reasoning (implicit)**
              - No existing category fits a restaurant/fast-food spend.\s
              - Call createCategory("Eating Out") → suppose it returns { "id": 6, "name": "Eating Out" }.
              
              """
                    .formatted(
                            descriptions.stream().map(d -> " - `" + d + "`").reduce((a, b) -> a + "\n" + b).orElse("")
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Prompt prompt = new Prompt(promptContent);

        var categoryTools = new CategoryService(openAiChatModel, categoryRepository);

        var response = ChatClient.create(openAiChatModel)
                .prompt(prompt)
                .options(options)
                .tools(categoryTools)
                .call();

        var responseObject = converter.convert(response.content());

        return descriptions.stream()
                .map(description -> {
                    AiCategoryResponse current = responseObject.categories().stream()
                            .filter(c -> description.equals(c.sourceDescription()))
                            .findFirst()
                            .orElseThrow();
                    return listCategories().stream()
                            .filter(category -> category.id() == current.categoryId())
                            .findFirst()
                            .orElseThrow();
                })
                .toList();
    }
}
