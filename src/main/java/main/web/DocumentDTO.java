package main.web;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentDTO {

    @Column(nullable = false,unique = true,length = 150)
    @Size(min=3,max=150)
    @NotBlank
    private String title;

    @Size(max = 500)
    private String description;

    @Size(max = 10000)
    @Column(columnDefinition = "TEXT")
    private String content;
    private String filePath;
}
