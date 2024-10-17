package com.example.flowerrecognitionapp;

import java.util.List;

public class ImaggaResponse {
    private Result result;

    public Result getResult() {
        return result;
    }

    public static class Result {
        private List<Tag> tags;

        public List<Tag> getTags() {
            return tags;
        }
    }

    public static class Tag {
        private TagDetail tag;
        private double confidence;

        public TagDetail getTag() {
            return tag;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public static class TagDetail {
        private String en;

        public String getEn() {
            return en;
        }
    }
}
