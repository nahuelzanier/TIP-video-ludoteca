package com.tip_video_ludoteca.users;

import jakarta.validation.constraints.Size;

public record UpdateDescriptionRequest(
        @Size(
            max = UserProfileService.MAX_DESCRIPTION_LENGTH,
            message = "La descripción no puede superar los "
                    + UserProfileService.MAX_DESCRIPTION_LENGTH + " caracteres.")
        String description) {
}
