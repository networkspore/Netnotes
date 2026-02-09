package io.netnotes.system.cmd;

public class CommandResult {
    
    public enum Action {
        NONE,
        PUSH,
        POP,
        REPLACE
    }
    
    private final Action action;
    private final SystemProcess targetProcess;
    private final String message;
    
    private CommandResult(Action action, SystemProcess process, String message) {
        this.action = action;
        this.targetProcess = process;
        this.message = message;
    }
    
    public static CommandResult none() {
        return new CommandResult(Action.NONE, null, null);
    }
    
    public static CommandResult none(String message) {
        return new CommandResult(Action.NONE, null, message);
    }
    
    public static CommandResult push(SystemProcess process) {
        return new CommandResult(Action.PUSH, process, null);
    }
    
    public static CommandResult pop() {
        return new CommandResult(Action.POP, null, null);
    }
    
    public static CommandResult replace(SystemProcess process) {
        return new CommandResult(Action.REPLACE, process, null);
    }
    
    public boolean changesProcess() {
        return action != Action.NONE;
    }
    
    public Action getAction() { return action; }
    public SystemProcess getTargetProcess() { return targetProcess; }
    public String getMessage() { return message; }
}