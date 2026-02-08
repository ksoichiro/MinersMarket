package com.minersmarket.state;

import java.util.UUID;

public record FinishedPlayer(UUID playerId, String playerName, int finishTimeTicks) {
}
