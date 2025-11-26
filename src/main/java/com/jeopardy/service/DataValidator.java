package com.jeopardy.service;

import com.jeopardy.model.Category;
import com.jeopardy.model.Question;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating Jeopardy game data before use.
 * <p>
 * This validator is typically used by CSV, JSON and XML loaders to ensure that:
 * <ul>
 *     <li>At least one category exists</li>
 *     <li>Category names are present and unique</li>
 *     <li>Each category contains at least one question</li>
 *     <li>Question values are positive and not repeated within a category</li>
 *     <li>Each question has valid text, options A–D, and a valid correct answer</li>
 * </ul>
 */

public class DataValidator {

     /**
     * Validates a list of categories and their contained questions.
     * <p>
     * Checks that:
     * <ul>
     *     <li>The list is not {@code null} or empty</li>
     *     <li>Each category passes {@link #validateCategory(Category)}</li>
     *     <li>No two categories share the same name (case-insensitive)</li>
     * </ul>
     *
     * @param categories the list of categories to validate
     * @throws IllegalArgumentException if validation fails for any reason
     */
    public static void validateCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("No categories found in file.");
        }

        Set<String> categoryNames = new HashSet<>();

        for (Category c : categories) {
            validateCategory(c);

            if (!categoryNames.add(c.getName().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate category name: " + c.getName());
            }
        }
    }

    /**
     * Validates a single category and all of its questions.
     * <p>
     * Checks that:
     * <ul>
     *     <li>The category name is present and non-blank</li>
     *     <li>The category contains at least one question</li>
     *     <li>Each question passes {@link #validateQuestion(Question, String)}</li>
     *     <li>No two questions share the same point value within this category</li>
     * </ul>
     *
     * @param c the category to validate
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public static void validateCategory(Category c) {

        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is missing.");
        }

        // getQuestions() returns a MAP <Integer, Question>
        if (c.getAllQuestions() == null || c.getAllQuestions().isEmpty()) {
            throw new IllegalArgumentException(
                "Category '" + c.getName() + "' has no questions."
            );
        }

        Set<Integer> values = new HashSet<>();

        // Iterate through the values() of the map
        for (Question q : c.getAllQuestions()) {

            validateQuestion(q, c.getName());

            if (!values.add(q.getValue())) {
                throw new IllegalArgumentException(
                    "Category '" + c.getName() + "' has repeated question value: " + q.getValue()
                );
            }
        }
    }

    /**
     * Validates an individual question within a specific category.
     * <p>
     * Checks that:
     * <ul>
     *     <li>Question text is present and non-blank</li>
     *     <li>Options map is non-null and contains keys A, B, C and D</li>
     *     <li>Each option A–D has non-empty text</li>
     *     <li>The correct answer is one of A, B, C or D</li>
     *     <li>The question value is positive</li>
     * </ul>
     *
     * @param q            the question to validate
     * @param categoryName the name of the category this question belongs to
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public static void validateQuestion(Question q, String categoryName) {
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            throw new IllegalArgumentException("A question in category '" + categoryName + "' has no text.");
        }

        Map<String, String> options = q.getOptions();
        if (options == null ||
                !options.containsKey("A") ||
                !options.containsKey("B") ||
                !options.containsKey("C") ||
                !options.containsKey("D")) {
            throw new IllegalArgumentException(
                    "A question in category '" + categoryName + "' must have options A, B, C, and D.");
        }

        for (String key : new String[]{"A", "B", "C", "D"}) {
            String option = options.get(key);
            if (option == null || option.isBlank()) {
                throw new IllegalArgumentException(
                        "Option " + key + " in category '" + categoryName + "' cannot be empty.");
            }
        }

        String correct = q.getCorrectAnswer();
        if (correct == null ||
                !"ABCD".contains(correct.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid correct option in category '" + categoryName +
                            "'. Must be A, B, C, or D.");
        }

        if (q.getValue() <= 0) {
            throw new IllegalArgumentException(
                    "Question in '" + categoryName + "' has invalid value: " + q.getValue());
        }
    }
}
