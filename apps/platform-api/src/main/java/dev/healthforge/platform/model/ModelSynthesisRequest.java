package dev.healthforge.platform.model;

import dev.healthforge.platform.answer.GroundedAnswerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ModelSynthesisRequest(@NotNull @Valid GroundedAnswerResponse evidencePacket) {}
