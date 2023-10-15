import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static File file;

    public static void main(String[] args) throws Exception {

        try (ServerSocket serverSocket = new ServerSocket(2021, 6, InetAddress.getLocalHost());
             DatagramSocket datagramSocket = new DatagramSocket(2020);
        ) {
            //检查根目录是否有效
            if (args.length != 0) {
                checkArgs(args[0]);
                System.out.println("ip地址[" + serverSocket.getInetAddress() + "] port" + serverSocket.getLocalPort());
                System.out.println("根目录：[" + args[0] + "]");
            } else {
                System.out.println("未配置根目录路径！");
            }

            while (true) {
                try {
                    //等待客户端建立连接
                    Socket socket = serverSocket.accept();
                    System.out.println("新客户端连接。ip地址：" + socket.getRemoteSocketAddress());
                    new ConnectionController(socket, file, datagramSocket).start();
                } catch (Exception e) {

                }
            }
        }
    }

    private static void checkArgs(String args) {
        file = new File(args);
        if (!file.isDirectory()) {
            System.out.println("根目录非有效目录");
            System.exit(0);
        }
        System.out.print("服务端启动！");

    }
}
