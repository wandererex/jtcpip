package com.github.jtcpip;

import jpcap.PacketReceiver;

import java.util.ArrayList;
import java.util.List;
import jpcap.packet.Packet;

public class PacketProvider implements IPacketProvider{
    private List<PacketReceiver> receiverList = new ArrayList<PacketReceiver>();

    @SuppressWarnings("unused")
    protected void pushPacketToReceivers(Packet packet) {
        for (int i = 0; i < this.receiverList.size(); i++) {
            PacketReceiver receiver = (PacketReceiver) this.receiverList.get(i);
            receiver.receivePacket(packet);
        }
    }

    @Override
    public void registerPacketReceiver(PacketReceiver receiver) {
        if (this.receiverList.contains(receiver) != true) {
            this.receiverList.add(receiver);
        }
    }
}
