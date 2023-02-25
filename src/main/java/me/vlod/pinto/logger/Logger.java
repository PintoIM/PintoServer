package me.vlod.pinto.logger;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.Utils;

public class Logger {
	public final ArrayList<Delegate> targets = new ArrayList<Delegate>();
	
	public void log(String header, String message, Color color, Object... format) {
		message = String.format(message, (Object[])format);		
		
		LocalDateTime localDateTime = LocalDateTime.now();
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String text = dateTimeFormatter.format(localDateTime) + " [" + header + "] " + message;
		
		for (Delegate target : this.targets) {
			target.call(text, color);
		}
	}
	
	public void log(String header, String message, Object... format) {
		this.log(header, message, Color.black, format);
	}
	
	public void level(LogLevel level, String message, Object... format) {
		if (level == LogLevel.ERROR || 
			level == LogLevel.SEVERE || 
			level == LogLevel.FATAL) {
			this.log(level.toString(), message, Color.red, format);
		} else if (level == LogLevel.WARN) {
			this.log(level.toString(), message, new Color(246, 190, 0), format);
		} else {
			this.log(level.toString(), message, Color.black, format);
		}
	}
	
	public void verbose(String message, Object... format) {
		this.level(LogLevel.VERBOSE, message, format);
	}
	
	public void info(String message, Object... format) {
		this.level(LogLevel.INFO, message, format);
	}
	
	public void warn(String message, Object... format) {
		this.level(LogLevel.WARN, message, format);
	}
	
	public void error(String message, Object... format) {
		this.level(LogLevel.ERROR, message, format);
	}
	
	public void severe(String message, Object... format) {
		this.level(LogLevel.SEVERE, message, format);
	}
	
	public void fatal(String message, Object... format) {
		this.level(LogLevel.FATAL, message, format);
	}
	
	public void throwable(Throwable throwable) {
		this.level(LogLevel.ERROR, "An throwable of type " + throwable.getClass().getName() + 
				" has occured: " + throwable.getMessage());
		this.level(LogLevel.ERROR, "Throwable stacktrace: " + Utils.getThrowableStackTraceAsStr(throwable));
	}
}
