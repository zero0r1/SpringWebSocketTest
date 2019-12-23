package com.test.websocket.websocketTest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * @author shawn
 */
@EnableWebSocket
@SpringBootApplication
public class WebsocketTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsocketTestApplication.class, args);
	}

}
