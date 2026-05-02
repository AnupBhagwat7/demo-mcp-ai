package com.example.demomcpai.tools;

import com.example.demomcpai.entity.Category;
import com.example.demomcpai.repository.CategoryRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryTools {

    private final CategoryRepository categoryRepository;

    public CategoryTools(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
}
