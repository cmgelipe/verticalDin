/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

/**
 *
 * @author pipe
 */
public class VerticalDin {

    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        //arg[0] es token
        //arg[1] es dirección de logs
        Vigilante vig=new Vigilante(args[0],args[1]);
        
    }
    
}
