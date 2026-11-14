package com.tip_video_ludoteca.games;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateGameCustomizationRequest(
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color de fondo no es válido.")
        String backgroundColor,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color de acento no es válido.")
        String accentColor,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color del texto no es válido.")
        String textColor,
        @NotBlank
        @Pattern(regexp = "^(SYSTEM|SERIF|MONO|ARIAL|VERDANA|TREBUCHET|GEORGIA|COURIER)$", message = "La fuente seleccionada no es válida.")
        String fontFamily,
        @NotBlank
        @Pattern(regexp = "^(SOLID|GRADIENT)$", message = "El estilo de fondo no es válido.")
        String backgroundStyle) {
}
