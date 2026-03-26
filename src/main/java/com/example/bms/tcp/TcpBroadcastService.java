package com.example.bms.tcp;

import org.springframework.stereotype.Service;

@Service
public class TcpBroadcastService {
    private final TcpServer tcpServer;
    public TcpBroadcastService(TcpServer tcpServer) { this.tcpServer = tcpServer; }
    public void broadcastRaw(String cmd) { tcpServer.broadcastRaw(cmd); }
}