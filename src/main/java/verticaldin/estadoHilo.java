package verticaldin;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
/**
 *
 * @author pipe
 */
public class estadoHilo extends Thread {
    public boolean banderaRefragmentacion;
    @Override
    public void run(){
        try {
            ServerSocket crearServidor = new ServerSocket(5000);
            while(true){
                System.out.println("Aceptando conexiones sobre socket");
                Socket socket = crearServidor.accept();
                this.banderaRefragmentacion=new DataInputStream(socket.getInputStream()).readBoolean();
                System.out.println("Bandera de refragmentacion: "+banderaRefragmentacion);
                socket.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }                
    }
}
