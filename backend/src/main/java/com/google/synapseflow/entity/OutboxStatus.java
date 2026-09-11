package com.google.synapseflow.entity;

public enum OutboxStatus {
    PENDING,
    DISPATCHING,
    SENT,
    FAILED,
    DEAD_LETTERED
}
