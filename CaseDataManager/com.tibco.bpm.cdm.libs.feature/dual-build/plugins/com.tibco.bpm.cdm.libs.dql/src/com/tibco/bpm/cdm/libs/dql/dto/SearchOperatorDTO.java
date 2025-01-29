package com.tibco.bpm.cdm.libs.dql.dto;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an operator that may form part of an AttributeSearchCondition
 * @author smorgan
 */
public class SearchOperatorDTO
{
	public static final SearchOperatorDTO			EQ				= new SearchOperatorDTO("=");
	public static final SearchOperatorDTO			NEQ				= new SearchOperatorDTO("!=");
	public static final SearchOperatorDTO			LT				= new SearchOperatorDTO("<");
	public static final SearchOperatorDTO			GT				= new SearchOperatorDTO(">");
	public static final SearchOperatorDTO			LTE				= new SearchOperatorDTO("<=");
	public static final SearchOperatorDTO			GTE				= new SearchOperatorDTO(">=");
	public static final SearchOperatorDTO			LTGT			= new SearchOperatorDTO("<>");
	public static final SearchOperatorDTO			BETN			= new SearchOperatorDTO("between");
	public static final SearchOperatorDTO			NOT_BETN		= new SearchOperatorDTO("not between");
	public static final SearchOperatorDTO			IN				= new SearchOperatorDTO("in");
	public static final SearchOperatorDTO			NOT_IN			= new SearchOperatorDTO("not in");
	public static final SearchOperatorDTO			LIKE			= new SearchOperatorDTO("like");
	
	private static final List<SearchOperatorDTO>	ALL_OPERATORS	= Arrays.asList(EQ,NEQ,LT,GT,LTE,GTE,LTGT,BETN,NOT_BETN,IN,NOT_IN,LIKE);

	private String									dqlSymbol;

	private SearchOperatorDTO(String dqlSymbol)
	{
		this.dqlSymbol = dqlSymbol;
	}

	/**
	 * Obtains the instance corresponding to the given DQL symbol
	 * @param dqlSymbol
	 * @return
	 */
	public static SearchOperatorDTO fromDQLSymbol(String dqlSymbol)
	{
		return ALL_OPERATORS.stream().filter(s -> s.dqlSymbol.equals(dqlSymbol)).findFirst().orElse(null);
	}

	public String toString()
	{
		return dqlSymbol;
	}
}
