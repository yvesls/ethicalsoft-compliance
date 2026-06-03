package com.ethicalsoft.ethicalsoft_complience.common.util.mapper;

import jakarta.persistence.Id;
import lombok.experimental.UtilityClass;
import org.hibernate.collection.spi.PersistentCollection;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MappingContext;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class ModelMapperUtils {

	private static final ModelMapper modelMapper;
	private static final ModelMapper modelMapperDynamic;

	private static final Map<MapperConfig, ModelMapper> customMapperCache = new ConcurrentHashMap<>();

	static {
		modelMapper = buildMap( MatchingStrategies.STRICT, false );
		modelMapperDynamic = buildMap( MatchingStrategies.STANDARD, false );
	}

	private static ModelMapper buildMap( MatchingStrategy matchingStrategy, boolean preferNestedProperties ) {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.createTypeMap( String.class, LocalDate.class );
		modelMapper.createTypeMap( String.class, LocalDateTime.class );
		modelMapper.addConverter( new ModelMapperLocalDateConverter() );
		modelMapper.addConverter( new ModelMapperLocalDateTimeConverter() );
		modelMapper.getConfiguration().setMatchingStrategy( matchingStrategy ).setPreferNestedProperties( preferNestedProperties );

		return modelMapper;
	}

	private static ModelMapper chooseMap( MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {

		if ( matchingStrategy == null && preferNestedProperties == null ) {
			return ModelMapperUtils.modelMapper;
		}

		MatchingStrategy strategy = ( matchingStrategy == null ) ? ModelMapperUtils.modelMapper.getConfiguration().getMatchingStrategy() : matchingStrategy;

		boolean preferNested = ( preferNestedProperties == null ) ? ModelMapperUtils.modelMapper.getConfiguration().isPreferNestedProperties() : preferNestedProperties;

		MapperConfig config = new MapperConfig( strategy, preferNested );

		return customMapperCache.computeIfAbsent( config, k -> buildMap( k.strategy(), k.preferNested() ) );
	}

	public static <D, T> D map( final T entity, Class<D> outClass, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		return modelMapper.map( entity, outClass );
	}

	public static <D, T> List<D> mapAll( final Collection<T> entityList, Class<D> outCLass, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {

		if ( entityList == null || entityList.isEmpty() ) {
			return List.of();
		}

		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );

		return entityList.stream().map( entity -> modelMapper.map( entity, outCLass ) ).toList();
	}

	public static <S, D> D map( final S source, D destination, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		modelMapper.map( source, destination );
		return destination;
	}

	public static <S, D> D map( final S source, Type genericType, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		return modelMapper.map( source, genericType );
	}

	public static <D, T> D map( final T entity, Class<D> outClass ) {
		return map( entity, outClass, null, null );
	}


	public static <D, T> List<D> mapAll( final Collection<T> entityList, Class<D> outCLass ) {
		return mapAll( entityList, outCLass, null, null );
	}

	public static <S, D> D map( final S source, D destination ) {
		return map( source, destination, null, null );
	}

	public static <S, D> D map( final S source, Type genericType ) {
		return map( source, genericType, null, null );
	}

	public static <D, T> D mapDynamic( final T entity, Class<D> outClass ) {
		return modelMapperDynamic.map( entity, outClass );
	}

	public static <D, T> List<D> mapAllDynamic( final Collection<T> entityList, Class<D> outCLass ) {
		if ( entityList == null || entityList.isEmpty() ) {
			return List.of();
		}
		return entityList.stream().map( entity -> mapDynamic( entity, outCLass ) ).toList();
	}

	public static <S, D> D mapDynamic( final S source, D destination ) {
		modelMapperDynamic.map( source, destination );
		return destination;
	}

	public static <S, D> D mapDynamic( final S source, Type genericType ) {
		return modelMapperDynamic.map( source, genericType );
	}

	public static void removeMappingsIfLazyIsNotInitialized( ModelMapper modelMapper ) {
		modelMapper.getConfiguration().setPropertyCondition( context -> !( context.getSource() instanceof PersistentCollection p ) || p.wasInitialized() );
	}

	public static <T> Converter<Collection<T>, Collection<Long>> convertEntityIdToLong() {
		return ctx -> toSet(ctx, ModelMapperUtils::getIdValue);
	}

	public static <T> Converter<Collection<T>, Collection<String>> convertEntityIdToString() {
		return ctx -> toSet(ctx, ModelMapperUtils::getStringIdValue);
	}

	public static <T> Converter<Collection<Long>, Collection<T>> convertLongToEntityId( Class<T> clazz ) {
		return ctx -> toSet( ctx, id -> ModelMapperUtils.setIdValue( clazz, id ) );
	}

	public static <T> Converter<Collection<String>, Collection<T>> convertStringToEntityId( Class<T> clazz ) {
		return ctx -> toSet( ctx, id -> ModelMapperUtils.setStringIdValue( clazz, id ) );
	}

	private static <S, R> Collection<R> toSet(MappingContext<Collection<S>, Collection<R>> ctx, Function<S, R> mapper) {
        if (ctx.getSource() == null) {
            return List.of();
        }
        return ctx.getSource().stream().map(mapper).collect(Collectors.toSet());
    }

	public static <T> Long getIdValue( T entity ) {
		return readIdField( entity, Long.class );
	}

	public static <T> String getStringIdValue( T entity ) {
		return readIdField( entity, String.class );
	}

	public static <T> T setIdValue( Class<T> clazz, Long id ) {
		return writeIdField( clazz, id );
	}

	public static <T> T setStringIdValue( Class<T> clazz, String id ) {
		return writeIdField( clazz, id );
	}

	private static <T, R> R readIdField( T entity, Class<R> idType ) {
		if ( entity == null ) {
			return null;
		}
		PropertyAccessor accessor = getEntityAccessor( entity );
		Class<?> currentClass = entity.getClass();
		while ( currentClass != null && currentClass != Object.class ) {
			for ( Field field : currentClass.getDeclaredFields() ) {
				if ( hasIdAnnotation( field ) ) {
					return idType.cast( accessor.getPropertyValue( field.getName() ) );
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return null;
	}

	private static <T, ID> T writeIdField( Class<T> clazz, ID id ) {
		try {
			T entity = clazz.getDeclaredConstructor().newInstance();
			Class<?> currentClass = clazz;

			while ( currentClass != null && currentClass != Object.class ) {
				for ( Field field : currentClass.getDeclaredFields() ) {
					if ( hasIdAnnotation( field ) ) {
						ReflectionUtils.makeAccessible( field );
						ReflectionUtils.setField( field, entity, id );
						return entity;
					}
				}
				currentClass = currentClass.getSuperclass();
			}
			throw new IllegalArgumentException( "Nenhum campo @Id encontrado na classe " + clazz.getName() + " ou superclasses." );

		} catch ( ReflectiveOperationException | IllegalArgumentException | SecurityException e ) {
			throw new IllegalStateException( "Não foi possível instanciar ou definir o ID para a entidade " + clazz.getSimpleName(), e );
		}
	}

	private static <T> PropertyAccessor getEntityAccessor( T entity ) {
		return PropertyAccessorFactory.forBeanPropertyAccess( entity );
	}

	private static boolean hasIdAnnotation( Field field ) {
		return field.isAnnotationPresent( Id.class );
	}

	private record MapperConfig( MatchingStrategy strategy, boolean preferNested ) {}

	private static class ModelMapperLocalDateConverter implements Converter<String, LocalDate> {
		@Override
		public LocalDate convert( org.modelmapper.spi.MappingContext<String, LocalDate> context ) {
			return context.getSource() == null ? null : LocalDate.parse( context.getSource() );
		}
	}

	private static class ModelMapperLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		@Override
		public LocalDateTime convert( org.modelmapper.spi.MappingContext<String, LocalDateTime> context ) {
			return context.getSource() == null ? null : LocalDateTime.parse( context.getSource() );
		}
	}
}
