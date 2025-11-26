package com.example.demomcpai.controller;

import com.example.demomcpai.entity.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @GetMapping("/categories")
    public List<Category> listCategories() {
        return List.of(
                new Category(1L, "Salary"),
                new Category(2L, "Office supplies"),
                new Category(3L, "Travel"),
                new Category(4L, "Rent"),
                new Category(5L, "Health insurance")
        );
    }
}
