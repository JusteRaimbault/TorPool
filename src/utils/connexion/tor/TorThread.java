/**
 * 
 */
package utils.connexion.tor;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class TorThread extends Thread {
	
	
	/**
	 * Default tor command
	 */
	public static String torCommand = "tor";

	
	/**
	 * If the task is running
	 */
	public boolean running;
	
	/**
	 * Port for this thread
	 */
	public int port;
	
	/**
	 * Basic constructor
	 * 
	 * Does not actually launch the tor command, but attributes port and updates tables
	 */
	public TorThread(){
		// pick a port to run the new thread
		// assumed to be totally free if in list of available ports
		port = TorPool.available_ports.keySet().iterator().next().intValue();
		
		while(TorPool.used_ports.contains(new Integer(port))){
		  port = TorPool.available_ports.keySet().iterator().next().intValue();
		}
		TorPool.used_ports.put(new Integer(port), new Integer(port));
		TorPool.available_ports.remove(new Integer(port));
		running=true;
		System.out.println("Starting new TorThread on port "+port);

		/*this.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread th, Throwable ex) {
				System.out.println("Uncaught exception: " + ex);
			}
		});*/
	}
    
	
	/**
	 * Run the thread : launch shell command
	 */
	public void run() {

		try{
			try{
				new File(".tor_tmp/torpid"+port).delete();
				File pidfile = new File(".tor_tmp/torpid"+port);
				pidfile.createNewFile();
				//System.out.println(pidfile.getAbsolutePath());
			}catch(Exception e){e.printStackTrace();}
			
			if(new File("conf/torcommand").exists()){//try to read a replacement tor command in conf file
				// TODO command as java task arg ?
				BufferedReader r = new BufferedReader(new FileReader(new File("conf/torcommand")));
				torCommand = r.readLine();r.close();
			}

			String execcommand = torCommand+" --SOCKSPort "+port+" --DataDirectory .tor_tmp/data"+port+" --PidFile .tor_tmp/torpid"+port;
			Process p=Runtime.getRuntime().exec(execcommand);
			InputStream s = p.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(s));

			// must run in background
			// Really necessary ?
			// FIXME test that + memory leaks ?
			// the Stream displayer registers the port when bootstrap is ok
			new StreamDisplayer(r,port).start();

			//String currentLine=r.readLine();
			while(true){
				//System.out.println((new Date()).toString()+currentLine);
				//currentLine = r.readLine();

				sleep(1000);
				
				//check if the Thread has to be stopped
				if((new File(".tor_tmp/kill"+port)).exists()){
					System.out.println("Detected kill file for port "+port);
					running = false;
				}
				
				if(!running){
					cleanStop(true);
					break;
				}
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
			//while(true){e.printStackTrace();}
			//throw new RuntimeException();
		}
	}


	/**
	 * Clean stop of the thread.
	 * @param withNew should a replacing thread be launched
	 */
	public void cleanStop(boolean withNew) {

		try{
			// running should already be at false
			running=false;

			try{
				//delete stopping signal
				try{(new File(".tor_tmp/kill"+port)).delete();}catch(Exception e){}
				
				// kill tor process
				String pid = new BufferedReader(new FileReader(new File(".tor_tmp/torpid"+port))).readLine();
				System.out.println("running: "+running+" ; sending SIGTERM to tor... PID : "+pid);
				Process p=Runtime.getRuntime().exec("kill -SIGTERM "+pid);
				p.waitFor();
				
				// delete port from communication file ? not needed, done at reading.
				
			}catch(Exception e){e.printStackTrace();}

			//put port again in list of available ports
			TorPool.available_ports.put(new Integer(port),new Integer(port));
			if(TorPool.used_ports.containsKey(new Integer(port))) {TorPool.used_ports.remove(new Integer(port));}

			if(Context.getMongoMode()) {
				MongoConnection.deletePortInMongo(new Integer(port).toString());
			}else {
				removeInFileWithLock(new Integer(port).toString(),".tor_tmp/ports",".tor_tmp/lock");
			}

			try{Thread.sleep(500);}catch(Exception e){}
			
			//launch a new thread to replace this one if required
			if(withNew){
				TorPool.newThread(true);
			}

		}catch(Exception e){
			e.printStackTrace();
			//throw new RuntimeException();
		}
	}


	/**
	 * Remove a target string in a locked text file
	 * @param s
	 * @param file
	 * @param lock
	 */
	public static void removeInFileWithLock(String s,String file,String lock){
		//System.out.println("removing from file port "+s);
		try{
			boolean locked = true;
			while(locked){
				System.out.println("Waiting for lock on "+lock);
				Thread.sleep(200);
				locked = (new File(lock)).exists();
			}
			File lockfile = new File(".tor_tmp/lock");lockfile.createNewFile();
			BufferedReader r = new BufferedReader(new FileReader(new File(file)));

			LinkedList<String> newcontent = new LinkedList<>();
			String currentline = r.readLine();
			while(currentline!=null){
				if(Integer.parseInt(currentline.replace("\n",""))!=Integer.parseInt(s)){
					newcontent.add(currentline);
				}
				currentline=r.readLine();
			}

			BufferedWriter w = new BufferedWriter(new FileWriter(new File(file)));
			for(String remp:newcontent){
				//System.out.println(remp);
				w.write(remp);w.newLine();
			}
			w.close();
			// unlock the dir
			lockfile.delete();
		}catch(Exception e){e.printStackTrace();}
	}


	/**
	 * Append string line to a locked file
	 *
	 * @param s
	 * @param file
	 * @param lock
	 */
	public static void appendWithLock(String s,String file,String lock){
		try{
			boolean locked = (new File(lock)).exists();
			while(locked){
				System.out.println("Waiting for lock on "+lock);
				Thread.sleep(200);
				locked = (new File(lock)).exists();
			}
			File lockfile = new File(".tor_tmp/lock");lockfile.createNewFile();
			BufferedWriter r = new BufferedWriter(new FileWriter(new File(file),true));
			r.write(s);
			r.newLine();
			r.close();
			lockfile.delete();
		}catch(Exception e){e.printStackTrace();}
	}


	
	
	
	
}
