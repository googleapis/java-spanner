package com.google.cloud.spanner;

abstract class InternalOption {
  abstract void appendToOptions(Options options);
}
