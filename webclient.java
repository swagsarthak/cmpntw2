import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class webclient{

    public static void main(String[] args) {
        try {
            String url1 = args[0];
            if (!url1.startsWith("http://") && !url1.startsWith("https://")) {
                url1 = "http://" + url1;
            }

            @SuppressWarnings("deprecation")
            URL url = new URL(url1);
            String host = url.getHost();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();
            if (url.getPath().isEmpty()) {
                path = "/";
            } else {
                path = url.getPath();
            }
            int port;
            if (url.getPort() == -1) {
                port = 80;
            } else {
                port = url.getPort();
            };

            String fileName = "webout";
            boolean savio = true;
            boolean isPing = false;
            boolean pktio = false;
            boolean showInfo = false;

            if (args.length == 3 && args[1].equals("-f")) {
                fileName = args[2];
            } else if (args.length == 2 && args[1].equals("-nf")) {
                savio = false;
            } else if (args.length == 2 && args[1].equals("-ping")) {
                isPing = true;
            } else if (args.length == 2 && args[1].equals("-pkt")) {
                pktio = true;
                savio = false;
            } else if (args.length == 2 && args[1].equals("-info")) {
                showInfo = true;
            }

            if (isPing) {
                ping(host);
            } else {
                Socket socket = new Socket(host, port);

                if (showInfo) {
                    con_info(socket);
                }

                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
                writer.write("GET " + path + " HTTP/1.1\r\n");
                writer.write("Host: " + host + "\r\n");
                writer.write("Connection: close\r\n"); //change to keep-alive if need be
                writer.write("\r\n");
                writer.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;

                if (pktio) {
                    List<String> packetData = new ArrayList<>();
                    long startTime = System.currentTimeMillis();
                    int totalBytes = 0;

                    while ((line = reader.readLine()) != null) {
                        long currentTime = System.currentTimeMillis() - startTime;
                        int bytesRead = line.getBytes().length;
                        totalBytes += bytesRead;
                        packetData.add("Time: " + currentTime + " ms, Bytes " + bytesRead);
                    }

                    System.out.println("Packet Collected");
                    for (String pkt : packetData) {
                        System.out.println(pkt);
                    }
                    System.out.println("Total Bytes Received " + totalBytes);

                } else {
                    if (savio) {
                        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(fileName));
                        while ((line = reader.readLine()) != null) {
                            fileWriter.write(line);
                            fileWriter.newLine();
                        }
                        fileWriter.close();
                        System.out.println("Response saved ");
                    } else {
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                }

                reader.close();
                writer.close();
                socket.close();
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void ping(String host) {
        try {
            InetAddress inet = InetAddress.getByName(host);

            long startTime = System.currentTimeMillis();

            if (inet.isReachable(5000)) {
                long endTime = System.currentTimeMillis();
                long rtt = endTime - startTime;
                System.out.println(inet.getHostAddress() + " RTT: " + rtt + " ms");
            } else {
                System.out.println(host + " is not reachable.");
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void con_info(Socket socket) {
        try {
            System.out.println("Local Address " + socket.getLocalAddress());
            InetAddress inet = socket.getInetAddress();
            System.out.println("Remote Address " + inet.getHostAddress());
            System.out.println("Remote Port " + socket.getPort());
            //this part reads from terminal of linux as java doesnt has the ability like c to read from kernel
            String host = inet.getHostAddress();
            @SuppressWarnings("deprecation")
            Process process = Runtime.getRuntime().exec("ping -c 1 " + host);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=")) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("time=")) {
                            String rtt = part.split("=")[1];
                            System.out.println("RTT " + rtt + " ms");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    
}
