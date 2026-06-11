package com.cmdai;

/**
 * Represents a parsed command result from the LLM response.
 */
public class CommandResult {

    private String command;
    private String explanation;
    private String bash;
    private String powershell;
    private String cmd;
    private String fish;
    private String[] warnings;

    public CommandResult() {}

    public String getCommand()        { return command; }
    public void setCommand(String c)  { this.command = c; }
    public String getExplanation()    { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }
    public String getBash()           { return bash; }
    public void setBash(String b)     { this.bash = b; }
    public String getPowershell()     { return powershell; }
    public void setPowershell(String p) { this.powershell = p; }
    public String getCmd()            { return cmd; }
    public void setCmd(String c)      { this.cmd = c; }
    public String getFish()           { return fish; }
    public void setFish(String f)     { this.fish = f; }
    public String[] getWarnings()     { return warnings; }
    public void setWarnings(String[] w) { this.warnings = w; }
}
