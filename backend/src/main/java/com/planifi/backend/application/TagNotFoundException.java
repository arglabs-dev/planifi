package com.planifi.backend.application;

import java.util.List;

public class TagNotFoundException extends RuntimeException {

    private final List<String> missingTags;

    public TagNotFoundException(List<String> missingTags) {
        super("Missing tags: " + String.join(", ", missingTags));
        this.missingTags = List.copyOf(missingTags);
    }

    public List<String> getMissingTags() {
        return missingTags;
    }
}
