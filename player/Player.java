package player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Player {
	
	public static ArrayList<Resolution> resolutions;
	public static ConcurrentLinkedDeque<Fotogram> deque = new ConcurrentLinkedDeque<>();
	public static int delay;
	private static Thread pThread;
	private static Thread cThread;
	private static Viewer viewer;
	private static String filename;
	private static int segmentsize;

	public static void main(String[] args) throws Exception {
		
		if( args.length != 3 ) {
			filename="Lifted";
			delay=12;
			segmentsize=10;
			//System.out.printf("usage: java Player URL delay segmentSize\n") ;
			//System.exit(0);
		}
		else{
			delay = Integer.parseInt(args[1]);
			filename = args[0];
			segmentsize = Integer.parseInt(args[2]);
		}
		Socket con = sendHttpRequest(filename);//
		receiveIndex(con);
		viewer = new Viewer(1280, 720, resolutions.get(0).size()-1);
		
		pThread = new Thread(new FotogramProducer(segmentsize,resolutions,0));//
	    cThread = new Thread(new FotogramConsumer(viewer,0));
	    pThread.start();
	    cThread.start();
		
	}
	
	public static void goTo(int sec)throws Exception{
		pThread.interrupt();
	    cThread.interrupt();
	    Player.deque.clear();
	    pThread = new Thread(new FotogramProducer(segmentsize,resolutions,sec));//
	    cThread = new Thread(new FotogramConsumer(Player.viewer,sec));
	    pThread.start();
	    cThread.start();
	}

	private static void receiveIndex(Socket con) throws Exception{
		DataInputStream data = new DataInputStream(con.getInputStream());

		Scanner s = new Scanner(data);
		resolutions = new ArrayList<Resolution>();
		
		for (String x = s.nextLine(); x.length() > 0; x = s.nextLine()){
		}
		
		for (String x = s.nextLine(); x.length() > 0; x = s.nextLine()){
			resolutions.add(new Resolution(x));
		}
		
		String r;
		long b;
		for(int i = 0; i< resolutions.size();i++)
			resolutions.get(i).add(0, 0);
		while(s.hasNext()){
			r = s.next();
			s.next();
			b = s.nextLong();
			for(int i = 0; i< resolutions.size();i++)
				if(r.equals(resolutions.get(i).quality()))
					resolutions.get(i).add(resolutions.get(i).size(), b);
		}
		s.close();
		con.close();
		data.close();
		
	}

	private static Socket sendHttpRequest(String movieName) throws Exception{
		Socket socket = new Socket("localhost",8080);
		DataOutputStream data = new DataOutputStream(socket.getOutputStream());
		data.writeBytes("GET /"+movieName+"-index.dat HTTP/1.0\n");
		data.writeBytes("\r\n");
		return socket;
	}
}

class FotogramProducer implements Runnable {
	private int segmentsize;
	private int quality;
	private List<Resolution> resolutions;
	private int sec;
	
	public FotogramProducer(int segmentsize,List<Resolution> resolutions,int sec){
	 this.segmentsize = segmentsize;
	 quality = 0;
	 this.resolutions = resolutions;
	 this.sec = sec;
	}
	
	@Override
	public void run(){
		
		long timestamp,timestamp2;
		try{
			Socket socket = new Socket("localhost",8080);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream data = new DataOutputStream(socket.getOutputStream());
			while(sec< resolutions.get(0).size()-1&&!Thread.currentThread().isInterrupted()){
				if (sec + segmentsize>=resolutions.get(0).size())
					segmentsize=resolutions.get(0).size()-sec-1;
				timestamp = System.currentTimeMillis();
				//System.out.println(resolutions.get(quality).quality());
				data.writeBytes("GET /"+resolutions.get(quality).quality()+" HTTP/1.1\n");
				data.writeBytes("Host: Player\n");
				data.writeBytes("Range:bytes="+resolutions.get(quality).get(sec)
						+"-"+(resolutions.get(quality).get(sec+segmentsize)-1)+"\n");
				data.writeBytes("\r\n");
			
				long length2=resolutions.get(quality).get(sec+segmentsize)-resolutions.get(quality).get(sec);
				//String[] asd;
				String linha;
				
				do{
					linha = HTTPUtilities.readLine(dis);
					//System.out.println(linha);
					//asd = HTTPUtilities.parseHttpHeader(linha);
					//if (linha.length()>0)
						//if (asd[0].equals("Content-Length"))
							//length2 = Long.parseLong(asd[1]);
				}while(linha.length()>3&&!Thread.currentThread().isInterrupted());
				//System.out.println(length2);
				byte[] buf = new byte[128 * 1024];
				do {
					int length = dis.readInt();
					long ts = dis.readLong();
					//System.out.println(length+":"+ts);
					dis.readFully(buf, 0, length);
					Player.deque.add(new Fotogram(buf,length,quality,ts));
					//long diff = (ts-Player.deque.element().getTimestamp())/1000000000;
					length2-=4+8+length;
					/*if (diff>2*segmentsize){
						Thread.sleep(1000);
						timestamp+=1000;
					}*/

				}while(length2>0&&!Thread.currentThread().isInterrupted());
				//System.out.println(length2);
				
				sec+=segmentsize;
				timestamp2 = System.currentTimeMillis();
				if (timestamp2-timestamp < segmentsize*1000 
						&& (Player.deque.getFirst().getTimestamp()/1000000000)+Player.delay<sec)
					quality++;
				else if (timestamp2-timestamp > segmentsize*1000
						&& (Player.deque.getFirst().getTimestamp()/1000000000)+Player.delay>sec)
					quality--;
				if (quality < 0) quality = 0;
				if (quality >= resolutions.size()) quality = resolutions.size()-1;
			}
			dis.close();
			socket.close();
		}			
		catch(EOFException e){}
		catch(Exception e){
			Thread.currentThread().interrupt();
		}
	}
}

class FotogramConsumer implements Runnable {
	private Viewer viewer;
	private long sec;
	
	public FotogramConsumer(Viewer viewer, int sec){
		this.viewer = viewer;
		this.sec = sec*1000;
	}
	
	@Override
	public void run() {
		try {
			boolean ready = false;
			int elapsed = 0;
			do{
				Thread.sleep(1000);
				elapsed+=1;
				if (!Player.deque.isEmpty())
					if ((Player.deque.getLast().getTimestamp()-
							Player.deque.getFirst().getTimestamp())/1000000>=Player.delay*1000)
						ready=true;
			}while(!ready&&elapsed<Player.delay&&!Thread.currentThread().isInterrupted());
			long init = System.currentTimeMillis();
			init-=this.sec;
			long now,diference;
			Fotogram f;
			while (!Player.deque.isEmpty()&&!Thread.currentThread().isInterrupted()) {
				f = Player.deque.remove();
				now = System.currentTimeMillis()-init;
				diference = (f.getTimestamp()/1000000)-now;
				
				if (diference>0)
					Thread.sleep(diference);
				viewer.updateFrame(f.getFotogram(), f.getLength(), f.getTimestamp());
				if (Player.deque.size()==0){//rebuffering || end
					ready = false;
					elapsed = 0;
					do{
						Thread.sleep(1000);
						init+=1000;
						elapsed+=1;
						if (!Player.deque.isEmpty())
							if ((Player.deque.getLast().getTimestamp()-
									Player.deque.getFirst().getTimestamp())/1000000>=Player.delay*1000)
								ready=true;
					}while(!ready&&elapsed<Player.delay&&!Thread.currentThread().isInterrupted());
				}
		}
		} catch (Exception e) {
			Thread.currentThread().interrupt();
		}
	}
}
