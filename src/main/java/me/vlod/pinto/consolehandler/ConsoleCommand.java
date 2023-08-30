package me.vlod.pinto.consolehandler;

import me.vlod.pinto.PintoServer;

public interface ConsoleCommand extends ConsoleCallable {
    public String getUsage();
    public int getMinArgsCount();
    public int getMaxArgsCount();
    public void execute(PintoServer server, ConsoleCaller caller, String[] args) throws Exception;
}