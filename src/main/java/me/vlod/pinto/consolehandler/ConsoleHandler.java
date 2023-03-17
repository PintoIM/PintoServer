package me.vlod.pinto.consolehandler;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.Tuple;
import me.vlod.pinto.Utils;
import me.vlod.pinto.consolehandler.commands.BanCMD;
import me.vlod.pinto.consolehandler.commands.BanIPCMD;
import me.vlod.pinto.consolehandler.commands.GetIPCMD;
import me.vlod.pinto.consolehandler.commands.KickCMD;
import me.vlod.pinto.consolehandler.commands.KickIPCMD;
import me.vlod.pinto.consolehandler.commands.ReloadCMD;
import me.vlod.pinto.consolehandler.commands.UnbanCMD;
import me.vlod.pinto.consolehandler.commands.UnbanIPCMD;

public class ConsoleHandler {
	protected PintoServer server;
	protected ConsoleCaller caller;
    public final ArrayList<ConsoleCommand> commands = new ArrayList<ConsoleCommand>();
    public final ArrayList<ConsoleVariable> variables = new ArrayList<ConsoleVariable>();

    public ConsoleHandler(PintoServer server, ConsoleCaller caller) {
    	this.server = server;
    	this.caller = caller;
    	this.commands.add(new BanCMD());
    	this.commands.add(new BanIPCMD());
    	this.commands.add(new GetIPCMD());
    	this.commands.add(new KickCMD());
    	this.commands.add(new KickIPCMD());
    	this.commands.add(new ReloadCMD());
    	this.commands.add(new UnbanCMD());
    	this.commands.add(new UnbanIPCMD());
    }

	public void handleInput(String input) {
		if (input == null || input.isEmpty()) return;
    	Tuple<String, String[]> inputParsed = this.parseInput(input.trim());
    	
    	if (inputParsed.item1.equalsIgnoreCase("help")) {
        	if (inputParsed.item2.length > 0) {
        		String helpArg = inputParsed.item2[0];
        		if (Utils.isNumeric(helpArg, false)) {
        			this.doHelpCommand(Integer.valueOf(helpArg), null);
        		} else {
        			this.doHelpCommand(0, helpArg);
        		}
        	} else {
        		this.doHelpCommand(0, null);
        	}
        } else if (inputParsed.item1.equalsIgnoreCase("helpvar")) {
        	if (inputParsed.item2.length > 0) {
        		String helpArg = inputParsed.item2[0];
        		if (Utils.isNumeric(helpArg, false)) {
        			this.doHelpVarCommand(Integer.valueOf(helpArg), null);
        		} else {
        			this.doHelpVarCommand(0, helpArg);
        		}
        	} else {
        		this.doHelpVarCommand(0, null);
        	}
        } else {
    		this.processCommand(inputParsed.item1, inputParsed.item2);
    	}
	}

    protected Tuple<String, String[]> parseInput(String input) {
        String cmd = null;
        String[] cmdArgs = new String[0];

        if (input.contains(String.valueOf((char)0x20))) {
            String[] splittedInput = Utils.splitBySpace(input);
            cmd = splittedInput[0];
            cmdArgs = (String[]) ArrayUtils.subarray(splittedInput, 1, splittedInput.length);

            for (int cmdArgIndex = 0; cmdArgIndex < cmdArgs.length; cmdArgIndex++) {
                String cmdArg = cmdArgs[cmdArgIndex];

                if (cmdArg.startsWith("\"")) {
                	cmdArg = cmdArg.substring(1, cmdArg.length() - 1);
                }
                    
                if (cmdArg.endsWith("\"")) {
                	cmdArg = cmdArg.substring(0, cmdArg.length() - 1);
                }
                
                cmdArgs[cmdArgIndex] = cmdArg;
            }
        } else {
        	cmd = input;
        }

        return new Tuple<String, String[]>(cmd, cmdArgs);
    }

    public boolean processCommand(String cmd, String[] cmdArgs) {
        ConsoleCommand conCmd = getCommandByName(cmd);
        ConsoleVariable conVar = getVariableByName(cmd);

        if (conCmd != null) {
        	if (!(cmdArgs.length < conCmd.getMinArgsCount() || cmdArgs.length > conCmd.getMaxArgsCount())) {
            	try {
            		conCmd.execute(this.server, this.caller, cmdArgs);
            	} catch (Exception ex) {
            		this.caller.sendMessage("An error has occured whilst executing the specified command!");
            		PintoServer.logger.error("Unable to execute \"" + cmd + "\": " + 
            				Utils.getThrowableStackTraceAsStr(ex));
            	}
            } else {
            	this.caller.sendMessage("Usage: " + conCmd.getUsage());
            }
            return true;
        } else if (conVar != null) {
        	if (cmdArgs.length < 1) {
            	printConVarHelp(conVar);
            } else {
                Object argsValue = ConsoleVariableTypeTools.getStringAsVarType(cmdArgs[0], conVar.getValueType());

                if (argsValue != null) {
                	conVar.setValue(argsValue);
                } else {
                	this.caller.sendMessage("Invalid value provided for this variable!");
                }
            }
            
            return true;
        }
        
        this.caller.sendMessage("Unrecognized command or variable \"" + cmd + "\"!");	
        return false;
    }

    public ConsoleCommand getCommandByName(String name) {
        for (ConsoleCommand conCmd : this.commands) {
            if (conCmd.getName().equalsIgnoreCase(name) || 
            	Arrays.asList(conCmd.getAliases())
            		.stream().anyMatch(x -> x.equalsIgnoreCase(name))) {
            	return conCmd;
            }
        }

        return null;
    }

    public ConsoleVariable getVariableByName(String name) {
        for (ConsoleVariable conVar : this.variables) {
            if (conVar.getName().equalsIgnoreCase(name) ||
            	Arrays.asList(conVar.getAliases())
            		.stream().anyMatch(x -> x.equalsIgnoreCase(name))) {
            	return conVar;
            }
        }

        return null;
    }

    public void printConCmdHelp(ConsoleCommand conCmd) {
    	this.caller.sendMessage("\"" + conCmd.getName() + "\" " + 
        		conCmd.getMinArgsCount() + "/" + 
        		conCmd.getMaxArgsCount()+ " (" + 
        		conCmd.getUsage() + ") - " + 
        		conCmd.getDescription());
    }

    public void printConVarHelp(ConsoleVariable conVar) {
    	this.caller.sendMessage("\"" + conVar.getName() + "\" \"" + 
        		conVar.getValue() + "\" (" + 
        		conVar.getValueType() + ") - " + 
        		conVar.getDescription());
    }
    
    public void doHelpCommand(int pageNumber, String cmd) {
    	if (cmd != null) {
            ConsoleCommand conCmd = this.getCommandByName(cmd);
            
            if (conCmd != null) {
            	this.printConCmdHelp(conCmd);
            } else {
            	this.caller.sendMessage("Unrecognized command \"" + cmd + "\"!");	
            }
    	} else {
    		if (this.commands.size() < 1) {
    			this.caller.sendMessage("There are no commands available!");
    			return;
    		} else if (this.commands.size() >= 10) {
    			int commandsProccessed = 0;
    			for (int commandIndex = 10 * pageNumber; 
    				commandIndex < this.commands.size(); 
    				commandIndex++) {
    				commandsProccessed++;
    				if (commandsProccessed > 10) break;
    				
    				ConsoleCommand command = this.commands.get(commandIndex);
    				this.printConCmdHelp(command);
    			}
    		} else {
                for (ConsoleCommand conCmd : this.commands) {
                	this.printConCmdHelp(conCmd);
                }
    		}
    		
    		this.caller.sendMessage("Help format: name minargs/maxargs (usage) - description");
    		this.caller.sendMessage("Viewing page " + pageNumber + "/" + this.commands.size() / 10);
    	}
    }
    
    public void doHelpVarCommand(int pageNumber, String var) {
    	if (var != null) {
            ConsoleVariable conVar = this.getVariableByName(var);
            
            if (conVar != null) {
            	this.printConVarHelp(conVar);
            } else {
            	this.caller.sendMessage("Unrecognized variable \"" + var + "\"!");	
            }
    	} else {
    		if (this.variables.size() < 1) {
    			this.caller.sendMessage("There are no variables available!");
    			return;
    		} else if (this.variables.size() >= 10) {
    			int variablesProccessed = 0;
    			for (int variableIndex = 10 * pageNumber; 
    					variableIndex < this.commands.size(); 
    					variableIndex++) {
    				variablesProccessed++;
    				if (variablesProccessed > 10) break;
    				
    				ConsoleVariable variable = this.variables.get(variableIndex);
    				this.printConVarHelp(variable);
    			}
    		} else {
                for (ConsoleVariable conVar : this.variables) {
                	this.printConVarHelp(conVar);
                }
    		}
    		
    		this.caller.sendMessage("Help format: name value (value_type) - description");
    		this.caller.sendMessage("Viewing page " + pageNumber + "/" + this.variables.size() / 10);
    	}
    }
}