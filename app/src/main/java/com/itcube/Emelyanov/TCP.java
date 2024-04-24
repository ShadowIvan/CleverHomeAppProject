package com.itcube.Emelyanov;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class TCP {
    private static final String SERVER_IP = "your_server_ip";
    private static final int SERVER_PORT = 12345; // Порт, на котором работает ваш сервер

    public static void main(String[] args) {
        try {
            // Создаем сокет для отправки и получения пакетов UDP
            DatagramSocket socket = new DatagramSocket();

            // Подготавливаем данные для отправки
            byte[] sendData = "Hello, Server!".getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);

            // Отправляем пакет на сервер
            socket.send(sendPacket);

            // Получаем ответ от сервера
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Response from server: " + response);

            // Закрываем сокет
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
