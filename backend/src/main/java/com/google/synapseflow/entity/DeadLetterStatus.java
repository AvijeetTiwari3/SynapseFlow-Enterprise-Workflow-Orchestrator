package com.google.synapseflow.entity;

public enum DeadLetterStatus {
    QUARANTINED,
    REPLAYING,
    REPLAYED,
    DISCARDED
}
