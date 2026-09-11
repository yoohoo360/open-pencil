package cn.jongwong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssPresignDownloadRequest {

    @NotBlank(message = "Path is required")
    private String path;
}
