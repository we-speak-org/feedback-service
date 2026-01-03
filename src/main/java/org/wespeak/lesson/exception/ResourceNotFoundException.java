package org.wespeak.lesson.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String code;
    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String code, String resourceType, String resourceId) {
        super(String.format("%s not found: %s", resourceType, resourceId));
        this.code = code;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public static ResourceNotFoundException courseNotFound(String courseId) {
        return new ResourceNotFoundException("COURSE_NOT_FOUND", "Course", courseId);
    }

    public static ResourceNotFoundException unitNotFound(String unitId) {
        return new ResourceNotFoundException("UNIT_NOT_FOUND", "Unit", unitId);
    }

    public static ResourceNotFoundException lessonNotFound(String lessonId) {
        return new ResourceNotFoundException("LESSON_NOT_FOUND", "Lesson", lessonId);
    }

    public static ResourceNotFoundException exerciseNotFound(String exerciseId) {
        return new ResourceNotFoundException("EXERCISE_NOT_FOUND", "Exercise", exerciseId);
    }

    public static ResourceNotFoundException progressNotFound(String userId, String language) {
        return new ResourceNotFoundException("PROGRESS_NOT_FOUND", "Progress", userId + ":" + language);
    }
}
