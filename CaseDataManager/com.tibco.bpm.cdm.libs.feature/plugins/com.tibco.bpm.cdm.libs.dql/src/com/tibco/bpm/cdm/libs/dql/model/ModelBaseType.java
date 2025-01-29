package com.tibco.bpm.cdm.libs.dql.model;

/**
 * Base (simple) types. A predefined set; cannot be instantiated.
 * Implementors of {@link ModelAttribute#getType()} should return one
 * of these instances when the attribute is of a base type.
 * @author smorgan
 * @since 2019
 */
public class ModelBaseType implements ModelAbstractType
{
	public static final ModelBaseType	TEXT				= new ModelBaseType("Text");

	public static final ModelBaseType	NUMBER				= new ModelBaseType("Number");

	public static final ModelBaseType	FIXED_POINT_NUMBER	= new ModelBaseType("FixedPointNumber");

	public static final ModelBaseType	URI					= new ModelBaseType("URI");

	public static final ModelBaseType	BOOLEAN				= new ModelBaseType("Boolean");

	public static final ModelBaseType	DATE				= new ModelBaseType("Date");

	public static final ModelBaseType	TIME				= new ModelBaseType("Time");

	public static final ModelBaseType	DATE_TIME_TZ		= new ModelBaseType("DateTimeTZ");

	private String name;
	
	private ModelBaseType(String name)
	{
		this.name =name;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}
