package ru.onelove.command;

public interface CommandProvider {
    Command command(String alias);
}
