package org.pintoim.pinto.consolehandler;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.Tuple;
import org.pintoim.pinto.Utils;
import org.pintoim.pinto.consolehandler.commands.Ban;
import org.pintoim.pinto.consolehandler.commands.BanIP;
import org.pintoim.pinto.consolehandler.commands.ChangePassword;
import org.pintoim.pinto.consolehandler.commands.CreateAccount;
import org.pintoim.pinto.consolehandler.commands.GetIP;
import org.pintoim.pinto.consolehandler.commands.Kick;
import org.pintoim.pinto.consolehandler.commands.KickIP;
import org.pintoim.pinto.consolehandler.commands.ListUsers;
import org.pintoim.pinto.consolehandler.commands.Notification;
import org.pintoim.pinto.consolehandler.commands.Reload;
import org.pintoim.pinto.consolehandler.commands.Stop;
import org.pintoim.pinto.consolehandler.commands.Unban;
import org.pintoim.pinto.consolehandler.commands.UnbanIP;
import org.pintoim.pinto.consolehandler.commands.client.Add;
import org.pintoim.pinto.consolehandler.commands.client.Remove;

public class ConsoleHandler {
	protected PintoServer server;
    public final ArrayList<ConsoleCommand> commands = new ArrayList<ConsoleCommand>();
    public final ArrayList<ConsoleVariable> variables = new ArrayList<ConsoleVariable>();

    public ConsoleHandler(PintoServer server, boolean isConsole) {
    	this.server = server;

    	if (isConsole) {
        	this.commands.add(new Ban());
        	this.commands.add(new BanIP());
        	this.commands.add(new GetIP());
        	this.commands.add(new Kick());
        	this.commands.add(new KickIP());
        	this.commands.add(new Reload());
        	this.commands.add(new Unban());
        	this.commands.add(new UnbanIP());
        	this.commands.add(new Stop());
        	this.commands.add(new ListUsers());
        	this.commands.add(new Notification());
        	this.commands.add(new ChangePassword());
        	this.commands.add(new CreateAccount());
    	} else {
    		this.commands.add(new Add());
    		this.commands.add(new Remove());
    	}
    }

	public void handleInput(String input, ConsoleCaller caller) {
		if (input == null || input.isEmpty()) return;
    	Tuple<String, String[]> inputParsed = this.parseInput(input.trim());
    	
    	if (inputParsed.item1.equalsIgnoreCase("help")) {
        	if (inputParsed.item2.length > 0) {
        		String helpArg = inputParsed.item2[0];
        		if (Utils.isNumeric(helpArg, false)) {
        			this.doHelpCommand(Integer.valueOf(helpArg), null, caller);
        		} else {
        			this.doHelpCommand(0, helpArg, caller);
        		}
        	} else {
        		this.doHelpCommand(0, null, caller);
        	}
        } else if (inputParsed.item1.equalsIgnoreCase("helpvar")) {
        	if (inputParsed.item2.length > 0) {
        		String helpArg = inputParsed.item2[0];
        		if (Utils.isNumeric(helpArg, false)) {
        			this.doHelpVarCommand(Integer.valueOf(helpArg), null, caller);
        		} else {
        			this.doHelpVarCommand(0, helpArg, caller);
        		}
        	} else {
        		this.doHelpVarCommand(0, null, caller);
        	}
        } else {
    		this.processCommand(inputParsed.item1, inputParsed.item2, caller);
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

    public boolean processCommand(String cmd, String[] cmdArgs, ConsoleCaller caller) {
        ConsoleCommand conCmd = getCommandByName(cmd);
        ConsoleVariable conVar = getVariableByName(cmd);

        if (conCmd != null) {
        	if (!(cmdArgs.length < conCmd.getMinArgsCount() || cmdArgs.length > conCmd.getMaxArgsCount())) {
            	try {
            		conCmd.execute(this.server, caller, cmdArgs);
            	} catch (Exception ex) {
            		caller.sendMessage("An error has occured whilst executing the specified command!");
            		PintoServer.logger.error("Unable to execute \"" + cmd + "\": " + 
            				Utils.getThrowableStackTraceAsStr(ex));
            	}
            } else {
            	caller.sendMessage("Usage: " + conCmd.getUsage());
            }
            return true;
        } else if (conVar != null) {
        	if (cmdArgs.length < 1) {
            	this.printConVarHelp(conVar, caller);
            } else {
                Object argsValue = ConsoleVariableTypeTools.getStringAsVarType(cmdArgs[0], conVar.getValueType());

                if (argsValue != null) {
                	conVar.setValue(argsValue);
                } else {
                	caller.sendMessage("Invalid value provided for this variable!");
                }
            }
            
            return true;
        }
        
        caller.sendMessage("Unrecognized command or variable \"" + cmd + "\"!");	
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

    public void printConCmdHelp(ConsoleCommand conCmd, ConsoleCaller caller) {
    	caller.sendMessage("\"" + conCmd.getName() + "\" " + 
        		conCmd.getMinArgsCount() + "/" + 
        		conCmd.getMaxArgsCount()+ " (" + 
        		conCmd.getUsage() + ") - " + 
        		conCmd.getDescription());
    }

    public void printConVarHelp(ConsoleVariable conVar, ConsoleCaller caller) {
    	caller.sendMessage("\"" + conVar.getName() + "\" \"" + 
        		conVar.getValue() + "\" (" + 
        		conVar.getValueType() + ") - " + 
        		conVar.getDescription());
    }
    
    public void doHelpCommand(int pageNumber, String cmd, ConsoleCaller caller) {
    	if (cmd != null) {
            ConsoleCommand conCmd = this.getCommandByName(cmd);
            
            if (conCmd != null) {
            	this.printConCmdHelp(conCmd, caller);
            } else {
            	caller.sendMessage("Unrecognized command \"" + cmd + "\"!");	
            }
    	} else {
    		if (this.commands.size() < 1) {
    			caller.sendMessage("There are no commands available!");
    			return;
    		} else if (this.commands.size() >= 10) {
    			int commandsProccessed = 0;
    			for (int commandIndex = 10 * pageNumber; 
    				commandIndex < this.commands.size(); 
    				commandIndex++) {
    				commandsProccessed++;
    				if (commandsProccessed > 10) break;
    				
    				ConsoleCommand command = this.commands.get(commandIndex);
    				this.printConCmdHelp(command, caller);
    			}
    		} else {
                for (ConsoleCommand conCmd : this.commands) {
                	this.printConCmdHelp(conCmd, caller);
                }
    		}
    		
    		caller.sendMessage("Help format: name minargs/maxargs (usage) - description");
    		caller.sendMessage("Viewing page " + pageNumber + "/" + this.commands.size() / 10);
    	}
    }
    
    public void doHelpVarCommand(int pageNumber, String var, ConsoleCaller caller) {
    	if (var != null) {
            ConsoleVariable conVar = this.getVariableByName(var);
            
            if (conVar != null) {
            	this.printConVarHelp(conVar, caller);
            } else {
            	caller.sendMessage("Unrecognized variable \"" + var + "\"!");	
            }
    	} else {
    		if (this.variables.size() < 1) {
    			caller.sendMessage("There are no variables available!");
    			return;
    		} else if (this.variables.size() >= 10) {
    			int variablesProccessed = 0;
    			for (int variableIndex = 10 * pageNumber; 
    					variableIndex < this.commands.size(); 
    					variableIndex++) {
    				variablesProccessed++;
    				if (variablesProccessed > 10) break;
    				
    				ConsoleVariable variable = this.variables.get(variableIndex);
    				this.printConVarHelp(variable, caller);
    			}
    		} else {
                for (ConsoleVariable conVar : this.variables) {
                	this.printConVarHelp(conVar, caller);
                }
    		}
    		
    		caller.sendMessage("Help format: name value (value_type) - description");
    		caller.sendMessage("Viewing page " + pageNumber + "/" + this.variables.size() / 10);
    	}
    }
}