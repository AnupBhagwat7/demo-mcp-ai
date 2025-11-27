package com.example.demomcpai.service;

import com.example.demomcpai.entity.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {


    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper;

    public CategoryService(OpenAiChatModel openAiChatModel, ObjectMapper objectMapper) {
        this.openAiChatModel = openAiChatModel;
        this.objectMapper = objectMapper;
    }

    public record AiCategoryResponse(
            @JsonProperty(required = true) Long id,
            @JsonProperty(required = true) String observation
    ) {
    }

    public List<Category> listCategories() {
        return List.of(
                new Category(1L, "Salary"),
                new Category(2L, "Office supplies"),
                new Category(3L, "Travel"),
                new Category(4L, "Rent"),
                new Category(5L, "Health insurance")
        );
    }

    public Category guessCategory(String description){

        BeanOutputConverter<AiCategoryResponse> converter = new BeanOutputConverter<>(AiCategoryResponse.class);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_1_MINI)
                .maxTokens(1000) // helps to manage cost by limiting the quantity of tokens
                .temperature(0.0) // makes the answer closer to deterministic
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, converter.getJsonSchema()))
                .build();


        String promptContent;
        try {
            promptContent = """
              You are a financial transaction classifier. You will analyze the parsed transaction and define what should be the category.
              
              # Input Transaction
              * `%s`
              
              # System candidates
              ```json
              %s
              ```
              
              # Instructions:
              - Your input should be from **Input Transactions**
              - Category is mandatory, so, make the most educated guess, however, there will be cases where an assumption should be made.
              - Your output contains the categoryId, with is a long that must match the list of **System Candidates Categories**
              - The output also contains observation an optional String that you should inform additional notes or rational behind the chosen category.
              """.formatted(description, objectMapper.writeValueAsString(listCategories()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Prompt prompt = new Prompt(promptContent, options);

        var response = openAiChatModel.call(prompt);
        Long categoryId = converter.convert(response.getResult().getOutput().getText()).id;

        return listCategories().stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst()
                .orElseThrow();
    }
}
