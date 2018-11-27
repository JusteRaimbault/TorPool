/**
 * 
 */
package utils.tor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import utils.Log;

/**
 * Manager communicating with the external TorPool app, via .tor_tmp files (TorPool must be run within same directory for now)
 * 
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class TorPoolManager {

	/**
	 * TODO : Concurrent access from diverse apps to a single pool ?
	 * Difficult as would need listener on this side...
	 * 
	 */
	
	/**
	 * the port currently used.
	 */
	public static int currentPort=0;
	
	
	public static boolean hasTorPoolConnexion = false;
	
	
	/**
	 * Checks if a pool is currently running, and setup initial port correspondingly.
	 */
	public static void setupTorPoolConnexion(boolean portexclusivity) throws Exception {
		
		Log.stdout("Setting up TorPool connection...");
		
		// check if pool is running.
		checkRunningPool();
		
		System.setProperty("socksProxyHost", "127.0.0.1");
		
		
		try{
			//changePortFromFile(new BufferedReader(new FileReader(new File(".tor_tmp/ports"))));
			switchPort(portexclusivity);
		}catch(Exception e){e.printStackTrace();}
		
		//showIP();
		
		hasTorPoolConnexion = true;
	}
	
	
	/**
	 * Send a stop signal to the whole pool -> needed ? Yes to avoid having tasks going on running on server e.g.
	 */
	public static void closePool(){
		
	}
	
	
	private static void checkRunningPool() throws Exception{
		if(!new File(".tor_tmp/ports").exists()){throw new Exception("NO RUNNING TOR POOL !"); }
	}
	
	
	/**
	 * Switch the current port to the oldest living TorThread.
	 *   - Reads communication file -
	 */
	public static void switchPort(boolean portexclusivity){
		try{
			//send kill signal via kill file
			// if current port is set
			if(currentPort!=0){
				Log.stdout("Sending kill signal for current tor thread...");
				(new File(".tor_tmp/kill"+currentPort)).createNewFile();
			}

			String portpath = ".tor_tmp/ports";
			String lockfile = ".tor_tmp/lock";
			int newport = changePortFromFile(portpath,lockfile);
			if(portexclusivity){
				removeInFileWithLock(new Integer(newport).toString(),portpath,lockfile);
			}

			// show ip to check
			showIP();
			
		}catch(Exception e){e.printStackTrace();}
	}

	/**
	 *
	 * @param portpath
	 * @param lockfile
	 */
	private static int changePortFromFile(String portpath,String lockfile){
		//String newPort = "9050";
		String newPort = "";

		// assumes ports with 4 digits
		// and that the file always has a content
		while(newPort.length()<4) {
			try{
				newPort = readLineWithLock(portpath,lockfile);
				if(newPort.length()<4){System.out.println("Waiting for an available tor port");Thread.sleep(1000);}
			}catch(Exception e){e.printStackTrace();}
		}

		// set the new port
		System.setProperty("socksProxyPort",newPort);
		currentPort = Integer.parseInt(newPort);
		Log.stdout("Current Port set to "+newPort);
		return(newport)
	}

	/**
	 *
	 *
	 * @param portpath
	 * @param lockfile
	 * @return
	 */
	private static String readLineWithLock(String portpath,String lockfile){
		String res = "";
		try{
			boolean locked = true;int t=0;
			while(locked){
				Log.stdout("Waiting for lock on "+lockfile);
				Thread.sleep(200);
				locked = (new File(lockfile)).exists();t++;
			}
			File lock = new File(".tor_tmp/lock");lock.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(portpath)));
			res=r.readLine();
			lock.delete();
		}catch(Exception e){e.printStackTrace();}
		return(res);
	}


	private static void removeInFileWithLock(String s,String file,String lock){
		try{
			boolean locked = true;
			while(locked){
				System.out.println("Waiting for lock on "+lock);
				Thread.sleep(200);
				locked = (new File(lock)).exists();
			}
			File lockfile = new File(".tor_tmp/lock");lockfile.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(file)));
			LinkedList<String> newcontent = new LinkedList<String>();
			String currentline = r.readLine();
			while(currentline!=null){
				if(currentline.replace("\n","")!=s){newcontent.add(currentline);}
			}
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(file)));
			for(String remp:newcontent){
				w.write(remp);w.newLine();
			}
			w.close();
			// unlock the dir
			lockfile.delete();
		}catch(Exception e){e.printStackTrace();}
	}
	
	
	
	
	// Test functions
	
	private static void testRemotePool(){
		try{setupTorPoolConnexion();
		
		showIP();
		
		while(true){
			Thread.sleep(10000);
			Log.stdout("TEST : Switching port... ");
			switchPort();
			showIP();
		}
		}catch(Exception e){e.printStackTrace();}
	}
	
	private static void showIP(){
		try{
		BufferedReader r = new BufferedReader(new InputStreamReader(new URL("http://ipecho.net/plain").openConnection().getInputStream()));
		String currentLine=r.readLine();
		while(currentLine!= null){Log.stdout(currentLine);currentLine=r.readLine();}
		}catch(Exception e){e.printStackTrace();}
	}
	
	
	public static void main(String[] args){
		testRemotePool();
	}
	
	
	
	
	
	
}
