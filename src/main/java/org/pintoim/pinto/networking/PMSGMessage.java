package org.pintoim.pinto.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.pintoim.pinto.Utils;

public class PMSGMessage {
    public Map<String, String> headers;
    public byte[] data;

    public PMSGMessage() {
        this.headers = new HashMap<>();
        this.data = new byte[0];
    }

    public PMSGMessage(String body) {
    	this.headers = new HashMap<>();
        this.headers.put("Content-Type", "pinto/text");
        this.data = body.getBytes(StandardCharsets.UTF_16BE);
    }

    public PMSGMessage(byte[] data, boolean isImage) {
    	this.headers = new HashMap<>();
    	this.headers.put("Content-Type", "pinto/" + (isImage ? "file-plain" : "file-image"));
        this.data = data;
    }

    private static byte[] encodeGZIP(byte[] data) throws IOException {
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
    	gzipOutputStream.write(data);
    	gzipOutputStream.flush();
    	gzipOutputStream.close();
    	return outputStream.toByteArray();
    }
    
    private static byte[] decodeGZIP(byte[] data) throws IOException {
    	GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(data));
    	byte[] decoded = Utils.readNBytes(inputStream, Integer.MAX_VALUE);
    	inputStream.close();
    	return decoded;
    }
    
    public byte[] encode() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            String header = entry.getKey() + ": " + entry.getValue() + "\r\n";
            stream.write(header.getBytes(StandardCharsets.US_ASCII));
        }

        stream.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        stream.write(encodeGZIP(this.data));

        return stream.toByteArray();
    }

    private static byte[] readToEnd(InputStream stream) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readCount;

        while ((readCount = stream.read(buffer, 0, buffer.length)) > 0) {
            data.write(buffer, 0, readCount);
        }

        return data.toByteArray();
    }

    private static String readLine(InputStream stream) throws IOException {
        StringBuilder line = new StringBuilder();
        int prevC = 0;
        int c;

        while ((c = stream.read()) != -1 && (c != '\n' || prevC != '\r')) {
            prevC = c;
            line.append((char) c);
        }

        return line.toString().trim();
    }

    public static PMSGMessage decode(byte[] payload) throws IOException {
        InputStream stream = new ByteArrayInputStream(payload);
        PMSGMessage message = new PMSGMessage();
        message.headers = new HashMap<>();

        String line;
        while ((line = readLine(stream)).length() > 0) {
            String[] parts = line.split(":");
            message.headers.put(parts[0], parts[1].trim());
        }
        message.data = decodeGZIP(readToEnd(stream));

        return message;
    }
}
