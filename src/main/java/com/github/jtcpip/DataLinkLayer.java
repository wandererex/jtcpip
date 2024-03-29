package com.github.jtcpip;


import jpcap.NetworkInterfaceAddress;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;

public class DataLinkLayer extends PacketProvider implements jpcap.PacketReceiver, IMacReceiver{
    //change 1
    private static DataLinkLayer instance = null;
    private NetworkInterface device = null;
    private Inet4Address ipAddress = null;
    private byte[] macAddress = null;
    JpcapSender sender = null;

    JpcapCaptor captor= null;

    private DataLinkLayer() {

    }

    public static DataLinkLayer getInstance() {
        if (instance == null) {
            instance = new DataLinkLayer();
        }

        return instance;
    }

    // change 2
    public void initWithOpenDevice(NetworkInterface device) {
        this.device = device;
        this.ipAddress = this.getDeviceIpAddress();
        this.macAddress = new byte[6];
        this.getDeviceMacAddress();
        try {
            captor = JpcapCaptor.openDevice(device,2000,false,3000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.sender = captor.getJpcapSenderInstance();
        new Thread(() -> {
            captor.loopPacket(-1, this);
        }).start();

        //测试arp协议
        this.testARPProtocol();
    }

    private Inet4Address getDeviceIpAddress() {
        for (NetworkInterfaceAddress addr  : this.device.addresses) {
            //网卡网址符合ipv4规范才是可用网卡
            if (!(addr.address instanceof Inet4Address)) {
                continue;
            }

            return (Inet4Address) addr.address;
        }

        return null;
    }

    private void getDeviceMacAddress() {
        int count = 0;
        for (byte b : this.device.mac_address) {
            this.macAddress[count] = (byte) (b & 0xff);
            count++;
        }
    }

    // change 3
    public  byte[] deviceIPAddress() {
        return this.ipAddress.getAddress();
    }

    public byte[] deviceMacAddress() {
        return this.macAddress;
    }


    @Override
    public void receivePacket(Packet packet) {
        //将受到的数据包推送给上层协议
        this.pushPacketToReceivers(packet);
    }

    public void sendData(byte[] data, byte[] dstMacAddress, short frameType) {
        /*
         * 给上层协议要发送的数据添加数据链路层包头，然后使用网卡发送出去
         */
        if (data == null) {
            return;
        }

        Packet packet = new Packet();
        packet.data = data;

        /*
         * 数据链路层会给发送数据添加包头：
         * 0-5字节：接受者的mac地址
         * 6-11字节： 发送者mac地址
         * 12-13字节：数据包发送类型，0x0806表示ARP包，0x0800表示ip包，
         */

        EthernetPacket ether=new EthernetPacket();
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac= this.device.mac_address;
        ether.dst_mac= dstMacAddress;
        packet.datalink = ether;

        sender.sendPacket(packet);
    }

    private void testARPProtocol() {
        ARPProtocolLayer arpLayer = new ARPProtocolLayer();
        this.registerPacketReceiver(arpLayer);

        byte[] ip;
        try {
            ip = InetAddress.getByName("192.168.0.1").getAddress();
            arpLayer.getMacByIP(ip, this);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void receiveMacAddress(byte[] ip, byte[] mac) {
        System.out.println("receive arp reply msg with sender ip: ");
        for (byte b: ip) {
            System.out.print(Integer.toUnsignedString(b & 0xff) + ".");
        }
        System.out.println("with sender mac :");
        for (byte b : mac)
            System.out.print(Integer.toHexString(b&0xff) + ":");

    }
}