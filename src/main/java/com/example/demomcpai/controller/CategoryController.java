package com.example.demomcpai.controller;

import com.example.demomcpai.entity.Category;
import com.example.demomcpai.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<Category> listCategories() {
        return categoryService.listCategories();
    }

    @GetMapping("/categories/ai-classification")
    public Category guessCategory(@RequestParam String description){
        return categoryService.guessCategory(description);
    }
}
