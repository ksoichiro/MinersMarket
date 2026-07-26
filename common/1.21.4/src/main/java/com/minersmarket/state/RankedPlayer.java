package com.minersmarket.state;

/**
 * One row of a sales ranking. Ranks are 1-based and shared by players with equal
 * amounts, so a tie for first produces two entries with rank 1 and the next
 * distinct amount is rank 3.
 */
public record RankedPlayer(int rank, String playerName, long salesAmount) {
}
