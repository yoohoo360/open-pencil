package cn.jongwong.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssPresignResponse {

    private String url;
    private Map<String, String> headers;
    private String key;
}
