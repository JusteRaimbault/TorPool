/**
 * 
 */
package utils.connexion.tor;

import java.io.BufferedReader;
import java.util.Date;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class StreamDisplayer extends Thread {
	
	private BufferedReader reader;
	private int port;
	
	public StreamDisplayer(BufferedReader r,int p){
		reader = r;
		port = p;
	}
	
	@Override
	public void run(){
		try{
			String currentLine=reader.readLine();

			while(true&&currentLine!=null){
				if(currentLine.contains("Bootstrapped 100")){
					TorThread.appendWithLock(new Integer(port).toString(),".tor_tmp/ports",".tor_tmp/lock");
					System.out.println("Bootstrap achieved: appended port "+port+" to .tor_tmp/ports");
				}
				System.out.println((new Date()).toString()+currentLine);currentLine = reader.readLine();
			}
		}catch(Exception e){e.printStackTrace();}
	}
	
}
