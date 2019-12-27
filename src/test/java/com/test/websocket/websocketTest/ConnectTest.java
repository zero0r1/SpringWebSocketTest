package com.test.websocket.websocketTest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;


@SpringBootTest(classes = WebsocketTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConnectTest {
    //连接数
    public static int connectNum = 0;
    //连接成功数
    public static int successNum = 0;
    //连接失败数
    public static int errorNum = 0;
    //消费数量
    public static int consumptionNum = 0;

    static final String WEBSOCKET_URI = "ws://qa3.ws.yqn:39025/gs-guide-websocket";
    static final String WEBSOCKET_TOPIC = "/user/exchange/exchange_message/nxd";
    private volatile static List<StompSession> sessions = Collections.synchronizedList(new ArrayList());


    /**
     * 测试websocket最大连接数
     *
     * @throws InterruptedException
     */
    @Test
    public void testConnect() throws InterruptedException, ExecutionException {

        final List<StompSession> list = new ArrayList<>();
        final AtomicInteger item = new AtomicInteger(0);
        new Thread() {
            @Override
            public void run() {

                while (true)
                    try {
                        {
                            try {
                                Thread.sleep(3000);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }

                            for (int i = 0; i < sessions.size(); i++) {
                                try {
                                    final StompSession session = sessions.get(i);
                                    if (session == null || session.getSessionId() == null) {
                                        continue;
                                    }
                                    final ClientMessage msg = new ClientMessage(session.getSessionId(),
                                            item.getAndIncrement());

                                    msg.setFrom(session.getSessionId());
                                    msg.setText(String.format("%s%s", session.getSessionId(), item.toString()));

                                    try {
                                        session.send(WEBSOCKET_TOPIC, msg);
                                    } catch (final Exception e) {
                                        addErrorNum();
                                        e.printStackTrace();
                                    }

                                    if (consumptionNum > 0) {
                                        countdownConsumptionNum();
                                    }

                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            // 每次3秒打印一次连接结果
                            System.out.println(
                                    DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss:sss")
                                            + "  连接数：" + connectNum + "  成功数：" + successNum + "  失败数：" + errorNum);
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
            }
        }.start();

        System.out.println("开始时间：" + DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss:sss"));
        final AtomicInteger ai = new AtomicInteger(0);
        while (true) {
            // 连接失败超过10次，停止测试
            // if (errorNum > 10) {
            // break;
            // }
            if (ai.get() < 100000) {
                synchronized (this) {
                    sessions.add(newSession(++connectNum));
                }
            }
            // Thread.sleep(10);
            ai.getAndIncrement();
        }
    }

    /**
     * 创建websocket连接
     *
     * @param i
     * @return
     */
    private StompSession newSession(final int i) {

        final WebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
        // 模拟用户数量
        final List<Transport> transports = new ArrayList<>(1);
        transports.add((Transport) new WebSocketTransport(simpleWebSocketClient));

        final SockJsClient sockJsClient = new SockJsClient(transports);
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        final String userId = "spring-" + ThreadLocalRandom.current().nextInt(1, 99);
        StompSession connect = null;
        try {
            connect = stompClient.connect(WEBSOCKET_URI, new TestConnectHandler(userId)).get(100, TimeUnit.DAYS);
        } catch (final TimeoutException e) {
        } catch (final ExecutionException e) {
        } catch (final InterruptedException e) {
        } catch (final Exception e) {
        }
        return connect;
    }

    private static synchronized void addSuccessNum() {
        successNum++;
    }

    private static synchronized void addErrorNum() {
        errorNum++;
    }

    private static synchronized void addConsumptionNum() {
        consumptionNum++;
    }

    private static synchronized void countdownConsumptionNum() {
        consumptionNum--;
    }

    private static class TestConnectHandler extends StompSessionHandlerAdapter {

        private final String userId;

        public TestConnectHandler(final String userId) {
            this.userId = userId;
        }

        private void showHeaders(final StompHeaders headers) {
            for (final Map.Entry<String, List<String>> e : headers.entrySet()) {
                System.err.print("  " + e.getKey() + ": ");
                boolean first = true;
                for (final String v : e.getValue()) {
                    if (!first)
                        System.err.print(", ");
                    System.err.print(v);
                    first = false;
                }
                System.err.println();
            }
        }

        private void sendJsonMessage(final StompSession session) {
            final ClientMessage msg = new ClientMessage(userId, "hello from spring");
            session.send(WEBSOCKET_TOPIC, msg);
        }

        @Override
        public void afterConnected(final StompSession session, final StompHeaders connectedHeaders) {
            addSuccessNum();
            subscribeTopic(WEBSOCKET_TOPIC, session);
            sendJsonMessage(session);
        }

        private void subscribeTopic(final String topic, final StompSession session) {
            session.subscribe(topic, new StompFrameHandler() {

                @Override
                public Type getPayloadType(final StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(final StompHeaders headers, final Object payload) {
                    addConsumptionNum();
                    System.err.println(DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss:sss")
                            + "  当前连接数：" + connectNum + "  成功数：" + successNum + "  失败数：" + errorNum + "  消费数："
                            + consumptionNum);
                }
            });
        }

        @Override
        public void handleTransportError(final StompSession session, final Throwable exception) {
            addErrorNum();
        }
    }
}
