package me.vlod.pinto.consolehandler;

public interface ConsoleVariable extends ConsoleCallable {
    public ConsoleVariableType getValueType();
    public Object getValue();
    public void setValue(Object value);
}