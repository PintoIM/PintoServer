package me.vlod.pinto;

public class Tuple<X, Y> {
	public final X item1;
	public final Y item2;
	
	public Tuple(X item1, Y item2) {
		this.item1 = item1;
		this.item2 = item2;
	}
}