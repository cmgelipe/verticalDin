/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package verticaldin;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
/**
 *
 * @author pipe
 */
public class estadoHilo extends Thread {
    public boolean banderaRefragmentacion;
    public void run(){
        try {
            ServerSocket crearServidor = new ServerSocket(5000);
            while(true){
                Socket socket = crearServidor.accept();
                this.banderaRefragmentacion=new DataInputStream(socket.getInputStream()).readBoolean();
                socket.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }                
    }
}
