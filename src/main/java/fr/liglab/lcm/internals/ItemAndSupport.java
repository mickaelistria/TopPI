package fr.liglab.lcm.internals;

/**
 * A < B <=> A.support > B.support
 */
public class ItemAndSupport implements Comparable<ItemAndSupport> {
	
	public int item;
	public int support;
	
	public ItemAndSupport(int i, int s) {
		this.item = i;
		this.support = s;
	}

	/**
	 *  Returns a negative integer, zero, or a positive integer 
	 *  as this object's support is less than, equal to, or 
	 *  greater than the specified object's support. 
	 */
	public int compareTo(ItemAndSupport other) {
		return other.support - this.support;
	}

}