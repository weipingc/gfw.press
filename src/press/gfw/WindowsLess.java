/**
* 
*    GFW.Press
*    Copyright (C) 2016  chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*    
**/
package press.gfw;
import java.io.IOException;
import java.security.Security;
import java.sql.Timestamp;

import org.json.simple.JSONObject;
/**
 * 
 * GFW.Press Commandline
 * 
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 * @author chinap.anthole
 *
 */
public class WindowsLess {
    public static void main(String[] args) throws IOException {
    	Security.setProperty("crypto.policy", "unlimited");
        WindowsLess windows = new WindowsLess();
        windows.start();
    }
    private Client client = null;
    private Config config = null;
    private String serverHost = "", serverPort = "", password = "", proxyPort = "";

    public WindowsLess() {
        config = new Config();
        loadConfig();
    }

    private void loadConfig() {
        JSONObject json = config.getClientConfig();
        if (json != null) {
            serverHost = json.get("ServerHost") == null ? "" : (String) json.get("ServerHost");
            serverPort = json.get("ServerPort") == null ? "" : (String) json.get("ServerPort");
            proxyPort = json.get("ProxyPort") == null ? "" : (String) json.get("ProxyPort");
            password  = json.get("Password")  == null ? "" : (String) json.get("Password");
        }
    }
    
    private void log(Object o) {
        String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);
        System.out.println("[" + time + "] " + o.toString());
    }
    
    public void start() {
        if (client != null && !client.isKill()) {
            if (serverHost.equals(client.getServerHost())
             && serverPort.equals(String.valueOf(client.getServerPort()))
             && password.equals(client.getPassword())
             && proxyPort.equals(String.valueOf(client.getListenPort()))
            ) {
                return;
            } else {
                client.kill();
            }
        }
        client = new Client(config.getAllowedHosts(), serverHost, serverPort, password, proxyPort);
        client.start();
        // log(client.getName());
    }
}
