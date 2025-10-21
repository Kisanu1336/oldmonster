package ru.onelove.command.impl;

import ru.onelove.command.Parameters;
import ru.onelove.command.ParametersFactory;

import java.util.Arrays;

public class ParametersFactoryImpl implements ParametersFactory {

    @Override
    public Parameters createParameters(String message, String delimiter) {
        return new ParametersImpl(message.split(delimiter));
    }
}
