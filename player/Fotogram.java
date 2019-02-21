package player;

import java.util.Arrays;

public class Fotogram {
	private byte[] buf;
	private int quality;
	private long timestamp;
	private int length;
	
	public Fotogram(byte[] buf,int length,int quality,long timestamp){
		this.buf = new byte[length];
		this.buf=Arrays.copyOf(buf, length);
		this.quality = quality;
		this.timestamp = timestamp;
		this.length = length;
	}
	
	public byte[] getFotogram(){
		return buf;
	}
	
	public int getQuality(){
		return quality;
	}
	
	public long getTimestamp(){
		return timestamp;
	}
	
	public int getLength(){
		return length;
	}
	
}
