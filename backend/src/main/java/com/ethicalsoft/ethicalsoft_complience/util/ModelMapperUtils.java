package com.ethicalsoft.ethicalsoft_complience.util;

import jakarta.persistence.Id;
import lombok.experimental.UtilityClass;
import org.hibernate.collection.spi.PersistentCollection;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@UtilityClass
public class ModelMapperUtils {

	private static final ModelMapper modelMapper;
	private static final ModelMapper modelMapperDynamic;

	// Cache para instâncias de ModelMapper customizadas
	private static final Map<MapperConfig, ModelMapper> customMapperCache = new ConcurrentHashMap<>();

	static {
		modelMapper = buildMap( MatchingStrategies.STRICT, false );
		modelMapperDynamic = buildMap( MatchingStrategies.STANDARD, false );
	}

	/**
	 * Cria um model mapper novo, para qualquer requisitante.
	 */
	private static ModelMapper buildMap( MatchingStrategy matchingStrategy, boolean preferNestedProperties ) {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.createTypeMap( String.class, LocalDate.class );
		modelMapper.createTypeMap( String.class, LocalDateTime.class );
		modelMapper.addConverter( new ModelMapperLocalDateConverter() );
		modelMapper.addConverter( new ModelMapperLocalDateTimeConverter() );
		modelMapper.getConfiguration().setMatchingStrategy( matchingStrategy ).setPreferNestedProperties( preferNestedProperties );

		return modelMapper;
	}

	/**
	 * Escolhe o model mapper padrão, ou obtém/cria um mapper customizado do cache.
	 */
	private static ModelMapper chooseMap( MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		// Se nenhum parâmetro customizado for passado, usa o mapper padrão (STRICT)
		if ( matchingStrategy == null && preferNestedProperties == null ) {
			return ModelMapperUtils.modelMapper;
		}

		// Define os valores padrão se algum for nulo
		MatchingStrategy strategy = ( matchingStrategy == null ) ? ModelMapperUtils.modelMapper.getConfiguration().getMatchingStrategy() : matchingStrategy;

		boolean preferNested = ( preferNestedProperties == null ) ? ModelMapperUtils.modelMapper.getConfiguration().isPreferNestedProperties() : preferNestedProperties;

		// Cria a chave de cache
		MapperConfig config = new MapperConfig( strategy, preferNested );

		// Obtém do cache. Se não existir, cria, armazena e retorna.
		// Isso é thread-safe e muito mais performático que criar um novo mapper a cada chamada.
		return customMapperCache.computeIfAbsent( config, k -> buildMap( k.strategy(), k.preferNested() ) );
	}

	/**
	 * Faz o mapeamento entre classes.
	 */
	public static <D, T> D map( final T entity, Class<D> outClass, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		return modelMapper.map( entity, outClass );
	}

	/**
	 * Faz o mapeamento de uma coleção de classes.
	 */
	public static <D, T> List<D> mapAll( final Collection<T> entityList, Class<D> outCLass, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		// Usa List.of() (Java 9+) para retornar uma lista vazia imutável
		if ( entityList == null || entityList.isEmpty() ) {
			return List.of();
		}

		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );

		return entityList.stream().map( entity -> modelMapper.map( entity, outCLass ) ).toList();
	}

	/**
	 * Faz o mapeamento para uma instância de destino existente.
	 */
	public static <S, D> D map( final S source, D destination, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		modelMapper.map( source, destination );
		return destination;
	}

	/**
	 * Faz o mapeamento para um tipo genérico.
	 */
	public static <S, D> D map( final S source, Type genericType, MatchingStrategy matchingStrategy, Boolean preferNestedProperties ) {
		ModelMapper modelMapper = chooseMap( matchingStrategy, preferNestedProperties );
		return modelMapper.map( source, genericType );
	}

	public static <D, T> D map( final T entity, Class<D> outClass ) {
		return map( entity, outClass, null, null );
	}

	// --- Métodos de conveniência (padrão) ---

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

	// --- Métodos dinâmicos ---

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

	/**
	 * Prevents {@code LazyInitializationException} by skipping LAZY fields if not initialized.
	 */
	public static void removeMappingsIfLazyIsNotInitialized( ModelMapper modelMapper ) {
		modelMapper.getConfiguration().setPropertyCondition( context -> !( context.getSource() instanceof PersistentCollection p ) || p.wasInitialized() );
	}

	/**************************************************************************************************************************************************
	 *
	 * Converter aux methods
	 *
	 **************************************************************************************************************************************************/

	/**
	 * Converte uma Coleção de Entidades em uma Coleção de IDs (Long).
	 */
	public static <T> Converter<Collection<T>, Collection<Long>> convertEntityIdToLong() {
		return ctx -> ctx.getSource().stream().map( ModelMapperUtils::getIdValue ).collect( Collectors.toSet() ); // Mantém Set mutável
	}

	/**
	 * Converte uma Coleção de Entidades em uma Coleção de IDs (String).
	 */
	public static <T> Converter<Collection<T>, Collection<String>> convertEntityIdToString() {
		return ctx -> ctx.getSource().stream().map( ModelMapperUtils::getStringIdValue ).collect( Collectors.toSet() );
	}

	/**
	 * Converte uma Coleção de IDs (Long) em uma Coleção de Entidades (com apenas o ID).
	 */
	public static <T> Converter<Collection<Long>, Collection<T>> convertLongToEntityId( Class<T> clazz ) {
		return ctx -> ctx.getSource().stream().map( id -> ModelMapperUtils.setIdValue( clazz, id ) ).collect( Collectors.toSet() );
	}

	/**
	 * Converte uma Coleção de IDs (String) em uma Coleção de Entidades (com apenas o ID).
	 */
	public static <T> Converter<Collection<String>, Collection<T>> convertStringToEntityId( Class<T> clazz ) {
		return ctx -> ctx.getSource().stream().map( id -> ModelMapperUtils.setStringIdValue( clazz, id ) ).collect( Collectors.toSet() );
	}

	/**
	 * Encontra o campo anotado com @Id (inclusive em superclasses) e retorna seu valor.
	 * <p>
	 * <b>Refatoração:</b> Corrigida a lógica de busca na superclasse.
	 */
	public static <T> Long getIdValue( T entity ) {
		if ( entity == null ) {
			return null;
		}

		PropertyAccessor accessor = getEntityAccessor( entity );
		Class<?> currentClass = entity.getClass();

		while ( currentClass != null && currentClass != Object.class ) {
			for ( Field field : currentClass.getDeclaredFields() ) {
				if ( hasIdAnnotation( field ) ) {
					return ( Long ) accessor.getPropertyValue( field.getName() );
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return null;
	}

	/**
	 * Encontra o campo anotado com @Id (inclusive em superclasses) e retorna seu valor.
	 */
	public static <T> String getStringIdValue( T entity ) {
		if ( entity == null ) {
			return null;
		}

		PropertyAccessor accessor = getEntityAccessor( entity );
		Class<?> currentClass = entity.getClass();

		while ( currentClass != null && currentClass != Object.class ) {
			for ( Field field : currentClass.getDeclaredFields() ) {
				if ( hasIdAnnotation( field ) ) {
					return ( String ) accessor.getPropertyValue( field.getName() );
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return null;
	}

	/**
	 * Cria uma nova instância da entidade e define seu valor de @Id (Long).
	 * 1. Corrigida a lógica de busca na superclasse.
	 * 2. Lança {@code IllegalStateException} em vez de {@code e.printStackTrace()}.
	 * 3. Usa <b>String Templates (STR)</b> para a mensagem de exceção.
	 */
	public static <T> T setIdValue( Class<T> clazz, Long id ) {
		try {
			T entity = clazz.getDeclaredConstructor().newInstance();
			PropertyAccessor accessor = getEntityAccessor( entity );
			Class<?> currentClass = clazz;

			while ( currentClass != null && currentClass != Object.class ) {
				for ( Field field : currentClass.getDeclaredFields() ) {
					if ( hasIdAnnotation( field ) ) {
						accessor.setPropertyValue( field.getName(), id );
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

	/**
	 * Cria uma nova instância da entidade e define seu valor de @Id (String).
	 */
	public static <T> T setStringIdValue( Class<T> clazz, String id ) {
		try {
			T entity = clazz.getDeclaredConstructor().newInstance();
			PropertyAccessor accessor = getEntityAccessor( entity );
			Class<?> currentClass = clazz;

			while ( currentClass != null && currentClass != Object.class ) {
				for ( Field field : currentClass.getDeclaredFields() ) {
					if ( hasIdAnnotation( field ) ) {
						accessor.setPropertyValue( field.getName(), id );
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

	/**
	 * Verifica se o campo possui a anotação @Id.
	 * pelo método {@code isAnnotationPresent()}, que é muito mais limpo e eficiente.
	 */
	private static boolean hasIdAnnotation( Field field ) {
		return field.isAnnotationPresent( Id.class );
	}

	private record MapperConfig( MatchingStrategy strategy, boolean preferNested ) {}

	/****************************************************************************************************************************************************/

	// --- Classes Stub (apenas para o código compilar) ---
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