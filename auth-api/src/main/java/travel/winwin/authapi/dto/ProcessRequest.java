package travel.winwin.authapi.dto;

import jakarta.validation.constraints.NotNull;

/** Body of /api/process: { "text": "..." }. */
public record ProcessRequest(@NotNull String text) {
}
