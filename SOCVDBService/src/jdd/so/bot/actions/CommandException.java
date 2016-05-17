package jdd.so.bot.actions;

/**
 * The exception thrown by BotCommand if it ca not
 * understand or execute the command
 * @author Petter Friberg
 *
 */
public class CommandException extends Exception {
	
	private static final long serialVersionUID = 6459410250943426135L;

	public CommandException(String message){
		super(message);
	}
	
	public CommandException(String message, Throwable rootCause){
		super(message,rootCause);
	}
}
