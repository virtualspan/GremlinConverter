package net.virtualspan.model;

import java.util.Map;
import java.util.Set;

public record SpriteResult(
        String spriteMap,
        String introSprite,
        String outroSprite,
        Set<String> skip,
        Map<String, Integer> values) {
}
