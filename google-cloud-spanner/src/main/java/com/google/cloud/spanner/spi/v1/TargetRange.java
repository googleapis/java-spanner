package com.google.cloud.spanner.spi.v1;

import com.google.protobuf.ByteString;

public class TargetRange {
  public ByteString start;
  public ByteString limit;
  public boolean approximate;

  public TargetRange(ByteString start, ByteString limit, boolean approximate) {
    this.start = start;
    this.limit = limit;
    this.approximate = approximate;
  }

  public boolean isPoint() {
    return limit.isEmpty();
  }

  /**
   * Merges another TargetRange into this one. The resulting range will be the union of the two.
   * This logic is a direct port of the C++ implementation in `recipe.cc`.
   */
  public void mergeFrom(TargetRange other) {
    if (ByteString.unsignedLexicographicalComparator().compare(other.start, this.start) < 0) {
      this.start = other.start;
    }
    if (other.isPoint()
        && ByteString.unsignedLexicographicalComparator().compare(other.start, this.limit) >= 0) {
      this.limit = SsFormat.makePrefixSuccessor(other.start);
    } else if (ByteString.unsignedLexicographicalComparator().compare(other.limit, this.limit)
        > 0) {
      this.limit = other.limit;
    }
    this.approximate |= other.approximate;
  }
}
