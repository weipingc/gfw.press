/**
*    GFW.Press
*    Copyright (C) 2016  chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/
package press.gfw;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashSet;

import javax.crypto.SecretKey;

public class Client extends Thread {
    private int listenPort = 0;
    private String serverHost = null;
    private int serverPort = 0;
    private String password = null;
    private SecretKey key = null;
    private Encrypt aes = null;
    private boolean kill = false;
    private String status = null;
    private ServerSocket listenSocket = null;
    private HashSet<String> allowedHosts = null;
    
    public Client(HashSet<String> allowedHosts, String serverHost, int serverPort, String password, int listenPort) {
        super();
        this.allowedHosts = allowedHosts;
        this.listenPort = listenPort;
        this.serverHost = (serverHost == null) ? null : serverHost.trim();
        this.serverPort = serverPort;
        this.password = (password == null) ? null : password.trim();
        aes = new Encrypt();
        if (aes.isPassword(this.password)) {
            key = aes.getPasswordKey(this.password);
        }
    }
    
    public Client(HashSet<String> allowedHosts, String serverHost, String serverPort, String password, String listenPort) {
        this(allowedHosts, serverHost,
        	(serverPort != null && (serverPort = serverPort.trim()).matches("\\d+")) ? Integer.valueOf(serverPort) : 0,
        	password,
        	(listenPort != null && (listenPort = listenPort.trim()).matches("\\d+")) ? Integer.valueOf(listenPort) : 0
        );
    }
    
    private void _sleep(long m) {
        try {
            sleep(m);
        } catch (InterruptedException ie) {
        }
    }
    public synchronized int getListenPort() {
        return listenPort;
    }
    public synchronized String getPassword() {
        return password;
    }
    public synchronized String getServerHost() {
        return serverHost;
    }
    public synchronized int getServerPort() {
        return serverPort;
    }
    public synchronized String getStatus() {
        return status;
    }
    public synchronized boolean isKill() {
        return kill;
    }
    
    public synchronized void kill() {
        kill = true;
        if (listenSocket != null && !listenSocket.isClosed()) {
            try {
                listenSocket.close();
            } catch (IOException ex) {
            }
            listenSocket = null;
        }
    }

    @SuppressWarnings("unused")
    private void log(Object o) {
        String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);
        System.out.println("[" + time + "] " + o.toString());
    }

    public void run() {
        if (serverHost == null || (serverHost = serverHost.trim()).length() == 0 || serverPort == 0 || listenPort == 0 || key == null) {
            status = "启动失败：配置错误";
            kill = true;
            return;
        }
        File lockFile = new File("client.lock");
        try {
            listenSocket = new ServerSocket(listenPort);
        } catch (IOException ex) {
            if (lockFile.exists()) {
                System.exit(0);
            }
            kill = true;
            status = "线程" + getName() + "启动时监听" + listenPort + "端口出错，线程结束。";
            return;
        }
        lockFile.deleteOnExit();
        if (!lockFile.exists()) {
            try {
                lockFile.createNewFile();
            } catch (IOException ioe) {
            }
        }
        while (!kill) {
            Socket agentSocket = null;
            try {
            	log("Accepting");
                agentSocket = listenSocket.accept();
            } catch (IOException ex) {
                if (listenSocket != null && !listenSocket.isClosed()) {
                    status = "线程" + getName() + "运行时监听" + listenPort + "端口出错，休息3秒钟后重试。";
                    _sleep(3000L);
                    continue;
                } else {
                    status = "线程" + getName() + "运行时监听" + listenPort + "端口出错，线程结束。";
                    break;
                }
            }
            
            String browserHostAddr = agentSocket.getInetAddress().getHostAddress();
        	log("Accepted from browserHostAddr=" + browserHostAddr);
            if (browserHostAddr == null || allowedHosts == null || !allowedHosts.contains(browserHostAddr.trim())) {
            	log("browserHostAddr not allowed: " + browserHostAddr);
            	continue;
            }
            ClientThread clientThread = new ClientThread(agentSocket, serverHost, serverPort, key);
            clientThread.start();
        }
        if (listenSocket != null && !listenSocket.isClosed()) {
            try {
                listenSocket.close();
            } catch (IOException ex) {
            }
            listenSocket = null;
        }
        kill = true;
    }
}
