package com.example.demomcpai.controller;

import com.example.demomcpai.entity.Category;
import com.example.demomcpai.service.CategoryService;
import com.example.demomcpai.tools.CategoryTools;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public List<Category> guessCategory(@RequestParam List<String> descriptions) {
        return categoryService.guessCategory(descriptions);
    }

    @PostMapping("/categories/ai-classification/pdf")
    public List<Category> guessCategoryPdf(@RequestParam("file") MultipartFile file) throws IOException {
        return categoryService.guessCategoryPdf(file);
    }

}
