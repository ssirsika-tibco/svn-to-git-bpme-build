package com.tibco.bpm.cdm.core.rest.v1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.bpm.cdm.api.dto.CaseReference;
import com.tibco.bpm.cdm.api.exception.ArgumentException;
import com.tibco.bpm.cdm.core.rest.RESTRequestException;
import com.tibco.bpm.cdm.util.DateTimeParser;
import com.tibco.bpm.cdm.util.TimestampOp;

/**
 * A simple specialised parser for '$filter' expressions passed to Case Data Manager's REST API.
 * Parses the supplied expression and provides getters to obtain the values plucked from it.
 *
 * This is intentionally only capable of dealing with 'anded' expressions (no ors, etc) as nothing 
 * more sophisticated is required.
 *
 * @author smorgan
 * @since 2019
 */
public class FilterParser
{
	private static final Pattern	AND_START								= Pattern.compile("(?i)(\\s+and\\s+).*");

	private static final Pattern	APPLICATION_ID_EXPRESSION				= Pattern
			.compile("applicationId\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	NAMESPACE_EXPRESSION					= Pattern
			.compile("namespace\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	CID_EXPRESSION							= Pattern
			.compile("cid\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	NAME_EXPRESSION							= Pattern
			.compile("name\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	CASETYPE_EXPRESSION						= Pattern
			.compile("caseType\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	CASE_STATE_EXPRESSION					= Pattern
			.compile("caseState\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	TARGET_CASE_REFERENCE_EXPRESSION		= Pattern
			.compile("targetCaseReference\\s+(?i)(eq)\\s+'(.+)'");

	private static final Pattern	APPLICATION_MAJOR_VERSION_EXPRESSION	= Pattern
			.compile("applicationMajorVersion\\s+(?i)(eq)\\s+(.+)");

	private static final Pattern	IS_CASE_EXPRESSION						= Pattern
			.compile("isCase\\s+(?i)(eq)\\s+(.+)");

	private static final Pattern	IS_IN_TERMINAL_STATE_EXPRESSION			= Pattern
			.compile("isInTerminalState\\s+(?i)(eq)\\s+(.+)");

    private static final Pattern MODIFICATION_TIMESTAMP_EXPRESSION =
            Pattern.compile("modificationTimestamp\\s+(?i)(le|gt|eq)\\s+(\\S+)");

    private static final Pattern IS_REFERENCED_BY_PROCESS_STATE_EXPRESSION =
            Pattern.compile("isReferencedByProcess\\s+(?i)(eq)\\s+'(.+)'");

	// "Function names are always expressed in lower case." (regarding 'in')
	// http://confluence.tibco.com/display/BPM/Ariel+REST+API+Query+Language#ArielRESTAPIQueryLanguage-Functions
	private static final Pattern	CASE_REFERENCE_IN_EXPRESSION			= Pattern
			.compile("caseReference\\s+in\\((.*)\\)");

	// "Function names are always expressed in lower case." (regarding 'in')
	// http://confluence.tibco.com/display/BPM/Ariel+REST+API+Query+Language#ArielRESTAPIQueryLanguage-Functions
	private static final Pattern	TARGET_CASE_REFERENCE_IN_EXPRESSION		= Pattern
			.compile("targetCaseReference\\s+in\\((.*)\\)");

	private static final Pattern	QUOTED_REF								= Pattern.compile("'(.+)'");

	// A backslash that is not followed by a second backslash
	private static final String		LONE_BACKSLASH							= "\\\\(?!\\\\)";

	private String					applicationId;

	private String					namespace;

	private String					caseType;

	private String					name;

	private String					caseReferencesString;

	private List<CaseReference>		caseReferences;

	private String					targetCaseReferencesString;

	private List<CaseReference>		targetCaseReferences;

	private String					targetCaseReferenceString;

	private CaseReference			targetCaseReference;

	private String					stateValue;

	private String					applicationMajorVersion;

	private String					cid;

	private String					isCase;

	private String					isInTerminalState;

	private String					modificationTimestampString;

	private Calendar				modificationTimestamp;
	private TimestampOp				timestampOperator = TimestampOp.LESS_THAN_EQUALS;

    private String isReferencedByProcess;

	private List<String>			allExpressions							= new ArrayList<>();

	private List<String>			invalidExpressions						= new ArrayList<>();

	private Map<String, String>		matchedExpressions						= new HashMap<String, String>();

	public FilterParser(String filter)
	{
		if (filter != null)
		{

			// Separate the 'anded' expressions and
			// attempt to parse them, setting the appropriate
			// variables based on what's found. Getters can later be
			// called to obtain this information.
			allExpressions = splitOnAnd(filter);

			// If no expressions were returned, but the filter is non-empty, this suggests
			// the entire string is invalid, so treat this as a single invalid expression.
			if (allExpressions.isEmpty() && filter.length() > 0)
			{
				invalidExpressions.add(filter);
			}

			for (String expr : allExpressions)
			{
				// Trim excess leading/trailing whitespace
				expr = expr.trim();

				// Attempt to match the expression against something we recognise
				boolean matched = false;

				matched = matchApplicationId(expr);
				if (!matched)
				{
					matched = matchNamespace(expr);
				}
				if (!matched)
				{
					matched = matchCaseType(expr);
				}
				if (!matched)
				{
					matched = matchName(expr);
				}
				if (!matched)
				{
					matched = matchCaseState(expr);
				}
				if (!matched)
				{
					matched = matchApplicationMajorVersion(expr);
				}
				if (!matched)
				{
					matched = matchCID(expr);
				}
				if (!matched)
				{
					matched = matchIsCase(expr);
				}
				if (!matched)
				{
					matched = matchIsInTerminalState(expr);
				}
				if (!matched)
				{
					matched = matchModificationTimestamp(expr);
				}
				if (!matched)
				{
					matched = matchCaseReferences(expr);
				}
				if (!matched)
				{
					matched = matchTargetCaseReferences(expr);
				}
				if (!matched)
				{
					matched = matchTargetCaseReference(expr);
				}
                if (!matched) {
                    matched = matchIsReferencedByProcess(expr);
                }

				// If nothing matched, that's an error, so store the expression
				// for later retrieval via getInvalidExpressions()
				if (!matched)
				{
					invalidExpressions.add(expr);
				}
			}
		}
	}

	// Removes backslashes from the value, except where followed by a second backslash
	// (which is an intentional literal backslash, preceded by the escape character)
	private String unescape(String value)
	{
		String result = value.replaceAll(LONE_BACKSLASH, "");
		return result;
	}

	private List<String> splitOnAnd(String filter)
	{
		List<String> expressions = new ArrayList<String>();
		boolean inQuotedPortion = false;
		boolean escaped = false;
		int start = 0;
		for (int i = 0; i < filter.length(); i++)
		{
			char c = filter.charAt(i);

			if (inQuotedPortion)
			{
				// When we're in a quoted portion, we're looking for the end of it (i.e. the closing quote)
				// During this, we need to be aware of escaped quotes that are part of the literal value
				// and not intended to end it.
				if (escaped)
				{
					// We're escaped, so this must be a \ or ' to be valid
					if (c != '\\' && c != '\'')
					{
						// An illegal escape sequence was found, so return an empty list.
						// The calling method interprets this as meaning the filter can't be parsed.
						return Collections.emptyList();
					}
					// We've found the character that was escaped, so escaping is no longer in effect.
					escaped = false;
				}
				else if (c == '\\')
				{
					// Escape character found within quoted portion
					// (Next character will need to be a \ or ')
					escaped = true;
				}
				// At the end of the quoted portion?
				else if (c == '\'')
				{
					// We've left the quoted portion, so we're interested in
					// looking for 'ands' again
					inQuotedPortion = false;
				}
			}
			else
			{
				if (c == '\'')
				{
					// Found a quote, so ignore everything until we leave the
					// quoted portion
					inQuotedPortion = true;
				}
				else
				{
					// Not in a quoted expression, so check for an ' and '
					Matcher m = AND_START.matcher(filter.substring(i));
					if (m.matches())
					{
						// Found an 'and', so consume everything up to this
						// point and skip over the 'and'.
						String expr = filter.substring(start, i);
						// if(expr.contains("\\\'")){
						// expr = expr.replaceAll("\\"+"\'", "asd");
						//
						// }
						expressions.add(expr);
						start = i + m.end(1);
						i = start;
					}
				}
			}
		}

		// Consume whatever is left after the final and
		if (start < filter.length())
		{
			String expr = filter.substring(start);
			expressions.add(expr);
		}

		return expressions;
	}

	public boolean matchCaseReferences(String expr)
	{
		Matcher m = CASE_REFERENCE_IN_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (matches)
		{
			caseReferencesString = m.group(1);
			// Split on comma
			String[] tokens = caseReferencesString.split(",");
			// then trim
			boolean error = false;
			caseReferences = new ArrayList<>();
			for (int i = 0; i < tokens.length && !error; i++)
			{
				String token = tokens[i].trim();
				try
				{
					Matcher mRef = QUOTED_REF.matcher(token);
					if (mRef.matches())
					{
						String ref = mRef.group(1);
						CaseReference caseReference = new CaseReference(ref);
						caseReferences.add(caseReference);
					}
					else
					{
						error = true;
					}
				}
				catch (ArgumentException e)
				{
					error = true;
				}
			}
			if (error)
			{
				// A parsing error was encountered, so set the list to null.
				// The caller can use the fact that the list is null, but the
				// caseReferencesString is not null to indicate that the
				// expression was invalid.
				caseReferences = null;
			}
			matchedExpressions.put("caseReference-in", expr);
		}
		return matches;
	}

	public boolean matchTargetCaseReferences(String expr)
	{
		Matcher m = TARGET_CASE_REFERENCE_IN_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (matches)
		{
			targetCaseReferencesString = m.group(1);
			// Split on comma
			String[] tokens = targetCaseReferencesString.split(",");
			// then trim
			boolean error = false;
			targetCaseReferences = new ArrayList<>();
			for (int i = 0; i < tokens.length && !error; i++)
			{
				String token = tokens[i].trim();
				try
				{
					Matcher mRef = QUOTED_REF.matcher(token);
					if (mRef.matches())
					{
						String ref = mRef.group(1);
						CaseReference caseReference = new CaseReference(ref);
						targetCaseReferences.add(caseReference);
					}
					else
					{
						error = true;
					}
				}
				catch (ArgumentException e)
				{
					error = true;
				}
			}
			if (error)
			{
				// A parsing error was encountered, so set the list to null.
				// The caller can use the fact that the list is null, but the
				// caseReferencesString is not null to indicate that the
				// expression was invalid.
				targetCaseReferences = null;
			}
			matchedExpressions.put("targetCaseReference-in", expr);
		}
		return matches;
	}

	private boolean matchApplicationId(String expr)
	{
		Matcher m = APPLICATION_ID_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			applicationId = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("applicationId", expr);
		}
		return matches;
	}

	private boolean matchNamespace(String expr)
	{
		Matcher m = NAMESPACE_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			namespace = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("namespace", expr);
		}
		return matches;
	}

	private boolean matchCID(String expr)
	{
		Matcher m = CID_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			cid = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("cid", expr);
		}
		return matches;
	}

	private boolean matchCaseType(String expr)
	{
		Matcher m = CASETYPE_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			caseType = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("caseType", expr);
		}
		return matches;
	}

	private boolean matchName(String expr)
	{
		Matcher m = NAME_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			name = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("name", expr);
		}
		return matches;
	}

	private boolean matchCaseState(String expr)
	{
		Matcher m = CASE_STATE_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			stateValue = unescape(m.group(2));
			matches = true;
			matchedExpressions.put("caseState", expr);
		}
		return matches;
	}

	private boolean matchApplicationMajorVersion(String expr)
	{
		Matcher m = APPLICATION_MAJOR_VERSION_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			applicationMajorVersion = m.group(2);
			matches = true;
			matchedExpressions.put("applicationMajorVersion", expr);
		}
		return matches;
	}

	private boolean matchIsCase(String expr)
	{
		Matcher m = IS_CASE_EXPRESSION.matcher(expr);
		boolean matches = false;
		if (m.matches())
		{
			isCase = m.group(2);
			matches = true;
			matchedExpressions.put("isCase", expr);
		}
		return matches;
	}

	private boolean matchIsInTerminalState(String expr)
	{
		Matcher m = IS_IN_TERMINAL_STATE_EXPRESSION.matcher(expr);
		boolean matches = false;
		if (m.matches())
		{
			isInTerminalState = m.group(2);
			matches = true;
			matchedExpressions.put("isInTerminalState", expr);
		}
		return matches;
	}

	private boolean matchTargetCaseReference(String expr)
	{
		Matcher m = TARGET_CASE_REFERENCE_EXPRESSION.matcher(expr);
		boolean matches = m.matches();
		if (m.matches())
		{
			targetCaseReferenceString = unescape(m.group(2));
			try
			{
				targetCaseReference = new CaseReference(targetCaseReferenceString);
			}
			catch (ArgumentException e)
			{
				// A parsing error was encountered, so set the value to null.
				// The caller can use the fact that it is null, but the
				// targetCaseReferenceString is not null to indicate that the
				// expression was invalid.
				targetCaseReference = null;
			}
			matches = true;
			matchedExpressions.put("targetCaseReference", expr);
		}
		return matches;
	}

	private boolean matchModificationTimestamp(String expr)
	{
		Matcher m = MODIFICATION_TIMESTAMP_EXPRESSION.matcher(expr);
		boolean matches = false;
		if (m.matches())
		{
			// Handle the operator for modificationTimestamp.
			String tsoprString = m.group(1);
			Optional<TimestampOp> opr = TimestampOp.get(tsoprString);
			if (opr.isPresent()) {
				timestampOperator = opr.get();
			} else {
				timestampOperator = TimestampOp.LESS_THAN_EQUALS;
			}
			
			modificationTimestampString = m.group(2);
			// This may return null. In that case, modificationTimestampString is now set to the original
			// (invalid) string, whereas modificationTimestamp is null, indicating
			// that it couldn't be parsed. The second argument here is a flag telling it to 'maximise' any
			// missing values (e.g. if time portion is omitted, it will be assumed to be 23:59:59.999).
			modificationTimestamp = parseModificationTimestamp(modificationTimestampString, true);
			matches = true;
			matchedExpressions.put("modificationTimestamp", expr);
        }
		return matches;
	}

    private boolean matchIsReferencedByProcess(String expr)
	    {
        Matcher m = IS_REFERENCED_BY_PROCESS_STATE_EXPRESSION.matcher(expr);
	        boolean matches = false;
	        if (m.matches())
	        {
            isReferencedByProcess = m.group(2);
	            matches = true;
	            matchedExpressions.put("isReferencedByProcess", expr);
	        }
	        return matches;
	    }

	private Calendar parseModificationTimestamp(String timestamp, boolean maximise) throws IllegalArgumentException
	{
		return DateTimeParser.parseString(timestamp, maximise);
	}

	public List<String> getAllExpressions()
	{
		return allExpressions;
	}

	public List<String> getInvalidExpressions()
	{
		return invalidExpressions;
	}

	public String getApplicationId()
	{
		return applicationId;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public String getCaseType()
	{
		return caseType;
	}

    public String getIsReferencedByProcess() {
        return isReferencedByProcess;
    }

    public void setReferencedByProcess(String isReferencedByProcess) {
        this.isReferencedByProcess = isReferencedByProcess;
    }

    public String getApplicationMajorVersion()
	{
		return applicationMajorVersion;
	}

	public String getCID()
	{
		return cid;
	}

	public String getIsCase()
	{
		return isCase;
	}

	public String getInInTerminalState()
	{
		return isInTerminalState;
	}

	public String getStateValue()
	{
		return stateValue;
	}

	public String getName()
	{
		return name;
	}

	public boolean isIsCaseValid()
	{
		// Note: Boolean values are case sensitive, as per spec:
		// http://confluence.tibco.com/display/BPM/Ariel+REST+API+Query+Language
		return (isCase != null && (isCase.equals("TRUE") || isCase.equals("FALSE")));
	}

	public Boolean getIsCaseAsBoolean()
	{
		Boolean result = null;
		if (isIsCaseValid())
		{
			result = isCase.equals("TRUE") ? true : (isCase.equals("FALSE") ? false : null);
		}
		return result;
	}

	public boolean isIsInTerminalStateValid()
	{
		// Note: Boolean values are case sensitive, as per spec:
		// http://confluence.tibco.com/display/BPM/Ariel+REST+API+Query+Language
		// We only support FALSE for this option.
		return (isInTerminalState != null && isInTerminalState.equals("FALSE"));
	}

	public String getIsInTerminalState()
	{
		return isInTerminalState;
	}
	
	public Boolean getIsInTerminalStateAsBoolean()
	{
		Boolean result = null;
		if (isIsInTerminalStateValid())
		{
			result = isInTerminalState.equals("FALSE") ? false : null;
		}
		return result;
	}

	/**
	 * Returns the unparsed string form of modificationTimestamp.
	 * Call getModificationTimestamp() to get in Calendar form.
	 */
	public String getModificationTimestampString()
	{
		return modificationTimestampString;
	}

	/**
	 * Returns the modificationTimestamp in  Calendar form.
	 * If this returns null, but getModificationTimestampString returns
	 * a non-null value, this indicates that the string was not
	 * of a valid format to be parsed.
	 */
	public Calendar getModificationTimestamp()
	{
		return modificationTimestamp;
	}

	/**
	 * If this returns null, but {@link #getCaseReferencesString()} returns non-null,
	 * that suggests that it could not be parsed.
	 *
	 * @return
	 */
	public List<CaseReference> getCaseReferences()
	{
		return caseReferences;
	}

	public String getCaseReferencesString()
	{
		return caseReferencesString;
	}

	/**
	 * If this returns null, but {@link #getTargetCaseReferencesString()} returns non-null,
	 * that suggests that it could not be parsed.
	 *
	 * @return
	 */
	public List<CaseReference> getTargetCaseReferences()
	{
		return targetCaseReferences;
	}

	public String getTargetCaseReferencesString()
	{
		return targetCaseReferencesString;
	}

	public CaseReference getTargetCaseReference()
	{
		return targetCaseReference;
	}

	public String getTargetCaseReferenceString()
	{
		return targetCaseReferenceString;
	}

	/**
	 * Throws an exception if the given FilterParser found any invalid
	 * expressions during parsing
	 */
	public void validate(List<String> recognizedFilters) throws RESTRequestException
	{
		// If any unrecognised expression were found, we'll fail for that reason.
		if (!invalidExpressions.isEmpty())
		{
			throw RESTRequestException.newBadFilterExpressions(invalidExpressions);
		}

		// There were no unrecognised expressions, but there may have been expressions
		// that the parser understands, but are not appropriate for the given use-case.
		// So, make sure we didn't match anything inappropriate.

		invalidExpressions = new ArrayList<String>();
		for (String matchedKey : matchedExpressions.keySet())
		{
			boolean match = recognizedFilters.stream().anyMatch(t -> t.equals(matchedKey));
			if (!match)
			{
				invalidExpressions.add(matchedExpressions.get(matchedKey));
			}
		}

		if (!invalidExpressions.isEmpty())
		{
			throw RESTRequestException.newBadFilterExpressions(invalidExpressions);
		}
	}

	public TimestampOp getTimestampOperator() {
		return timestampOperator;
	}
}
