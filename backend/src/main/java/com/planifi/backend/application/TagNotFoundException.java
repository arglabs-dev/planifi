package com.planifi.backend.application;

import java.util.List;

public class TagNotFoundException extends RuntimeException {

    private final List<String> missingTags;

    public TagNotFoundException(List<String> missingTags) {
        super("Tags not found: " + String.join(", ", missingTags));
        this.missingTags = missingTags;
    }

    public List<String> getMissingTags() {
        return missingTags;
    }
}
