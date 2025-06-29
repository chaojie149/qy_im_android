package com.tongxin.caihong.service.bean;

import java.util.List;

public class Question {
    private String questionId;
    private int companyMpId;
    private List<String> keywords;
    private String question;

    @Override
    public String toString() {
        return "Question{" +
                "questionId='" + questionId + '\'' +
                ", companyMpId=" + companyMpId +
                ", keywords=" + keywords +
                ", question='" + question + '\'' +
                '}';
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public int getCompanyMpId() {
        return companyMpId;
    }

    public void setCompanyMpId(int companyMpId) {
        this.companyMpId = companyMpId;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

}