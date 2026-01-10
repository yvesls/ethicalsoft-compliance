package com.ethicalsoft.ethicalsoft_complience.common.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;

@UtilityClass
public class ObjectUtils {

	public static boolean isNullOrEmpty( Object obj ) {
		return obj == null || ( obj instanceof Collection<?> c && c.isEmpty() ) || ( obj instanceof String s && s.isBlank() );
	}

	public static boolean isNotNullAndNotEmpty( Object object ) {
		if ( object == null ) {
			return false;
		}

		if ( object instanceof String string ) {
			return !string.isBlank() && !"0".equals( string.trim() ) && !"0.0".equals( string.trim() ) && !"false".equalsIgnoreCase( string.trim() );
		}

		if ( object instanceof Collection<?> collection ) {
			return !collection.isEmpty();
		}

		if ( object instanceof Number number ) {
			return number.doubleValue() != 0;
		}

		return !object.toString().isBlank();
	}

	public static String getOrNull( String value ) {
		return isNullOrEmpty( value ) ? null : value;
	}

}
