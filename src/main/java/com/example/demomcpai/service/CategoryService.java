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
            @ToolParam(description = "The name of the category, should be clean and preferable no more than 3 words")
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
                .model(OpenAiApi.ChatModel.GPT_4_1_MINI)
                .maxTokens(1000) // helps to manage cost by limiting the quantity of tokens
                .temperature(0.0) // makes the answer closer to deterministic
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, converter.getJsonSchema()))
                .build();


        String promptContent;
        try {
            promptContent = """
              You are a financial transaction classifier. You will analyze the parsed transactions and define what should be the category for each one.
              
              # Input Transaction
              %s
              
              # Instructions:
              - You must classify each transaction from the **Input Transactions** list
              - You should use listCategories tool to get the list of candidates categories
              - Category is mandatory, so, make the most educated guess, however, there will be cases where an assumption should be made.
              - You must try using the existing categories, even though, they don't have an exact match.
              - In the case there is no absolute no matching close category, you can use the tool createCategory, however, you should avoid it.
              - Your output contains:
                * categoryId, with is a long that must match the list of **System Candidates Categories**
                * sourceDescription: the original transaction text exactly as provided in the input
                * observation: an optional String that you should inform additional notes or rational behind the chosen category.
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
