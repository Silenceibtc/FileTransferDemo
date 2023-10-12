import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {

        try (Socket socket = new Socket("127.0.0.1", 2021);
             DatagramSocket datagramSocket = new DatagramSocket();
        ) {
            try {
                //创建输入流对象，包装为数据流
                InputStream is = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(is);
                String msg = dataInputStream.readUTF(); //接收连接成功信息
                System.out.println(msg);
                //创建输出流对象，包装为数据流，通知服务端要进行的操作
                OutputStream os = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(os);
                Scanner sc = new Scanner(System.in);
                System.out.println("请输入要进行的操作");
                System.out.println("[1]\tls\t服务器返回当前目录文件列表（<file/dir>\tname\tsize）");
                System.out.println("[2]\tcd  <dir>\t进入指定目录（需判断目录是否存在，并给出提示）");
                System.out.println("[3]\tget  <file>\t通过UDP下载指定文件，保存到客户端当前目录下");
                System.out.println("[4]bye\t断开连接，客户端运行完毕");

                while (true) {
                    String operation = sc.nextLine();
                    if ("bye".equals(operation))
                        break;
                    if (operation.startsWith("get")) {
                        operation = operation + " " + datagramSocket.getLocalPort();
                        dataOutputStream.writeUTF(operation);
                        String filename = operation.split(" ")[1];
                        String filePath = "FileTransfer\\src\\resources\\" + filename;
                        if ((msg = dataInputStream.readUTF()).equals("开始发送")){
                            try (OutputStream outputStream = new FileOutputStream(filePath)) {
                                System.out.println("开始接收文件：" + filename);
                                byte[] buffer = new byte[1500];
                                int len;
                                while (true) {
                                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                                    datagramSocket.receive(datagramPacket);
                                    byte[] data = datagramPacket.getData();
                                    len = datagramPacket.getLength();
                                    if (len == 0) {
                                        System.out.println("接收完毕");
                                        break;
                                    }
                                    outputStream.write(data, 0, len);
                                }
                            } catch (Exception e) {

                            }
                        }else {
                            System.out.print(msg);
                        }
                    } else {
                        dataOutputStream.writeUTF(operation);
                        System.out.println(dataInputStream.readUTF());
                    }
                }
            } catch (Exception e) {

            }
        }
    }
}
