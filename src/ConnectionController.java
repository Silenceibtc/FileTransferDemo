import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ConnectionController extends Thread {
    private final Socket socket;
    private File file;
    private final File root;
    private File[] list;
    private final DatagramSocket datagramSocket;

    public ConnectionController(Socket socket, File file, DatagramSocket datagramSocket) {
        this.socket = socket;
        this.file = file;
        this.root = file;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        try {
            OutputStream os = socket.getOutputStream();
            DataOutputStream dataoutputStream = new DataOutputStream(os);
            dataoutputStream.writeUTF(socket.getRemoteSocketAddress() + "-> 连接成功\n");

            InputStream is = socket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(is);
            while (true) {
                String operation = dataInputStream.readUTF();
                if ("ls".equals(operation)) {
                    listFiles(dataoutputStream);
                } else if (operation.startsWith("cd") && operation.length() > 3) {
                    cdFiles(dataoutputStream, operation);
                } else if (operation.startsWith("get ") && operation.length() > 4) {
                    String[] info = operation.split(" ");
                    String filename = info[1];
                    String filePath = file.getAbsolutePath() + "\\" + filename;
                    File ToBeTransfered = new File(filePath);
                    if (ToBeTransfered.isFile()) {
                        dataoutputStream.writeUTF("开始发送");
                        transferFile(filePath, info[2]);
                    } else {
                        dataoutputStream.writeUTF("非法的文件路径！\n");
                    }
                } else
                    dataoutputStream.writeUTF("输入的指令有误，请检查后重新输入\n");
            }
        } catch (Exception e) {
            System.out.println("有客户端断开了连接");
        }
    }

    private long getSizeOfDir(File file) {
        long size = 0;
        if (file.isFile()) {
            return file.length();
        }

        if (file.isDirectory()) {
            File[] listFile = file.listFiles();
            if (listFile != null){
                for (File f : listFile) {
                    if (f.isFile()) {
                        size += f.length();
                    } else {
                        size += getSizeOfDir(f);
                    }
                }
            }
        }
        return size;
    }

    /**
     * 列出当前目录下所有文件
     *
     * @param dataOutputStream 数据输出流
     */
    private void listFiles(DataOutputStream dataOutputStream) throws Exception {
        list = file.listFiles();
        String msg = "";
        for (File listFile : list) {
            if (listFile.isDirectory()) {
                msg += "<dir> " + listFile.getName() + " " + getSizeOfDir(listFile);
            } else {
                msg += "<file> " + listFile.getName() + " " + listFile.length();
            }
            if (listFile != list[list.length - 1])
                msg += "\n";
        }
        dataOutputStream.writeUTF(msg);
    }

    /**
     * 跳转目录
     *
     * @param dataOutputStream 数据输出流
     * @param operation        操作
     */
    private void cdFiles(DataOutputStream dataOutputStream, String operation) throws Exception {
        if ("cd..".equals(operation)) {
            //如果当前不是根目录，退回上一级目录
            if (!root.getName().equals(file.getName())) {
                String absolutePath = file.getAbsolutePath();
                String newPath = absolutePath.substring(0, absolutePath.length() - file.getName().length());
                file = new File(newPath);
                dataOutputStream.writeUTF(newPath + ">>OK");
            } else {
                dataOutputStream.writeUTF("当前是根目录，无法退回！");
            }
        } else if (operation.startsWith("cd ")) {
            String dir = operation.substring(3);
            String newPath = file.getAbsolutePath() + "\\" + dir;
            File newDir = new File(newPath);
            if (newDir.exists() && newDir.isDirectory()) {
                file = newDir;
                dataOutputStream.writeUTF(dir + ">>OK");
            } else {
                dataOutputStream.writeUTF("非有效路径，请重新输入");
            }

        } else {
            dataOutputStream.writeUTF("非法指令！");
        }
    }

    /**
     * 发送对应文件
     *
     * @param filePath 文件路径
     * @param port     接收文件的客户端端口号
     */
    private void transferFile(String filePath, String port) {
        try (InputStream is = new FileInputStream(filePath)) {//创建文件输入流与目标文件连接
            byte[] buffer = new byte[1500]; //定义字节数组用于装载数据文件
            System.out.println(port);
            int len;
            while (true) {
                if ((len = is.read(buffer)) != -1) {
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, len, InetAddress.getLocalHost(), Integer.parseInt(port));
                    datagramSocket.send(datagramPacket);
                } else {
                    //发送一个空包来表示发送完毕
                    byte[] emptyBuffer = new byte[0];
                    DatagramPacket emptyPacket = new DatagramPacket(emptyBuffer, 0, emptyBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(port));
                    datagramSocket.send(emptyPacket);
                    break;
                }
            }
        } catch (Exception e) {

        }
    }
}
