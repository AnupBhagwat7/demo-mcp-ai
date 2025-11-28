package com.example.demomcpai.repository;

import com.example.demomcpai.entity.Category;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CategoryRepository {

    private static final List<Category> categories = new ArrayList<>(List.of(
            new Category(1L, "Salary"),
            new Category(2L, "Office supplies"),
            new Category(3L, "Travel"),
            new Category(4L, "Rent"),
            new Category(5L, "Health insurance")
    ));

    private final List<Category> initialCategories = new ArrayList<>(categories);

    public List<Category> listCategories() {
        return List.copyOf(categories);
    }

    public Category addCategory(Category category) {

        categories.add(category);
        return category;
    }

    public void resetCategories() {
        categories.clear();
        categories.addAll(initialCategories);
    }
}
