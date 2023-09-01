package me.vlod.pinto;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import me.vlod.pinto.configuration.MainConfig;

public class PintoHttpServer implements Runnable {
	private HttpServer server;
	public boolean isStarted;
	public Exception startException;
	
	@Override
	public void run() {
		try {
			PintoServer.logger.info("Starting HTTP server on %s:%d", MainConfig.instance.listenIP, 
					MainConfig.instance.listenPort + 10);
			
			this.server = HttpServer.create(new InetSocketAddress(
					InetAddress.getByName(MainConfig.instance.listenIP), 
					MainConfig.instance.listenPort + 10), 0);
			
			File htmlDir = new File("html");
			if (!htmlDir.exists() || !htmlDir.isDirectory()) {
				if (htmlDir.exists()) htmlDir.delete();
				htmlDir.mkdir();
				Files.copy(PintoHttpServer.class.getResourceAsStream("/rules.html"), 
						Paths.get("html", "rules.html"));
				Files.copy(PintoHttpServer.class.getResourceAsStream("/welcome.html"), 
						Paths.get("html", "welcome.html"));
			}
			
			for (String file : MainConfig.instance.filesToServeOnHTTP.keySet()) {
				String contentType = MainConfig.instance.filesToServeOnHTTP.get(file);
				PintoServer.logger.info("Registering HTTP context for %s (%s)", file, contentType);
				this.server.createContext("/" + file, new HttpFileServe(file, contentType));
			}
			
			this.server.setExecutor(null);
			this.server.start();
			
			this.isStarted = true;
			PintoServer.logger.info("Started HTTP server! Listening...");
		} catch (Exception ex) {
			this.startException = ex;
			this.isStarted = true;
		}
	}
	
	public class HttpFileServe implements HttpHandler {
		private String file;
		private String contentType;
		
		public HttpFileServe(String file, String contentType) {
			this.file = file;
			this.contentType = contentType;
		}
		
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			File file = Paths.get("html", this.file).toFile();
			
			if (!file.exists()) {
				this.sendError(404, "Page Not Found", "The requested resource couldn't be found", exchange);
			} else if (!file.canRead()) {
				this.sendError(403, "Forbidden", "You aren't allowed to access this resource", exchange);
			} else {
				this.send200(Files.readAllBytes(file.toPath()), exchange);
			}
		}
		
		private void sendError(int errorCode, String errorDescription, 
				String pageBody, HttpExchange exchange) throws IOException {
			String page = String.format(
				"<!Doctype HTML>"
				+ "<html>"
				+ "<body>"
				+ "<h1>%d %s</h1>"
				+ "<p>%s</p>"
				+ "<hr>"
				+ "<p>PintoHttpServer</p>"
				+ "</body>"
				+ "</html>", 
				errorCode, 
				errorDescription, 
				pageBody
			);
			byte[] pageData = page.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/html; encoding=utf-8");
			exchange.getResponseHeaders().set("Server", "PintoHttpServer");
			exchange.sendResponseHeaders(errorCode, pageData.length);
			OutputStream outputStream = exchange.getResponseBody();
			outputStream.write(pageData);
			outputStream.close();
		}
		
		private void send200(byte[] fileData, HttpExchange exchange) throws IOException {
			exchange.getResponseHeaders().set("Content-Type", this.contentType);
			exchange.getResponseHeaders().set("Server", "PintoHttpServer");
			exchange.sendResponseHeaders(200, fileData.length);
			OutputStream outputStream = exchange.getResponseBody();
			outputStream.write(fileData);
			outputStream.close();
		}
	}
}
