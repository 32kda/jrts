package com.jrts.config;

import java.util.List;

/**
 * Audio section parsed from TOML [audio] block.
 */
public record AudioSection(
        List<String> voiceSelect,
        List<String> voiceMove,
        List<String> voiceAttack,
        List<String> voiceDie,
        List<String> voiceFeedback,
        List<String> voiceComment,
        String crushSound) {
}
