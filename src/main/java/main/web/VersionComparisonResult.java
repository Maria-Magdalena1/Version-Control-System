package main.web;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class VersionComparisonResult {

    private String versionNumber1;
    private String author1;
    private LocalDateTime createdAt1;
    private String status1;

    private String versionNumber2;
    private String author2;
    private LocalDateTime createdAt2;
    private String status2;

    private List<String> addedLines;
    private List<String> removedLines;
}
