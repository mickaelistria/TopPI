package edu.northwestern.at.utils;

/*	Please see the license information at the end of this file. */

import java.io.*;
import java.util.*;

/**	Comparison utilties. */

public class Compare {

	/**	Returns true if two case-insensitive strings are equal.
	 *
	 *	<p>Nulls are permitted and are equal only to themselves.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			True if s1 = s2 ignoring case.
	 */

	public static boolean equalsIgnoreCase (String s1, String s2) {
		if (s1 == null) {
			return s2 == null;
		} else {
			return s2 == null ? false : s1.equalsIgnoreCase(s2);
		}
	}

	/**	Returns true if two objects are equal.
	 *
	 *	<p>Nulls are permitted and are equal only to themselves.
	 *
	 *	@param	o1		Object 1.
	 *
	 *	@param	o2		Object 2.
	 *
	 *	@return			True if o1 = o2.
	 */

	public static boolean equals (Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o2 == null ? false : o1.equals(o2);
		}
	}

	/**	Compares two case-sensitive strings.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			< 0 if s1 < s2,
	 *					0 if s1 = s2,
	 *					> 0 if s1 > s2.
	 */

	public static int compare (String s1, String s2) {
		if (s1 == null) {
			return s2 == null ? 0 : -1;
		} else {
			return s1 == null ? +1 : s1.compareTo(s2);
		}
	}

	/**	Compares two case-insensitive strings.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			< 0 if s1 < s2,
	 *					0 if s1 = s2,
	 *					> 0 if s1 > s2.
	 */

	public static int compareIgnoreCase (String s1, String s2) {
		if (s1 == null) {
			return s2 == null ? 0 : -1;
		} else {
			return s2 == null ? +1 : s1.compareToIgnoreCase(s2);
		}
	}

	/**	Compares two dates.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	d1		Date 1.
	 *
	 *	@param	d2		Date 2.
	 *
	 *	@return			< 0 if d1 < d2,
	 *					0 if d1 = d2,
	 *					> 0 if d1 > d2.
	 */

	public static int compare (Date d1, Date d2) {
		if (d1 == null) {
			return d2 == null ? 0 : -1;
		} else {
			return d2 == null ? +1 : d1.compareTo(d2);
		}
	}

	/**	Compares two ints.
	 *
	 *	@param	n1		Int 1.
	 *
	 *	@param	n2		Int 2.
	 *
	 *	@return			-1 if n1 < n2,
	 *					0 if n1 = n2,
	 *					+1 if n1 > n2.
	 */

	public static int compare (int n1, int n2) {
		if (n1 < n2) {
			return -1;
		} else if (n1 > n2) {
			return +1;
		} else {
			return 0;
		}
	}

	/**	Compares two longs.
	 *
	 *	@param	n1		Long 1.
	 *
	 *	@param	n2		Long 2.
	 *
	 *	@return			-1 if n1 < n2,
	 *					0 if n1 = n2,
	 *					+1 if n1 > n2.
	 */

	public static int compare (long n1, long n2) {
		if (n1 < n2) {
			return -1;
		} else if (n1 > n2) {
			return +1;
		} else {
			return 0;
		}
	}

	/**	Compares two doubles.
	 *
	 *	@param	d1		double 1.
	 *
	 *	@param	d2		double 2.
	 *
	 *	@return			-1 if d1 < d2,
	 *					0 if d1 = d2,
	 *					+1 if d1 > d2.
	 */

	public static int compare( double d1 , double d2 )
	{
		if ( d1 < d2 )
		{
			return -1;
		}
		else if ( d1 > d2 )
		{
			return +1;
		}
		else
		{
			return 0;
		}
	}

	/**	Compares two Integers.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	n1		Integer 1.
	 *
	 *	@param	n2		Integer 2.
	 *
	 *	@return			-1 if n1 < n2,
	 *					0 if n1 = n2,
	 *					+1 if n1 > n2.
	 */

	public static int compare (Integer n1, Integer n2) {
		if (n1 == null) {
			return n2 == null ? 0 : -1;
		} else {
			return n2 == null ? +1 : compare(n1.intValue(), n2.intValue());
		}
	}

	/**	Compares two Longs.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	n1		Long 1.
	 *
	 *	@param	n2		Long 2.
	 *
	 *	@return			-1 if n1 < n2,
	 *					0 if n1 = n2,
	 *					+1 if n1 > n2.
	 */

	public static int compare (Long n1, Long n2) {
		if (n1 == null) {
			return n2 == null ? 0 : -1;
		} else {
			return n2 == null ? +1 : compare(n1.longValue(), n2.longValue());
		}
	}

	/**	Compares two Doubles.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.</p>
	 *
	 *	@param	d1		Double 1.
	 *
	 *	@param	d2		Double 2.
	 *
	 *	@return			-1 if d1 < d2,
	 *					0 if d1 = d2,
	 *					+1 if d1 > d2.
	 */

	public static int compare( Double d1 , Double d2 )
	{
		if ( d1 == null )
		{
			return ( d2 == null ) ? 0 : -1;
		}
		else
		{
			return ( d2 == null ) ?
				+1 : compare( d1.doubleValue() , d2.doubleValue() );
		}
	}

	/**	Compares two bytes.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	b1		byte 1.
	 *
	 *	@param	b2		byte 2.
	 *
	 *	@return			-1 if b1 < b2,
	 *					0 if b1 = b2,
	 *					+1 if b1 > b2.
	 */

	public static int compare( byte b1 , byte b2 )
	{
		if ( b1 < b2 )
		{
			return -1;
		}
		else if ( b1 > b2 )
		{
			return +1;
		}
		else
		{
			return 0;
		}
	}

	/** Hides the default no-arg constructor. */

	private Compare () {
		throw new UnsupportedOperationException();
	}

}

/*
 * <p>
 * Copyright &copy; 2004-2011 Northwestern University.
 * </p>
 * <p>
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more
 * details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 * </p>
 */

