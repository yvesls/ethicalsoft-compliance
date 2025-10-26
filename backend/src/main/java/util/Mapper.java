package util;

public interface Mapper<T, U> {
	U map( T t );
}