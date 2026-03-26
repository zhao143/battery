package com.example.bms.tcp;

import com.example.bms.domain.BatteryData;
import com.example.bms.domain.Device;
import com.example.bms.domain.ThresholdSetting;
import com.example.bms.service.BatteryService;
import com.example.bms.mapper.DeviceMapper;
import com.example.bms.mapper.ThresholdSettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TcpServer {
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);
    
    private final BatteryService batteryService;
    private final DeviceMapper deviceMapper;
    private final ThresholdSettingMapper thresholdMapper;
    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final List<Socket> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Long> deviceLastHeartbeat = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TcpServer(BatteryService batteryService, DeviceMapper deviceMapper, ThresholdSettingMapper thresholdMapper, @Value("${tcp.port:9000}") int port) {
        this.batteryService = batteryService;
        this.deviceMapper = deviceMapper;
        this.thresholdMapper = thresholdMapper;
        this.port = port;
    }

    @PostConstruct
    public void start() {
        running = true;
        log.info("Starting TCP server on port {}", port);
        
        scheduler.scheduleAtFixedRate(this::checkDeviceStatus, 30, 30, TimeUnit.SECONDS);
        
        Thread t = new Thread(this::acceptLoop, "tcp-server-accept");
        t.setDaemon(true);
        t.start();
    }

    private void checkDeviceStatus() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : deviceLastHeartbeat.entrySet()) {
            long diff = now - entry.getValue();
            if (diff > 60000) {
                String uuid = entry.getKey();
                deviceMapper.updateStatus(uuid, 0);
                log.info("Device {} is now offline (no data for 60s)", uuid);
                deviceLastHeartbeat.remove(uuid);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (Socket c : clients) {
            try { c.close(); } catch (IOException ignored) {}
        }
        clients.clear();
    }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            log.info("TCP Server listening on port {}", port);
            while (running) {
                Socket client = ss.accept();
                log.info("TCP client connected from {}", client.getRemoteSocketAddress());
                clients.add(client);
                Thread handler = new Thread(() -> handleClient(client), "tcp-client-" + client.getPort());
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            log.error("TCP Server error: {}", e.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            int ch;
            while (running && (ch = reader.read()) != -1) {
                char c = (char) ch;
                if (c == '\n' || c == '\r') {
                    if (sb.length() > 0) {
                        String line = sb.toString().trim();
                        log.info("Received: {}", line);
                        processBuffer(line, writer);
                        sb.setLength(0);
                    }
                } else {
                    sb.append(c);
                    if (countHashes(sb) >= 8) {
                        String s = sb.toString().trim();
                        String[] parts = s.split("#");
                        if (parts.length == 9 && parts[8].length() > 0) {
                            log.info("Processing complete data packet: {}", s);
                            processBuffer(s, writer);
                            sb.setLength(0);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Client handler error: {}", e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
            clients.remove(client);
            log.info("TCP client disconnected");
        }
    }

    private void processBuffer(String payload, PrintWriter writer) {
        BatteryData data = parseLine(payload.trim());
        if (data != null) {
            String uuid = data.getDeviceUuid();
            Device device = deviceMapper.selectByUuid(uuid);
            if (device == null) {
                log.warn("Unknown device UUID: {}, data discarded", uuid);
                return;
            }
            deviceMapper.updateStatus(uuid, 1);
            deviceLastHeartbeat.put(uuid, System.currentTimeMillis());
            batteryService.saveData(data);
            int fan = batteryService.getDesiredFanState();
            int relay = batteryService.getDesiredRelayState();
            writer.println("CTRL#" + fan + "#" + relay);
            log.info("Data saved for {}, voltage={}, current={}, temp={}", uuid, data.getVoltage(), data.getCurrent(), data.getTemperature());
        } else {
            writer.println("ERR#FORMAT");
            log.warn("Failed to parse data: {}", payload);
        }
    }

    private BatteryData parseLine(String line) {
        try {
            String[] parts = line.trim().split("#");
            if (parts.length != 9) return null;
            for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
            BatteryData d = new BatteryData();
            d.setDeviceUuid(parts[0]);
            d.setVoltage(Double.parseDouble(parts[1]));
            d.setCurrent(Double.parseDouble(parts[2]));
            d.setTemperature((int)Double.parseDouble(parts[3]));
            d.setCharge(Integer.parseInt(parts[4]));
            d.setPower(Double.parseDouble(parts[5]));
            d.setFanState(Integer.parseInt(parts[6]));
            d.setRelayState(Integer.parseInt(parts[7]));
            d.setAlarmState(Integer.parseInt(parts[8]));
            return d;
        } catch (Exception e) {
            log.error("Parse error: {}", e.getMessage());
            return null;
        }
    }

    private int countHashes(StringBuilder sb) {
        int count = 0;
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '#') count++;
        }
        return count;
    }

    public void broadcastControlUpdate(int fan, int relay) {
        String msg = "CTRL#" + fan + "#" + relay;
        broadcastRaw(msg);
    }

    public void broadcastRaw(String msg) {
        log.info("Broadcasting: {}", msg);
        for (Socket client : clients) {
            try {
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8);
                writer.println(msg);
            } catch (IOException e) {
                log.error("Broadcast error: {}", e.getMessage());
            }
        }
    }
}
