package me.vlod.pinto.logger;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.Utils;
import me.vlod.pinto.configuration.MainConfig;

public class Logger {
	public final ArrayList<Delegate> targets = new ArrayList<Delegate>();
	
	public void log(String header, String message, Color color, Object... format) {
		message = String.format(message, (Object[])format);		
		
		LocalDateTime localDateTime = LocalDateTime.now();
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String text = String.format("%s [%s] [%s] %s", 
				dateTimeFormatter.format(localDateTime), 
				Thread.currentThread().getName(),
				header, message);
		
		for (Delegate target : this.targets) {
			target.call(text, color);
		}
	}
	
	public void log(String header, String message, Object... format) {
		this.log(header, message, Color.black, format);
	}
	
	public void logMultiLine(String header, String message, Color color, Object... format) {
		message = String.format(message, (Object[])format);
		String[] logs = message.split("\n");
		
		for (String log : logs) {
			this.log(header, log.replace("\n", "").replace("\r", ""), color);
		}
	}
	
	public void logMultiLine(String header, String message, Object... format) {
		this.logMultiLine(header, message, Color.black, format);
	}
	
	public void level(LogLevel level, String message, Object... format) {
		if (level == LogLevel.ERROR || 
			level == LogLevel.SEVERE || 
			level == LogLevel.FATAL) {
			this.log(level.toString(), message, Color.red, format);
		} else if (level == LogLevel.WARN) {
			this.log(level.toString(), message, new Color(246, 190, 0), format);
		} else {
			if (MainConfig.instance != null && 
				!MainConfig.instance.showVerboseLogs && 
				level == LogLevel.VERBOSE) return;
			this.log(level.toString(), message, Color.black, format);
		}
	}
	
	public void levelMultiLine(LogLevel level, String message, Object... format) {
		if (level == LogLevel.ERROR || 
			level == LogLevel.SEVERE || 
			level == LogLevel.FATAL) {
			this.logMultiLine(level.toString(), message, Color.red, format);
		} else if (level == LogLevel.WARN) {
			this.logMultiLine(level.toString(), message, new Color(246, 190, 0), format);
		} else {
			if (MainConfig.instance != null && 
				!MainConfig.instance.showVerboseLogs && 
				level == LogLevel.VERBOSE) return;
			this.logMultiLine(level.toString(), message, Color.black, format);
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
		this.levelMultiLine(LogLevel.SEVERE, Utils.getThrowableStackTraceAsStr(throwable));
	}
}
