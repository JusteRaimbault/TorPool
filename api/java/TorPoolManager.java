/**
 *
 */
package bibliodata.utils.tor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;

import bibliodata.utils.Log;

/**
 * Manager communicating with the external TorPool app, via .tor_tmp files (TorPool must be run within same directory for now)
 *
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class TorPoolManager {


	private static final String ipurl = "http://ipecho.net/plain";


	/**
	 * Concurrent access from diverse apps to a single pool ?
	 *   -> achieved concurrently with portexclusivity option
	 */

	/**
	 * the port currently used
	 */
	public static int currentPort=0;

	/**
	 * is there a running torpool
	 */
	public static boolean hasTorPoolConnexion = false;

	/**
	 * current IP
	 */
	public static String currentIP = "";

	public static boolean mongoMode = true;
	public static MongoClient mongoClient;
	public static MongoDatabase mongoDatabase;

	public static final String mongoHost = "127.0.0.1";
	public static final int mongoPort = 27017;
	public static final String mongoDB = "tor";
	public static final String mongoCollection = "ports";

	public static void initMongo() {
		try {
			mongoClient = new MongoClient(mongoHost, mongoPort);
			mongoDatabase = mongoClient.getDatabase(mongoDB);
		} catch(Exception e){
			System.out.println("No mongo connection possible : ");
			e.printStackTrace();
		}
	}

	public static void closeMongo() {
		try{
			mongoClient.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String getPortFromMongo(boolean exclusivity){
		initMongo();
		MongoCollection<Document> collection = mongoDatabase.getCollection(mongoCollection);
		String res = "";
		if(exclusivity){
			Document d = collection.findOneAndDelete(exists("port"));
			if(d.containsKey("port")){res = d.getString("port");}
		}else{
			FindIterable fi =collection.find();
			if(fi.iterator().hasNext()){
				Document d = (Document) fi.iterator().next();
				if(d.containsKey("port")){res = d.getString("port");}
			}
		}
		closeMongo();
		return(res);
	}


	public static void setupTorPoolConnexion(boolean portexclusivity){setupTorPoolConnexion(portexclusivity,false);}


	/**
	 * Checks if a pool is currently running, and setup initial port correspondingly.
	 *
	 * @param portexclusivity should the used port be removed from the list of available ports (exclusivity)
	 */
	public static void setupTorPoolConnexion(boolean portexclusivity,boolean mongoMode) {

		Log.stdout("Setting up TorPool connection...");

		// check if pool is running.
		//checkRunningPool();

		if(hasRunningPool()) {

			Log.stdout("   -> running pool ok");

			System.setProperty("socksProxyHost", "127.0.0.1");

			try{
				switchPort(portexclusivity);
			}catch(Exception e){Log.stdout("Impossible to set port at initialization");e.printStackTrace();}

			hasTorPoolConnexion = true;

		}else{
			Log.stdout("   -> no running pool, not setting socks proxy");
		}
	}


	/**
	 * Send a stop signal to the whole pool
	 */
	public static void closePool(){

	}


	public static boolean hasRunningPool() {
		try {
			if (new File(".tor_tmp").exists()) {
				if(mongoMode) {try{initMongo();closeMongo();return(true);}catch(Exception e){return false;}}
				else return (new File(".tor_tmp/ports").exists());
			} else {
				return (false);
			}
		}catch(Exception e){e.printStackTrace();return(false);}
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
			String newport = "";

			while(newport.length()<4) {
				if (portexclusivity) {
					if(mongoMode){
						newport = getPortFromMongo(true);
					}else {
						newport = readAndRemoveLineWithLock(portpath, lockfile);
					}
				} else {
					if(mongoMode) {
						newport = getPortFromMongo(false);
					}else {
						newport = readLineWithLock(portpath, lockfile);
					}
				}
				// add significant waiting time to avoid overcrowding
				Thread.sleep(1000);
			}

			// show ip to check
			showIP();

			changePort(newport);

		}catch(Exception e){
			e.printStackTrace();
			if(!mongoMode) removeLock(".tor_tmp/lock");
		}
	}


	/**
	 * show and record IP
	 */
	private static void showIP(){
		try{
			BufferedReader r = new BufferedReader(new InputStreamReader(new URL(ipurl).openConnection().getInputStream()));
			String currentLine=r.readLine();
			while(currentLine!= null){
				Log.stdout("IP : "+currentLine);
				currentIP = currentLine;
				currentLine=r.readLine();
			}
		}catch(Exception e){}
	}



	/**
	 * Change port
	 */
	private static void changePort(String newport){
		// set the new port
		System.setProperty("socksProxyPort",newport);
		currentPort = Integer.parseInt(newport);
		Log.stdout("Current Port set to "+newport);
	}

	/**
	 * Release the current port (without killing the task) -> used when in exclusivity ?
	 */
	public static void releasePort() {
		Log.stdout("Releasing port "+currentPort);
		switchPort(false);
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
				if(t==0){Log.stdout("Waiting for lock on "+lockfile);}else{Log.stdout(".",false);}
				Thread.sleep(200);
				locked = (new File(lockfile)).exists();t++;
			}
			File lock = new File(lockfile);lock.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(portpath)));
			res=r.readLine();
			lock.delete();
		}catch(Exception e){
			e.printStackTrace();
			removeLock(lockfile);
		}
		return(res);
	}


	private static String readAndRemoveLineWithLock(String portfile,String lockfile) {
		String res = "";
		try {
			boolean locked = true;int t = 0;
			while (locked) {
				if(t==0){Log.stdout("Waiting for lock on "+lockfile);}else{Log.stdout(".",false);}
				Thread.sleep(200);
				locked = (new File(lockfile)).exists();t++;
			}
			File lock = new File(lockfile);lock.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(portfile)));
			LinkedList<String> newcontent = new LinkedList<String>();
			String currentline = r.readLine();
			while(currentline!=null){newcontent.add(currentline);currentline=r.readLine();}
			r.close();

			// content may be empty (too much requests simultaneously)
			if(newcontent.size()>0) {
				BufferedWriter w = new BufferedWriter(new FileWriter(new File(portfile)));
				res = newcontent.removeFirst();
				for (String remp : newcontent) {
					w.write(remp);
					w.newLine();
				}
				w.close();
			}
			// unlock the dir
			lock.delete();
		}catch(Exception e){
			e.printStackTrace();
			removeLock(lockfile);
		}
		return(res);
	}






	/*
	// FIXME function not used ?
	private static void removeInFileWithLock(String s,String file,String lock){
		try{
			boolean locked = true;
			while(locked){
				System.out.println("Waiting for lock on "+lock);
				Thread.sleep(200);
				locked = (new File(lock)).exists();
			}
			File lockfile = new File(lock);lockfile.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(file)));
			LinkedList<String> newcontent = new LinkedList<String>();
			String currentline = r.readLine();
			while(currentline!=null){
				if(currentline.replace("\n","")!=s){newcontent.add(currentline);currentline=r.readLine();}
			}
			r.close();
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(file)));
			for(String remp:newcontent){
				w.write(remp);w.newLine();
			}
			w.close();
			// unlock the dir
			lockfile.delete();
		}catch(Exception e){
			e.printStackTrace();
			removeLock(lock);
		}
	}
	*/

	private static void removeLock(String lock){
		try {
			File lockfile = new File(lock);
			if(lockfile.exists()){lockfile.delete();}
		}catch(Exception e){e.printStackTrace();}
	}




	// Test functions

	private static void testRemotePool(){
		try{setupTorPoolConnexion(true,mongoMode);

			showIP();

			while(true){
				Thread.sleep(10000);
				Log.stdout("TEST : Switching port... ");
				switchPort(true);
				showIP();
			}
		}catch(Exception e){e.printStackTrace();}
	}




	public static void main(String[] args){
		testRemotePool();
	}






}
