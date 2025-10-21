package ru.onelove.command;

public interface ParametersFactory {
    Parameters createParameters(String message, String delimiter);
}
