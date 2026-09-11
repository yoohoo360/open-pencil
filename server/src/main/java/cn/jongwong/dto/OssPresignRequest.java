package cn.jongwong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssPresignRequest {

    private String path;

    @NotBlank(message = "File name is required")
    private String fileName;

    private String contentType;
}
