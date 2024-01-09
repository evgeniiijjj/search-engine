package searchengine.dto;

import searchengine.entities.Index;

public record IndexLemmaMining(
        Index index,
        LemmaMining meaning
) {
}
