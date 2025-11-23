package com.jeopardy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameData {

    private List<Category> categories;

    public GameData() {
        this.categories = new ArrayList<>();
    }


    public void addQuestion(Question question) {
        String categoryName = question.getCategory();

        
        Optional<Category> existing = findCategory(categoryName);

        if (existing.isPresent()) {
            existing.get().addQuestion(question);
        } else {
            
            Category newCategory = new Category(categoryName);
            newCategory.addQuestion(question);
            categories.add(newCategory);
        }
    }

    
    public void addCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        categories.add(category);
    }

    
    public List<Category> getCategories() {
        return new ArrayList<>(categories); 
    }

    public Category getCategory(String name) {
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Optional<Category> findCategory(String name) {
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public int getTotalCategories() {
        return categories.size();
    }

    public int getTotalQuestions() {
        return categories.stream()
                .mapToInt(c -> c.getQuestions().size())
                .sum();
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }

    public boolean hasCategory(String name) {
        return categories.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    @Override
    public String toString() {
        return String.format(
            "GameData{categories=%d, totalQuestions=%d}",
            getTotalCategories(), getTotalQuestions()
        );
    }
}
