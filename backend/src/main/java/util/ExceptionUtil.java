package util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ExceptionUtil {

	public static List<String> getErrorStackTrace( Throwable exception, boolean showStackTrace ) {
		List<String> stackTrace = new ArrayList<>();
		if ( showStackTrace ) {
			addTrace( exception, stackTrace );
		}
		return stackTrace;
	}

	public static void addTrace( Throwable exception, List<String> stackTrace ) {
		if ( exception == null ) {
			return;
		}
		stackTrace.add( exception.getClass().getName() + ": " + exception.getLocalizedMessage() );
		String atStackString = "  at ";
		if ( exception.getStackTrace() != null ) {
			stackTrace.addAll( Stream.of( exception.getStackTrace() ).map( StackTraceElement::toString ).map( atStackString::concat ).toList() );
		}
		if ( exception.getCause() != null ) {
			stackTrace.add( "Caused by: " );
			addTrace( exception.getCause(), stackTrace );
		}
	}

}
