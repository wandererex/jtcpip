package com.github.jtcpip;
public interface IPacketProvider {
    void registerPacketReceiver(jpcap.PacketReceiver receiver);
}
