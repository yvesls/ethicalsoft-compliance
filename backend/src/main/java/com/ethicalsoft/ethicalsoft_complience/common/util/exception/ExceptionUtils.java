package com.ethicalsoft.ethicalsoft_complience.common.util.exception;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ExceptionUtils {

	public static List<String> getErrorStackTrace( Throwable exception, boolean showStackTrace ) {
		List<String> stackTrace = new ArrayList<>();

		if ( exception == null ) {
			stackTrace.add( "No exception provided." );
			return stackTrace;
		}
		if ( showStackTrace ) {
			addTrace( exception, stackTrace );
		}
		return stackTrace;
	}

	public static void addTrace(Throwable exception, List<String> stackTrace) {
		try {
			if (exception == null) {
				return;
			}
			stackTrace.add(exception.getClass().getName() + ": " + exception.getLocalizedMessage());
			String atStackString = "  at ";
			if (exception.getStackTrace() != null) {
				for (StackTraceElement element : exception.getStackTrace()) {
					stackTrace.add(atStackString + element);
				}
			}
			if (exception.getCause() != null) {
				stackTrace.add("Caused by: ");
				addTrace(exception.getCause(), stackTrace);
			}
		} catch (Exception e) {
			stackTrace.add("Error while processing exception: " + e.getMessage());
		}
	}



}
