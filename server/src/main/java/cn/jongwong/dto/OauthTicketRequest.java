package cn.jongwong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OauthTicketRequest {

    @NotBlank(message = "OAuth ticket is required")
    private String ticket;
}
