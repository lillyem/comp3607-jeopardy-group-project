package com.jeopardy.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class Category {
    private String name;
    private Map<Integer, Question> questions; 
    
    public Category(String name) {
        this.name = name;
        this.questions = new TreeMap<>(); 
    }
    
    
    public void addQuestion(Question question) {
        if (!name.equals(question.getCategory())) {
            throw new IllegalArgumentException("Question category must match category name");
        }
        questions.put(question.getValue(), question);
    }
    
    
    public String getName() { 
        return name; 
    }
    
    public Map<Integer, Question> getQuestions() { 
        return new TreeMap<>(questions); 
    }
    
    public Question getQuestion(int value) { 
        return questions.get(value); 
    }
    
    public boolean hasQuestion(int value) { 
        return questions.containsKey(value); 
    }
    
    public Collection<Question> getAllQuestions() {
        return questions.values();
    }
    
    
    public boolean allQuestionsAnswered() {
        return questions.values().stream().allMatch(Question::isAnswered);
    }
    
    public int getAvailableQuestionCount() {
        return (int) questions.values().stream().filter(q -> !q.isAnswered()).count();
    }
    
    public boolean hasAvailableQuestions() {
        return questions.values().stream().anyMatch(q -> !q.isAnswered());
    }
    
    public void resetAllQuestions() {
        questions.values().forEach(q -> q.setAnswered(false));
    }
    
    @Override
    public String toString() {
        return String.format("Category{name='%s', questionCount=%d}", name, questions.size());
    }
}