package player;

import java.util.ArrayList;

public class Resolution {
	private String quality;
	private ArrayList<Long> segments;
	
	public Resolution(String quality){
		this.quality = quality;
		segments = new ArrayList<Long>();
		segments.add(0,(long) 0);
	}
	
	public void add(int index, long segment){
		segments.add(index, segment);
	}
	
	public long get(int index){
		return segments.get(index);
	}
	
	public String quality(){
		return quality;
	}
	
	public int size(){
		return segments.size();
	}

}
