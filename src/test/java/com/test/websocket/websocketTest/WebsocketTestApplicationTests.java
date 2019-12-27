package com.test.websocket.websocketTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;


@SpringBootTest(classes = WebsocketTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebsocketTestApplicationTests {

    static final String WEBSOCKET_URI = "ws://localhost:39025/gs-guide-websocket";
    static final String WEBSOCKET_TOPIC = "/user/exchange/exchange_message/nxd";

    @Test
    public void shouldReceiveAMessageFromTheServer() throws Exception {

        WebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
        //模拟用户数量
        List<Transport> transports = new ArrayList<>(1);
        transports.add((Transport) new WebSocketTransport(simpleWebSocketClient));

        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        String userId = "spring-" + ThreadLocalRandom.current().nextInt(1, 99);
        StompSessionHandler sessionHandler = new MyStompSessionHandler(userId);
        StompSession session = stompClient.connect(WEBSOCKET_URI, sessionHandler).get();
        Integer item = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            ClientMessage msg = new ClientMessage(userId, item++);
            System.out.print(userId + " >> ");
            msg.setFrom(userId);
            msg.setText(String.format("%s%s", userId, item.toString()));
            session.send(WEBSOCKET_TOPIC, msg);
            Thread.sleep(5000);
            int a = 1;
        }
        //批量订阅
//        for (; ; ) {
//        System.out.print(userId + " >> ");
//        System.out.flush();
//        Thread.sleep(10);
//        session.subscribe(WEBSOCKET_TOPIC, new MyStompSessionHandler());
//        ClientMessage msg = new ClientMessage(userId, item++);
//        msg.setFrom(userId);
//        msg.setText(String.format("%s%s", userId, item.toString()));
//        while (true) {
//            Thread.sleep(5000);
//            session.send(WEBSOCKET_TOPIC, msg);
//        }
//        int a = 1;
//        }
    }

    static public class MyStompSessionHandler extends StompSessionHandlerAdapter {
        private String userId;

        public MyStompSessionHandler(String userId) {
            this.userId = userId;
        }

        private void showHeaders(StompHeaders headers) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                System.err.print("  " + e.getKey() + ": ");
                boolean first = true;
                for (String v : e.getValue()) {
                    if (!first) System.err.print(", ");
                    System.err.print(v);
                    first = false;
                }
                System.err.println();
            }
        }

        private void sendJsonMessage(StompSession session) {
            ClientMessage msg = new ClientMessage(userId, "hello from spring");
            session.send(WEBSOCKET_TOPIC, msg);
        }

        private void subscribeTopic(String topic, StompSession session) {
            session.subscribe(topic, new StompFrameHandler() {

                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    System.err.println(payload.toString());
                }
            });
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.err.println("Connected! Headers:");
            showHeaders(connectedHeaders);

            subscribeTopic(WEBSOCKET_TOPIC, session);
            sendJsonMessage(session);
        }
    }
}
