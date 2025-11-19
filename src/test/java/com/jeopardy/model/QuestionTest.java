package com.jeopardy.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

public class QuestionTest {
    @Test
    public void testQuestionCreation() {
        Map<String, String> options = new HashMap<>();
        options.put("A", "Option A"); options.put("B", "Option B");
        options.put("C", "Option C"); options.put("D", "Option D");
        
        Question question = new Question("Category", 100, "Test?", options, "A");
        assertEquals("Category", question.getCategory());
        assertEquals(100, question.getValue());
        assertTrue(question.isCorrect("A"));
        assertFalse(question.isCorrect("B"));
    }
}
